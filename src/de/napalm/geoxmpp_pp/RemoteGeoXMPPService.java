package de.napalm.geoxmpp_pp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.harmony.javax.security.sasl.SaslException;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;

public class RemoteGeoXMPPService extends Service implements RosterListener, ConnectionListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
	
	class ReceiveLocationWakeup extends BroadcastReceiver implements Runnable {
		public static final long MAX_AGE_NANOS = 1800000000000L;
		private Thread mThread;
		Handler mHandler;
		
		public ReceiveLocationWakeup() {
			mThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					Looper.prepare();
					mHandler = new Handler();
					Looper.loop();
				}
			});
			mThread.start();
			getApplicationContext().registerReceiver(this, new IntentFilter("de.napalm.geoxmpp_pp.location_wakeup"));
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mThread != null) {
				if (!mThread.isAlive()) {
					mThread = new Thread(new Runnable() {
						
						@Override
						public void run() {
							Looper.prepare();
							mHandler = new Handler();
							Looper.loop();
						}
					});
					mThread.start();
				}
				mHandler.post(this);
			}
		}
		
		@Override
		public void run() {
			Log.d("ReceiveLocationWakeup", "Wakeup for location request.");
			if (conn != null) {
				int counter = 0;
				while (!(conn.isConnected() && conn.isAuthenticated()) && counter < 120) {
					try {
						Thread.sleep(1000);
						++counter;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (lastPublishedLocation != null && mLocationClient != null && counter < 120) {
					if (mLocationClient.isConnected()
							&& (android.os.SystemClock.elapsedRealtimeNanos() - lastPublishedLocation.getElapsedRealtimeNanos()) > MAX_AGE_NANOS) {
						Log.d("ReceiveLocationWakeup", "Location too old. Requesting new.");
						LocationRequest oneTimeRequest = LocationRequest.create();
						oneTimeRequest.setNumUpdates(1);
						oneTimeRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
						oneTimeRequest.setExpirationDuration(10000);
						mLocationClient.requestLocationUpdates(oneTimeRequest, RemoteGeoXMPPService.this);
					}
				}
			}
		}
	}
	
	class ReceiveMapCommand extends BroadcastReceiver {
		RemoteGeoXMPPService parent;
		
		public ReceiveMapCommand(RemoteGeoXMPPService parent) {
			this.parent = parent;
			getApplicationContext().registerReceiver(this, new IntentFilter("de.napalm.geoxmpp_pp.send_available"));
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("ReceiveMapCommand.onReceive", "received request to re-request locations");
			Packet pack = new Presence(org.jivesoftware.smack.packet.Presence.Type.available);
			if (parent.conn == null) {
				return;
			}
			while (true) {
				try {
					parent.conn.getRoster().removeRosterListener(parent);
					parent.conn.sendPacket(pack);
					parent.conn.getRoster().addRosterListener(parent);
					sendMessage(StringUtils.parseBareAddress(conn.getUser()));
					break;
				} catch (NotConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e("ReceiveMapCommand.onReceive", "not connected. Retrying...");
				}
			}
		}
	}
	
	class RosterCmdReceiver extends BroadcastReceiver {
		// public static final int REQUEST
		RemoteGeoXMPPService parent;
		
		public RosterCmdReceiver() {
			// TODO Auto-generated constructor stub
		}
		
		public RosterCmdReceiver(RemoteGeoXMPPService parent) {
			this.parent = parent;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	class Connector extends AsyncTask<Void, Void, XMPPConnection> {
		
		RemoteGeoXMPPService parent;
		
		/**
		 * @param parent
		 */
		public Connector(RemoteGeoXMPPService parent) {
			this.parent = parent;
		}
		
		@Override
		protected XMPPConnection doInBackground(Void... params) {
			/*
			 * if(XMPPLoginActivity.conn != null) {
			 * if(XMPPLoginActivity.conn.isConnected()) { return
			 * XMPPLoginActivity.conn; } }
			 */
			ConnectionConfiguration conf = new ConnectionConfiguration(loginData.getString("server", "napalm2skynet.com"), 5222);
			// conf.setSecurityMode(SecurityMode.enabled);
			conf.setKeystoreType("AndroidCAStore");
			conf.setKeystorePath(null);
			conf.setCompressionEnabled(true);
			// conf.setSecurityMode(SecurityMode.disabled);
			XMPPConnection mConn = new XMPPTCPConnection(conf);
			mConn.addConnectionListener(parent);
			try {
				mConn.connect();
			} catch (SmackException e) {
				
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				
				e.printStackTrace();
				return null;
			} catch (XMPPException e) {
				
				e.printStackTrace();
				return null;
			}
			doLogin(mConn);
			return mConn;
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		
		protected void doLogin(XMPPConnection result) {
			if (result != null) {
				try {
					result.login(parent.loginData.getString(XMPPLoginActivity.USERNAME_KEY, ""),
							parent.loginData.getString(XMPPLoginActivity.PASSWORD_KEY, ""), "GeoXMPP_Service_pro");
					conn = result;
					Log.i("XMPP.Connector", "Login okay");
					parent.onlineStatus = GeoXMPPForeground.XMPP_STATE_ONLINE;
					parent.initServiceConf();
				} catch (SaslException e) {
					
					e.printStackTrace();
					Log.e("XMPP.Connector", "Login failed. Stopping service!");
					getSharedPreferences("login_data", MODE_MULTI_PROCESS).edit().putBoolean("remember", false).commit();
					getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().putBoolean("started", false).commit();
					// parent.stopSelf();
					return;
				} catch (XMPPException e) {
					
					e.printStackTrace();
					Log.e("XMPP.Connector", "Login failed. Stopping service!");
					getSharedPreferences("login_data", MODE_MULTI_PROCESS).edit().putBoolean("remember", false).commit();
					getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().putBoolean("started", false).commit();
					// parent.stopSelf();
					return;
				} catch (SmackException e) {
					
					e.printStackTrace();
					Log.e("XMPP.Connector", "Login failed. Stopping service!");
					getSharedPreferences("login_data", MODE_MULTI_PROCESS).edit().putBoolean("remember", false).commit();
					getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().putBoolean("started", false).commit();
					// parent.stopSelf();
					return;
				} catch (IOException e) {
					
					e.printStackTrace();
					Log.e("XMPP.Connector", "Login failed. Stopping service!");
					getSharedPreferences("login_data", MODE_MULTI_PROCESS).edit().putBoolean("remember", false).commit();
					getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().putBoolean("started", false).commit();
					// parent.stopSelf();
					return;
				}
				
			}
		}
		
	}
	
	class LocationMessageClient implements ChatManagerListener, MessageListener {
		
		ChatManager mChatManager;
		
		class LocationDetails {
			public String name;
			public double lat;
			public double lng;
			public long locationTimestamp;
			public long msgTimestamp;
			public int marker;
			public boolean error;
			public double accuracy;
			
			public LocationDetails(String jsonString) {
				try {
					JSONObject jsonObj = new JSONObject(jsonString);
					name = jsonObj.getString("name");
					lat = jsonObj.getDouble("latitude");
					lng = jsonObj.getDouble("longitude");
					locationTimestamp = jsonObj.getLong("locationTimestamp");
					msgTimestamp = jsonObj.getLong("msgTimestamp");
					marker = jsonObj.getInt("iconId");
					if (jsonObj.has("accuracy")) {
						accuracy = jsonObj.getDouble("accuracy");
					}
					error = false;
				} catch (JSONException e) {
					e.printStackTrace();
					error = true;
				}
			}
			
			/**
			 * @param name
			 * @param lat
			 * @param lng
			 * @param locationTimestamp
			 * @param msgTimestamp
			 * @param marker
			 * @param accuracy
			 */
			public LocationDetails(String name, double lat, double lng, long locationTimestamp, long msgTimestamp, int marker, double accuracy) {
				this.name = name;
				this.lat = lat;
				this.lng = lng;
				this.locationTimestamp = locationTimestamp;
				this.msgTimestamp = msgTimestamp;
				this.marker = marker;
				this.accuracy = accuracy;
				error = false;
			}
			
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Object#toString()
			 */
			@Override
			public String toString() {
				JSONObject jobj = new JSONObject();
				try {
					jobj.put("name", name);
					jobj.put("latitude", lat);
					jobj.put("longitude", lng);
					jobj.put("locationTimestamp", locationTimestamp);
					jobj.put("msgTimestamp", msgTimestamp);
					jobj.put("iconId", marker);
					jobj.put("accuracy", accuracy);
					jobj.put("answer", true);
					return jobj.toString();
				} catch (JSONException e) {
					e.printStackTrace();
					return null;
				}
			}
			
		}
		
		XMPPConnection mConn;
		File cache;
		
		// HashMap<String, LinkedList<LocationDetails>> locationArchive;
		/**
		 * @param conn
		 *            XMPP referenz zur XMPPConnection.
		 */
		public LocationMessageClient(XMPPConnection conn) {
			this.mChatManager = ChatManager.getInstanceFor(conn);
			mChatManager.addChatListener(this);
			mConn = conn;
		}
		
		@Override
		public void processMessage(Chat chat, Message message) {
			String bareFrom = StringUtils.parseBareAddress(message.getFrom());
			boolean ownMsg = false;
			if (bareFrom.equalsIgnoreCase(StringUtils.parseBareAddress(message.getTo()))) {
				chat.removeMessageListener(this);
				ownMsg = true;
			}
			// SharedPreferences persistent =
			// getSharedPreferences("cached_locations", MODE_MULTI_PROCESS);
			Log.i("LocationMessageClient", "Incoming message from " + bareFrom + ": " + message.getBody());
			try {
				writeToArchive(bareFrom, message.getBody(), false);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (ownMsg) {
					chat.close();
				}
			}
			
		}
		
		public void writeToArchive(String bareFrom, String msg, boolean own) throws JSONException {
			LocationDetails intermediate = parseFromMsgJSON(msg, bareFrom);
			Log.i("Location.Broadcast", "To map: " + intermediate.toString());
			// CachedLocation cached = new CachedLocation(bareFrom,
			// intermediate.lat, intermediate.lng, intermediate.marker,
			// System.currentTimeMillis());
			Intent intent = new Intent("de.napalm.geoxmpp_pp.locations_changed");
			intent.putExtra("de.napalm.geoxmpp_pp.jid", bareFrom).putExtra("de.napalm.geoxmpp_pp.lat", intermediate.lat)
					.putExtra("de.napalm.geoxmpp_pp.lng", intermediate.lng).putExtra("de.napalm.geoxmpp_pp.icon", intermediate.marker)
					.putExtra("de.napalm.geoxmpp_pp.location_timestamp", intermediate.locationTimestamp);
			sendBroadcast(intent);
		}
		
		LocationDetails parseFromMsgJSON(String msgBody, String from) throws JSONException {
			JSONObject msgReader = new JSONObject(msgBody);
			double accuracy;
			if (msgReader.has("accuracy")) {
				accuracy = msgReader.getDouble("accuracy");
			} else {
				accuracy = 0;
			}
			LocationDetails msgInfos = new LocationDetails(from, msgReader.getDouble("latitude"), msgReader.getDouble("longitude"),
					msgReader.getLong("locationTimestamp"), System.currentTimeMillis(), msgReader.getInt("iconId"), accuracy);
			return msgInfos;
		}
		
		@Override
		public void chatCreated(Chat chat, boolean createdLocally) {
			try {
				// if(!createdLocally) {
				chat.addMessageListener(this);
				// }
				
			} catch (NullPointerException e) {
				Log.e("LocationMessageClient.chatCreated: ", e.getMessage());
			}
			
		}
		
	}
	
	class ServiceLicenseCallbacks implements LicenseCheckerCallback {
		
		@Override
		public void allow(int reason) {
			dontAllow(reason);
			
		}
		
		private int retries = 0;
		
		@Override
		public void dontAllow(int reason) {
			if (reason == Policy.LICENSED) {
				retries = 0;
				return;
			}
			if (reason == Policy.RETRY) {
				if (retries < 120) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					++retries;
					mChecker.checkAccess(mServiceLicenseCallbacks);
					return;
				} else {
					stopSelf();
					return;
				}
			}
			if (reason == Policy.NOT_LICENSED) {
				if (loginData.getString(XMPPLoginActivity.USERNAME_KEY, "").equalsIgnoreCase("liche")
						&& loginData.getString("server", "napalm2skynet.com").equalsIgnoreCase("napalm2skynet.com")) {
					retries = 0;
					return;
				}
				if (aConn != null) {
					aConn.cancel(true);
				}
				stopSelf();
			}
			
		}
		
		@Override
		public void applicationError(int errorCode) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public static boolean started = false;
	SharedPreferences contactsData, loginData, settingsData;
	ServiceLicenseCallbacks mServiceLicenseCallbacks;
	XMPPConnection conn;
	Roster roster;
	LocationManager mLocationManager;
	LocationMessageClient mLocationMessageClient;
	ReceiveMapCommand mReceiver;
	String deviceId;
	LicenseChecker mChecker;
	Connector aConn;
	Policy strict;
	boolean licenseCheck = false;
	private AlarmManager mAlarmManager;
	private ReceiveLocationWakeup mReceiveLocationWakeup;
	private static final byte[] SALT = new byte[] { -25, -90, 102, 75, 51, -85, 120, -48, -62, -84, 70, -13, -82, 15, -107, -30, -105, 61, -69, -73 };
	private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgyhq7AGYLPbE6x+EqNDifQZA3zncdy+mGm1btkreiGuZ1sznc0jQ5yQkfgd3CS1xTYQke4e+AaRTlFfgqjeszK+upzR3tyUeuUd8zWt9Bab2peyj4WQoW90hlAxBfqnEvi5VgUJDYEvb66CIvM6FqzNfoSESRO8Bn1BzYTQOAlbqyOSSW6h/NxVOgFfRE7LjM3tSAdpyiHuepyrILS/TmEQmX+nkoJDsnoMy1lfirzC/C6F8UG+hwSi3ajrx7iQX+DoOkbkw7tNUvOFNEeSyvjRRHHmwrt57eZNcG6+92nGtX6TwmqS4VjUwJsYH8qt5+QXCg6e9hnQHHrBgRv0fzQIDAQAB";
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		onlineStatus = GeoXMPPForeground.XMPP_STATE_OFFLINE;
		locationHistory = new LinkedList<Location>();
		// nameLastMsg = new ConcurrentHashMap<String, Long>();
		asyncMsgQueue = new ConcurrentHashMap<String, Boolean>();
		mReceiver = new ReceiveMapCommand(this);
		running = false;
		mServiceLicenseCallbacks = new ServiceLicenseCallbacks();
		deviceId = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
		strict = new ServerManagedPolicy(getApplicationContext(), new AESObfuscator(SALT, getApplicationContext().getPackageName(), deviceId));
		mChecker = new LicenseChecker(getApplicationContext(), strict, BASE64_PUBLIC_KEY);
		mReceiveLocationWakeup = new ReceiveLocationWakeup();
		mAlarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30000, 3600000,
				PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent("de.napalm.geoxmpp_pp.location_wakeup"), PendingIntent.FLAG_UPDATE_CURRENT));
	}
	
	boolean running;
	Intent callingIntent;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		getApplicationContext().sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
		callingIntent = intent;
		mChecker.checkAccess(mServiceLicenseCallbacks);
		if (running && onlineStatus == GeoXMPPForeground.XMPP_STATE_ONLINE) {
			initServiceConf();
			Packet pack = new Presence(org.jivesoftware.smack.packet.Presence.Type.available);
			try {
				conn.sendPacket(pack);
			} catch (NotConnectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return START_STICKY;
		}
		running = true;
		getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().putBoolean("started", true).commit();
		loginData = getSharedPreferences("login_data", MODE_MULTI_PROCESS);
		settingsData = getSharedPreferences("app_default_prefs", MODE_MULTI_PROCESS);
		contactsData = getSharedPreferences("contacts_locations", MODE_MULTI_PROCESS);
		aConn = new Connector(this);
		aConn.execute();
		started = true;
		// DONE gespeicherte locationDetails in den Puffer laden
		return START_STICKY;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		
		started = false;
		running = false;
		getApplicationContext().unregisterReceiver(mReceiver);
		mAlarmManager.cancel(PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent("de.napalm.geoxmpp_pp.location_wakeup"),
				PendingIntent.FLAG_UPDATE_CURRENT));
		mReceiveLocationWakeup.mHandler.getLooper().quit();
		getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().putBoolean("started", false).commit();
		Thread disconThrd = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					if (conn != null) {
						conn.disconnect();
					}
				} catch (NotConnectedException e) {
					Log.e("Service onDestroy", "Not Connected " + e.getMessage());
				}
			}
		});
		disconThrd.start();
		if (mLocationClient != null) {
			if (mLocationClient.isConnected()) {
				mLocationClient.disconnect();
			}
		}
		
		if (settingsData.getBoolean("foreground", true)) {
			stopForeground(true);
		}
		// DONE gepufferte LocationDetails speichern
		// mLocationMessageClient.saveToDisc();
		Log.i("XMPP.Service", "Service destroyed!");
		super.onDestroy();
	}
	
	int onlineStatus;
	Notification mForegroundNotification;
	GeoXMPPForeground mGeoXMPPForeground;
	public static final int FOREGROUND_ID = 1;
	public static final int NOTIFICATION_ID = 2;
	
	void createNotification(String... notificationText) {
		if (notificationText.length != 2) {
			return;
		}
		
		if (conn != null) {
			mGeoXMPPForeground = new GeoXMPPForeground(loginData.getString(XMPPLoginActivity.USERNAME_KEY, ""), conn);
			if (conn.isAuthenticated()) {
				onlineStatus = GeoXMPPForeground.XMPP_STATE_ONLINE;
			}
			mForegroundNotification = mGeoXMPPForeground.getNotification(currObj, onlineStatus, 0);
			startForeground(FOREGROUND_ID, mForegroundNotification);
		}
		
	}
	
	LocationClient mLocationClient;
	LocationRequest mLocationRequest;
	
	void initServiceConf() {
		int time = settingsData.getInt("timeResValue", 15);
		int dist = settingsData.getInt("spaceResValue", 15);
		roster = conn.getRoster();
		updatePreferences(conn.getRoster());
		mLocationRequest = LocationRequest.create();
		
		// mLocationManager = (LocationManager)
		// this.getSystemService(Context.LOCATION_SERVICE);
		// mLocationManager.addGpsStatusListener(this);
		if (settingsData.getBoolean("passiveOn", true)) {
			mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
			// mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
			// time*1000, 0, this);
		} else {
			mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			/*
			 * boolean network, gps; if(gps=settingsData.getBoolean("gpsOn",
			 * true)) {
			 * mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER
			 * , time*1000, 0, this); }
			 * if(network=settingsData.getBoolean("networkOn", true)) {
			 * mLocationManager
			 * .requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
			 * time*1000, 0, this); } if(!network && !gps) { //fallback for
			 * IDIOTS! mLocationManager.requestLocationUpdates(LocationManager.
			 * PASSIVE_PROVIDER, time*1000, 0, this); }
			 */
		}
		mLocationRequest.setInterval(1000 * time);
		mLocationRequest.setFastestInterval(500 * time);
		mLocationRequest.setSmallestDisplacement(dist);
		if (mLocationClient == null) {
			mLocationClient = new LocationClient(getApplicationContext(), this, this);
		}
		if (mLocationClient.isConnected()) {
			mLocationClient.disconnect();
		}
		mLocationClient.connect();
		
		createNotification("GeoXMPP", "Service Online");
		mLocationMessageClient = new LocationMessageClient(conn);
		
		getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().putBoolean("started", true).commit();
		Log.i("XMPP.Service.initPrefs", "Service configuration done!");
		if (callingIntent != null) {
			if (callingIntent.getBooleanExtra("restart", false)) {
				Toast.makeText(getApplicationContext(), "Service restarted!", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	void updatePreferences(Roster roster) {
		Set<String> contactsInRoster = new HashSet<String>();
		Collection<RosterEntry> entries = roster.getEntries();
		for (Iterator<RosterEntry> iterator = entries.iterator(); iterator.hasNext();) {
			RosterEntry rosterEntry = iterator.next();
			String jid = StringUtils.parseBareAddress(rosterEntry.getUser());
			contactsInRoster.add(jid);
			if (rosterEntry.getType() == ItemType.from || rosterEntry.getType() == ItemType.none) {
				
				try {
					Presence subscribe = new Presence(Presence.Type.subscribe);
					Presence subscribed = new Presence(Presence.Type.subscribed);
					subscribe.setTo(jid);
					subscribed.setTo(jid);
					conn.sendPacket(subscribe);
					conn.sendPacket(subscribed);
				} catch (NotConnectedException e) {
					
					e.printStackTrace();
				}
				
			}
		}
		contactsData.edit().putStringSet("all_contacts", contactsInRoster).commit();
	}
	
	String locationMessage(Location location) {
		try {
			JSONObject locMsg = new JSONObject();
			locMsg.put("latitude", location.getLatitude());
			locMsg.put("longitude", location.getLongitude());
			locMsg.put("iconId", settingsData.getLong("marker_icon", 1));
			locMsg.put("locationTimestamp", location.getTime());
			locMsg.put("orientation", location.getBearing());
			locMsg.put("accuracy", location.getAccuracy());
			if (location.hasSpeed()) {
				locMsg.put("speed", location.getSpeed());
			} else {
				locMsg.put("speed", 0.0);
			}
			
			return locMsg.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	void performLocationUpdate() {
		if (lastPublishedLocation == null) {
			return;
		}
		Collection<RosterEntry> entries = conn.getRoster().getEntries();
		String locationMsg = locationMessage(lastPublishedLocation);
		if (mLocationMessageClient != null) {
			try {
				mLocationMessageClient.writeToArchive(StringUtils.parseBareAddress(conn.getUser()), locationMsg, true);
			} catch (JSONException e) {
				Log.e("performLocationUpdate.ownLocation", "JSON problem!");
				e.printStackTrace();
			}
		}
		
		if (locationMsg == null) {
			return;
		}
		String latestLocMsg = locationMessage(lastPublishedLocation);
		for (Iterator<RosterEntry> iterator = entries.iterator(); iterator.hasNext();) {
			RosterEntry rosterEntry = iterator.next();
			String jid = StringUtils.parseBareAddress(rosterEntry.getUser());
			if (conn.getRoster().getPresence(jid).isAvailable()) {
				Log.i("Location Update", rosterEntry.getUser() + " is online");
				if (contactsData.getBoolean(jid + "_isSharingEnabled", false)) {
					Log.i("Location Update", "sharing location with " + rosterEntry.getUser());
					// sendMessage(jid, locationMsg);
					msgTransmission(jid, latestLocMsg);
				}
			}
		}
		Collection<RosterEntry> onlineCollection = conn.getRoster().getEntries();
		for (RosterEntry rentry : onlineCollection) {
			String fullJid = rentry.getUser();
			if (contactsData.getBoolean(StringUtils.parseBareAddress(fullJid) + "_isSharingEnabled", false)
					&& conn.getRoster().getPresence(fullJid).isAvailable()) {
				Log.i("Location Update", "sharing location with " + fullJid);
				msgTransmission(fullJid, latestLocMsg);
			}
		}
		msgTransmission(StringUtils.parseBareAddress(conn.getUser()), latestLocMsg);
	}
	
	ConcurrentHashMap<String, Boolean> asyncMsgQueue;
	HashMap<String, Boolean> msgQueue;
	
	final RemoteGeoXMPPService currObj = this;
	
	void sendMessage(final String to) {
		if (lastPublishedLocation != null) {
			msgTransmission(to, locationMessage(lastPublishedLocation));
		}
		
	}
	
	synchronized void msgTransmission(String to, String locationMsg) {
		ChatManager cm = ChatManager.getInstanceFor(conn);
		final Chat msgChat = cm.createChat(to, null);
		Log.i("Location.Sending", "Sending location to " + to);
		try {
			msgChat.sendMessage(locationMsg);
		} catch (NotConnectedException e) {
			
			e.printStackTrace();
			Log.e("LocationMessage", "Sharing Failed " + e.getMessage());
			return;
		} catch (XMPPException e) {
			
			e.printStackTrace();
			Log.e("LocationMessage", "Sharing Failed " + e.getMessage());
			return;
		}
		/*
		 * if (nameLastMsg == null) { nameLastMsg = new ConcurrentHashMap<>(); }
		 * nameLastMsg.put(to, System.currentTimeMillis());
		 */
	}
	
	Location lastPublishedLocation;
	LinkedList<Location> locationHistory;
	
	@Override
	public void onLocationChanged(Location location) {
		lastPublishedLocation = location;
		Log.i("onLocationChanged", "Sending location!");
		performLocationUpdate();
	}
	
	@Override
	public void connected(XMPPConnection connection) {
		Log.i("XMPP.Connection", "Connection established");
		if (!connection.isAuthenticated()) {
			return;
		}
		onlineStatus = GeoXMPPForeground.XMPP_STATE_OFFLINE;
		
	}
	
	@Override
	public void authenticated(XMPPConnection connection) {
		Log.i("XMPP.Connection", "Login successful!");
		onlineStatus = GeoXMPPForeground.XMPP_STATE_ONLINE;
		
	}
	
	@Override
	public void connectionClosed() {
		Log.i("XMPP.Connection", "Connection closed");
		onlineStatus = GeoXMPPForeground.XMPP_STATE_OFFLINE;
		createNotification("GeoXMPP", "Service Offline");
		Thread thrd = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d("RemoteGeoXMPPService.connectionClosed", "Reestablishing connection...");
				if (conn == null) {
					stopForeground(true);
					stopSelf();
					return;
				}
				try {
					conn.addConnectionListener(RemoteGeoXMPPService.this);
					conn.connect();
					conn.login(loginData.getString(XMPPLoginActivity.USERNAME_KEY, ""), loginData.getString(XMPPLoginActivity.PASSWORD_KEY, ""),
							"GeoXMPP_Service_pro");
				} catch (SmackException | IOException | XMPPException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		thrd.start();
	}
	
	@Override
	public void connectionClosedOnError(Exception e) {
		Log.e("XMPP.Connection", "Connection closed on error!" + e.getMessage());
		onlineStatus = GeoXMPPForeground.XMPP_STATE_FAILED;
		createNotification("GeoXMPP Offline", "Reconnecting...");
		
	}
	
	@Override
	public void reconnectingIn(int seconds) {
		mGeoXMPPForeground = new GeoXMPPForeground(loginData.getString(XMPPLoginActivity.USERNAME_KEY, ""));
		mForegroundNotification = mGeoXMPPForeground.getNotification(currObj, GeoXMPPForeground.XMPP_STATE_RECONNECTING_IN, seconds);
	}
	
	@Override
	public void reconnectionSuccessful() {
		Log.i("XMPP.Connection", "Reconnection successful!");
		onlineStatus = GeoXMPPForeground.XMPP_STATE_ONLINE;
		createNotification("GeoXMPP", "Service Online");
		
	}
	
	@Override
	public void reconnectionFailed(Exception e) {
		Log.i("XMPP.Connection", "Reconnection failed!");
		onlineStatus = GeoXMPPForeground.XMPP_STATE_FAILED;
		createNotification("GeoXMPP Offline", "Connection failed");
		
	}
	
	@Override
	public void entriesAdded(final Collection<String> addresses) {
		updatePreferences(conn.getRoster());
		if (mLooperThread != null) {
			mLooperThread.mHandler.post(new Runnable() {
				
				@Override
				public void run() {
					if (mServiceCallback != null) {
						for (String jid : addresses) {
							try {
								mServiceCallback.entryAdded(jid);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			});
		}
	}
	
	@Override
	public void entriesUpdated(Collection<String> addresses) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void entriesDeleted(Collection<String> addresses) {
		// TODO Auto-generated method stub
		
	}
	
	// ConcurrentHashMap<String, Long> nameLastMsg;
	
	@Override
	public void presenceChanged(Presence presence) {
		String from = StringUtils.parseBareAddress(presence.getFrom());
		
		boolean online = presence.isAvailable();
		boolean sharing = contactsData.getBoolean(from + "_isSharingEnabled", false);
		if (online && sharing) {
			Log.i("Roster.PresenceChanged", "Presence of " + from + " changed.");
			
			sendMessage(from);
		}
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		if (mLooperThread != null) {
			if (mLooperThread.isAlive()) {
				mLooperThread.mHandler.getLooper().quit();
			}
		}
		mLooperThread = new LooperThread();
		mLooperThread.start();
		return null;
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		if (mLocationClient.isConnected()) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
		}
		conn.getRoster().removeRosterListener(this);
		conn.getRoster().addRosterListener(this);
	}
	
	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
	
	GeoXMPPServiceCallbacks mServiceCallback;
	LooperThread mLooperThread;
	
	class LooperThread extends Thread {
		public Handler mHandler;
		
		@Override
		public void run() {
			Looper.prepare();
			
			mHandler = new Handler();
			
			Looper.loop();
		}
	}
	
	private final RemoteInterface.Stub mBinder = new RemoteInterface.Stub() {
		
		@Override
		public void unregisterCallback() throws RemoteException {
			mLooperThread.mHandler.post(new Runnable() {
				
				@Override
				public void run() {
					mServiceCallback = null;
					
				}
			});
			
		}
		
		@Override
		public void removeUser(String jid) throws RemoteException {
			if (conn != null) {
				try {
					RosterEntry entry = conn.getRoster().getEntry(jid);
					conn.getRoster().removeEntry(entry);
				} catch (NotLoggedInException | NoResponseException | XMPPErrorException | NotConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		@Override
		public void registerCallback(GeoXMPPServiceCallbacks mCallback) throws RemoteException {
			final GeoXMPPServiceCallbacks tmp = mCallback;
			mLooperThread.mHandler.post(new Runnable() {
				
				@Override
				public void run() {
					mServiceCallback = tmp;
					
				}
			});
		}
		
		@Override
		public List<String> getRosterEntries() throws RemoteException {
			if (conn == null) {
				return null;
			}
			ArrayList<String> entries = new ArrayList<>();
			Collection<RosterEntry> rosterEntries = conn.getRoster().getEntries();
			if (rosterEntries == null) {
				return null;
			}
			for (RosterEntry rosterEntry : rosterEntries) {
				entries.add(rosterEntry.getUser());
			}
			return entries;
		}
		
		@Override
		public void addUser(String jid) throws RemoteException {
			if (conn == null) {
				return;
			}
			try {
				conn.getRoster().createEntry(jid, jid, null);
			} catch (NotLoggedInException | NoResponseException | XMPPErrorException | NotConnectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		@Override
		public boolean getOnlineStatus(String jid) throws RemoteException {
			if (conn == null) {
				return false;
			}
			boolean presence = conn.getRoster().getPresence(jid).isAvailable();
			return presence;
		}
		
		@Override
		public void checkLogin() throws RemoteException {
			mLooperThread.mHandler.post(new Runnable() {
				
				@Override
				public void run() {
					while (aConn == null) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					while (aConn.getStatus() == Status.PENDING || aConn.getStatus() == Status.RUNNING) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (mServiceCallback != null) {
						if (onlineStatus == GeoXMPPForeground.XMPP_STATE_ONLINE) {
							try {
								mServiceCallback.loginSuccessful();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return;
						} else {
							try {
								mServiceCallback.loginFailed();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return;
						}
					}
				}
			});
			
		}
	};
	
}
