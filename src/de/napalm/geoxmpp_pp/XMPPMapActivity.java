package de.napalm.geoxmpp_pp;

import java.util.Date;
import java.util.TreeMap;

import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

class LocationChangeReceiver extends BroadcastReceiver {
	
	LCRCallback mLCRCallback;
	
	public LocationChangeReceiver(LCRCallback callback) {
		mLCRCallback = callback;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// CachedLocation cached = (CachedLocation)
		// intent.getSerializableExtra("de.napalm.geoxmpp_pp.location_msg");
		String jid = intent.getStringExtra("de.napalm.geoxmpp_pp.jid");
		LatLng pos = new LatLng(intent.getDoubleExtra("de.napalm.geoxmpp_pp.lat", 0), intent.getDoubleExtra("de.napalm.geoxmpp_pp.lng", 0));
		int markerIcon = intent.getIntExtra("de.napalm.geoxmpp_pp.icon", 1);
		long locationTimestamp = intent.getLongExtra("de.napalm.geoxmpp_pp.location_timestamp", 0);
		mLCRCallback.receivedChange(jid, pos, markerIcon, locationTimestamp);
	}
	
}

interface LCRCallback {
	void receivedChange(String jid, LatLng pos, int markerIcon, long locationTimestamp);
}

public class XMPPMapActivity extends Activity implements LCRCallback, SensorEventListener, OnMyLocationButtonClickListener, OnCameraChangeListener,
		GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
	
	class FriendListAdapter extends BaseAdapter {
		XMPPMapActivity parentObj;
		public DialogFragment friendsDialog;
		
		public FriendListAdapter(XMPPMapActivity parent) {
			this.parentObj = parent;
		}
		
		@Override
		public int getCount() {
			return parentObj.markerStorage.size();
		}
		
		@Override
		public Object getItem(int position) {
			String itemKey = getName(position);
			return parentObj.markerStorage.get(itemKey);
		}
		
		public String getName(int position) {
			return (String) parentObj.markerStorage.navigableKeySet().toArray()[position];
		}
		
		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rootView;
			LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rootView = mInflater.inflate(R.layout.map_friend_list_item, null);
			TextView jid = (TextView) rootView.findViewById(R.id.friendJidTextView);
			String nameJid = StringUtils.parseName(getName(position));
			final Marker data = (Marker) getItem(position);
			jid.setText(nameJid);
			rootView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					LatLng pos = data.getPosition();
					parentObj.mSensorManager.unregisterListener(parentObj);
					cp = new CameraPosition.Builder().target(pos).zoom(16).bearing(0).tilt(0).build();
					perspective_state = NOT_CENTERED;
					parentObj.mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
					data.showInfoWindow();
					if (friendsDialog != null) {
						friendsDialog.dismiss();
					}
				}
			});
			return rootView;
		}
		
	}
	
	private GoogleMap mMap;
	// private SharedPreferences mLocations;
	private TreeMap<String, Marker> markerStorage;
	private LocationChangeReceiver mReceiver;
	FriendListAdapter fla;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private Handler sensorHandler[];
	private ScaleGestureDetector mScaleDetector;
	private Thread sensorEventThread, sensorEventThread2, cameraThread;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_xmppmap);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		sensorHandler = new Handler[3];
		sensorEventThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Looper.prepare();
				sensorHandler[0] = new Handler();
				Looper.loop();
			}
		});
		sensorEventThread.start();
		sensorEventThread2 = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Looper.prepare();
				sensorHandler[1] = new Handler();
				Looper.loop();
			}
		});
		sensorEventThread2.start();
		sensorHandler[2] = new Handler();
		markerStorage = new TreeMap<String, Marker>();
		
		fla = new FriendListAdapter(this);
		try {
			initMap();
			
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			finish();
		}
		mReceiver = new LocationChangeReceiver(this);
		IntentFilter filter = new IntentFilter("de.napalm.geoxmpp_pp.locations_changed");
		getApplicationContext().registerReceiver(mReceiver, filter);
		registered = true;
		// mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.xmppmap, menu);
		return true;
	}
	
	class FriendsDialog extends DialogFragment {
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.app.DialogFragment#onCreateDialog(android.os.Bundle)
		 */
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			ListView lv = new ListView(getApplicationContext());
			
			lv.setDividerHeight(0);
			lv.setAdapter(fla);
			
			// builder.setAdapter(fla, null);
			builder.setView(lv);
			builder.setTitle("Friends");
			
			builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			fla.friendsDialog = this;
			return builder.create();
			
		}
		
	}
	
	float savedZoomBeforePerspective = 14;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_friend_list) {
			DialogFragment fd = new FriendsDialog();
			fd.show(getFragmentManager(), "friend_list");
			// return true;
		} else if (id == R.id.action_perspective) {
			// Location last = mMap.getMyLocation();
			if (last == null) {
				if (mLocationClient != null) {
					if (mLocationClient.isConnected()) {
						last = mLocationClient.getLastLocation();
					}
				}
				
			}
			LatLng pos = new LatLng(last.getLatitude(), last.getLongitude());
			if (perspective_state != PERSPECTIVE) {
				savedZoomBeforePerspective = mMap.getCameraPosition().zoom;
				mSensorManager.registerListener(this, mSensor, 50 * 1000);// SensorManager.SENSOR_DELAY_FASTEST);
				cp = new CameraPosition.Builder().target(pos).zoom(savedZoomBeforePerspective).bearing(0).tilt(50).build();
				perspective_state = PERSPECTIVE;
				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
				
			} else {
				mSensorManager.unregisterListener(this);
				CameraPosition homePos = new CameraPosition.Builder().target(pos).zoom(savedZoomBeforePerspective).bearing(0).tilt(0).build();
				perspective_state = CENTERED;
				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(homePos));
				mMap.setOnCameraChangeListener(this);
				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(homePos));
			}
			
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	boolean registered;
	
	@Override
	public boolean onNavigateUp() {
		setResult(RESULT_CANCELED);
		boolean result = super.onNavigateUp();
		finish();
		return result;
	}
	
	private void initMap() throws Throwable {
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
			
			if (mMap == null) {
				throw new Throwable("Unable to create map.");
			}
			
			mMap.setMyLocationEnabled(true);
			mMap.getUiSettings().setMyLocationButtonEnabled(true);
			mMap.getUiSettings().setZoomControlsEnabled(false);
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			mLocationManager = lm;
			Location last = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			LatLng pos = new LatLng(last.getLatitude(), last.getLongitude());
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14));
			mMap.setOnMyLocationButtonClickListener(this);
			// mMap.setOnCameraChangeListener(this);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (registered == true) {
			getApplicationContext().unregisterReceiver(mReceiver);
			registered = false;
		}
		if (mLocationClient != null) {
			if (mLocationClient.isConnected()) {
				mLocationClient.removeLocationUpdates(this);
				mLocationClient.disconnect();
			}
		}
		
	}
	
	LocationClient mLocationClient;
	LocationRequest mLocationRequest;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(100);
		mLocationRequest.setFastestInterval(100);
		if (mLocationClient == null) {
			mLocationClient = new LocationClient(getApplicationContext(), this, this);
		}
		if (mLocationClient.isConnected()) {
			mLocationClient.disconnect();
		}
		mLocationClient.connect();
		try {
			initMap();
			IntentFilter filter;
			if (registered != true) {
				filter = new IntentFilter("de.napalm.geoxmpp_pp.locations_changed");
				getApplicationContext().registerReceiver(mReceiver, filter);
				registered = true;
			}
			
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Intent locationRequest = new Intent("de.napalm.geoxmpp_pp.send_available");
		sendBroadcast(locationRequest);
	}
	
	@Override
	public void receivedChange(String jid, LatLng pos, int markerIcon, long locationTimestamp) {
		Date now = new Date(locationTimestamp);
		BitmapDescriptor icon;
		switch (markerIcon) {
			case 1:
				icon = BitmapDescriptorFactory.defaultMarker(265);
				// markerIcon =
				// BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_default);
				
				break;
			case 2:
				// icon = BitmapDescriptorFactory.defaultMarker(36);
				// markerIcon =
				icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_one);
				break;
			case 3:
				icon = BitmapDescriptorFactory.defaultMarker(1);
				// markerIcon =
				// BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_two);
			default:
				icon = BitmapDescriptorFactory.defaultMarker(1);
				break;
		}
		if (jid == null) {
			return;
		}
		if (markerStorage.containsKey(jid)) {
			markerStorage.get(jid).setPosition(pos);
			markerStorage.get(jid).setSnippet("Received " + now.toString());
			markerStorage.get(jid).setIcon(icon);
		} else {
			MarkerOptions mOpts = new MarkerOptions().position(pos).title(StringUtils.parseName(jid)).snippet("Received: " + now.toString()).icon(icon);
			markerStorage.put(jid, mMap.addMarker(mOpts));
		}
		
	}
	
	LocationManager mLocationManager;
	int roundRobin = 0;
	
	@Override
	public void onSensorChanged(final SensorEvent event) {
		final float curZoom = savedZoomBeforePerspective;
		
		sensorHandler[roundRobin].post(new Runnable() {
			@Override
			public void run() {
				float[] rotMat = new float[9];
				SensorManager.getRotationMatrixFromVector(rotMat, event.values);
				float[] orientation = new float[3];
				SensorManager.getOrientation(rotMat, orientation);
				// Location last = mMap.getMyLocation();
				LatLng pos = new LatLng(last.getLatitude(), last.getLongitude());
				float camZoom = curZoom;
				float aroundX = (-1) * (float) Math.toDegrees(orientation[1]);
				float aroundZ = (float) Math.toDegrees(orientation[0]);
				float interpolated;
				
				if (aroundX < 0) {
					// aroundZ = (aroundZ + 180) % 360;
					aroundX = 0;
				}
				if (aroundX < 30) {
					interpolated = 10;
				} else if (aroundX >= 30 && aroundX < 45) {
					interpolated = (float) (0.2667 * aroundX + 2);
				} else if (aroundX >= 45 && aroundX < 70) {
					interpolated = (float) (0.06667 * aroundX + 11);
				} else {
					interpolated = (float) 15.5;
				}
				if (interpolated > camZoom) {
					camZoom = interpolated;
				}
				// Log.v("SensorChanged", "Rotation around X = " +
				// Float.toString(aroundX) + "°");
				
				final CameraPosition cam = new CameraPosition.Builder().target(pos).bearing(aroundZ).tilt(aroundX).zoom(camZoom).build();
				sensorHandler[2].post(new Runnable() {
					
					@Override
					public void run() {
						mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 50, null);
						
					}
				});
				
			}
		});
		roundRobin = (roundRobin + 1) % 2;
		
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	public static final int NOT_CENTERED = 0;
	public static final int CENTERED = 1;
	public static final int PERSPECTIVE = 2;
	private int perspective_state = 0;
	CameraPosition cp;
	Location last;
	
	@Override
	public boolean onMyLocationButtonClick() {
		// Location last = mMap.getMyLocation();
		if (last == null) {
			return false;
		}
		LatLng pos = new LatLng(last.getLatitude(), last.getLongitude());
		if (perspective_state == NOT_CENTERED) {
			cp = new CameraPosition.Builder().target(pos).zoom(16).bearing(0).tilt(0).build();
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
			perspective_state = CENTERED;
			mMap.setOnCameraChangeListener(this);
			return true;
		}
		if (perspective_state == CENTERED) {
			perspective_state = PERSPECTIVE;
			savedZoomBeforePerspective = mMap.getCameraPosition().zoom;
			mSensorManager.registerListener(this, mSensor, 50 * 1000);// SensorManager.SENSOR_DELAY_FASTEST);
			cp = new CameraPosition.Builder().target(pos).zoom(savedZoomBeforePerspective).bearing(0).tilt(50).build();
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
			
			return true;
		}
		if (perspective_state == PERSPECTIVE) {
			mSensorManager.unregisterListener(this);
			cp = new CameraPosition.Builder().target(pos).zoom(16).bearing(0).tilt(0).build();
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
			perspective_state = CENTERED;
			mMap.setOnCameraChangeListener(this);
			
			return true;
		}
		return false;
	}
	
	@Override
	public void onCameraChange(CameraPosition arg0) {
		if (mMap != null) {
			if (perspective_state == PERSPECTIVE) {
				mMap.setOnCameraChangeListener(null);
				return;
			} else {
				if (cp != null) {
					if (arg0.equals(cp)) {
						perspective_state = CENTERED;
					} else {
						perspective_state = NOT_CENTERED;
					}
				}
			}
		}
	}
	
	@Override
	public void onLocationChanged(Location arg0) {
		last = arg0;
		
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		
		if (mLocationClient.isConnected()) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
			last = mLocationClient.getLastLocation();
		}
		
	}
	
	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
	
}
