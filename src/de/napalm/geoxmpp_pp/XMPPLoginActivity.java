package de.napalm.geoxmpp_pp;

import java.io.IOException;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.harmony.javax.security.sasl.SaslException;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;

interface LoginTestResultListener {
	void onLoginSuccessful(String username, String password, XMPPConnection conn);
	
	void onLoginFailed();
	
	String getSelectedServer();
}

class LoginTester extends AsyncTask<String, Void, Boolean> {
	
	ConnectionConfiguration conf;
	public XMPPConnection conn;
	Context context;
	LoginTestResultListener callbacks;
	
	public LoginTester addTestResultListener(LoginTestResultListener callbacks, Context context) {
		this.callbacks = callbacks;
		this.context = context;
		return this;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	@Override
	protected void onPreExecute() {
		
		// conf = new ConnectionConfiguration("napalm2skynet.com", 5222);
		// conf.setSecurityMode(SecurityMode.disabled);
		// conf.setKeystoreType("AndroidCAStore");
		// conf.setKeystorePath(null);
		// conn = new XMPPTCPConnection(conf);
	}
	
	@Override
	protected Boolean doInBackground(String... params) {
		String user = params[0];
		String password = params[1];
		String server = params[2];
		try {
			conf = new ConnectionConfiguration(server, 5222);
			// conf.setSecurityMode(SecurityMode.disabled);
			conf.setKeystoreType("AndroidCAStore");
			conf.setKeystorePath(null);
			conf.setSendPresence(true);
			conf.setCompressionEnabled(true);
			// conf.setSecurityMode(SecurityMode.disabled);
			conn = new XMPPTCPConnection(conf);
			conn.connect();
			conn.login(user, password, "GeoXMPP_AppUI");
			// Intent intent = new Intent(context.getApplicationContext(),
			// RemoteGeoXMPPService.class);
			
			// intent.putExtra("username", user);
			// intent.putExtra("password", password);
			// context.getApplicationContext().startService(intent);
		} catch (SmackException e) {
			Log.e("Connection.Test", "SmackException " + e.getMessage());
			callbacks.onLoginFailed();
			return false;
		} catch (IOException e) {
			Log.e("Connection.Test", "IOException " + e.getMessage());
			callbacks.onLoginFailed();
			return false;
		} catch (XMPPException e) {
			Log.e("Connection.Test", "XMPPException " + e.getMessage());
			callbacks.onLoginFailed();
			return false;
		}
		if (conn.isConnected()) {
			if (conn.isAuthenticated()) {
				/*
				 * try { conn.disconnect(); } catch (NotConnectedException e) {
				 * Log.e("Connection.Test", "NotConnectedException " +
				 * e.getMessage()); callbacks.onLoginFailed(); return false; }
				 */
				// AsyncXMPP.conn = conn;
				callbacks.onLoginSuccessful(user, password, conn);
			} else {
				callbacks.onLoginFailed();
				return false;
			}
		} else {
			callbacks.onLoginFailed();
			return false;
		}
		Log.i("Connection.Test", "GUT!");
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(Boolean result) {
		// Intent intent = new Intent(context, GeoXMPPService.class);
		// context.bindService(intent, this, Context.BIND_AUTO_CREATE);
	}
	
}

interface LoginControl {
	SharedPreferences getPrefs();
	
	void startLogin(String username, String password);
	
	boolean isLicenseCheckValid();
	
	void setLicenseCheckValid(boolean licenseCheckValid);
	
	void logout(boolean deletePrefs);
}

public class XMPPLoginActivity extends Activity implements LoginControl, LoginTestResultListener, Roster2Activity {
	RemoteInterface mService;
	GeoXMPPServiceCallbacks mGeoCallbacks;
	class GeoCallbackListener extends GeoXMPPServiceCallbacks.Stub {

		@Override
		public void loginSuccessful() throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void loginFailed() throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void entryAdded(String jid) throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void entryRemoved(String jid) throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void presenceChanged(String jid, boolean isOnline)
				throws RemoteException {
			// TODO Auto-generated method stub
			
		}
		
	}
	class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = RemoteInterface.Stub.asInterface(service);
			
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			
		}
		
	}
	private class GeoXMPPLicenseCallback implements LicenseCheckerCallback {
		
		LoginControl parent;
		
		public GeoXMPPLicenseCallback() {
			parent = XMPPLoginActivity.this;
		}
		
		public GeoXMPPLicenseCallback(LoginControl parent) {
			this.parent = parent;
		}
		
		@Override
		public void allow(int reason) {
			if (isFinishing()) {
				return;
			}
			Log.i("XMPPLoginActivity.GeoXMPPLicenseCallback", "License valid! " + reason);
			parent.setLicenseCheckValid(true);
		}
		
		@Override
		public void dontAllow(int reason) {
			if (isFinishing()) {
				return;
			}
			if (reason == Policy.LICENSED) {
				allow(reason);
				return;
			}
			Log.e("XMPPLoginActivity.GeoXMPPLicenseCallback", "License invalid! " + reason);
			if (reason == Policy.RETRY) {
				Log.e("XMPPLoginActivity.GeoXMPPLicenseCallback", "License invalid. Retrying in 5 seconds...");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mChecker.checkAccess(this);
				return;
			}
			
			if (loginPrefs.getString(XMPPLoginActivity.USERNAME_KEY, "").equalsIgnoreCase("liche")
					&& loginPrefs.getString("server", "napalm2skynet.com").equalsIgnoreCase("napalm2skynet.com")) {
				Log.i("XMPPLoginActivity.GeoXMPPLicenseCallback", "Liche logged in! License valid! " + reason);
				allow(Policy.LICENSED);
				return;
			}
			Log.e("XMPPLoginActivity.GeoXMPPLicenseCallback", "License invalid! " + reason);
			parent.setLicenseCheckValid(false);
			// parent.setLicenseCheckValid(true);
			parent.logout(false);
			
		}
		
		@Override
		public void applicationError(int errorCode) {
			Log.e("XMPPLoginActivity.GeoXMPPLicenseCallback", "License error #" + errorCode);
			parent.setLicenseCheckValid(false);
			parent.logout(false);
			if (checkLogin != null) {
				checkLogin.cancel(true);
			}
		}
		
	}
	
	public static final String USERNAME_KEY = "login_username";
	public static final String PASSWORD_KEY = "login_password";
	public static final String VALID_FLAG_KEY = "login_valid";
	public static boolean initDone = false;
	LoginFragment loginFragment;
	SharedPreferences loginPrefs;
	boolean freshLogin;
	GeoXMPPLicenseCallback mLicenseCallback;
	LicenseChecker mChecker;
	private Handler mHandler;
	private static final byte[] SALT = new byte[] { -25, -90, 102, 75, 51, -85, 120, -48, -62, -84, 70, -13, -82, 15, -107, -30, -105, 61, -69, -73 };
	private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgyhq7AGYLPbE6x+EqNDifQZA3zncdy+mGm1btkreiGuZ1sznc0jQ5yQkfgd3CS1xTYQke4e+AaRTlFfgqjeszK+upzR3tyUeuUd8zWt9Bab2peyj4WQoW90hlAxBfqnEvi5VgUJDYEvb66CIvM6FqzNfoSESRO8Bn1BzYTQOAlbqyOSSW6h/NxVOgFfRE7LjM3tSAdpyiHuepyrILS/TmEQmX+nkoJDsnoMy1lfirzC/C6F8UG+hwSi3ajrx7iQX+DoOkbkw7tNUvOFNEeSyvjRRHHmwrt57eZNcG6+92nGtX6TwmqS4VjUwJsYH8qt5+QXCg6e9hnQHHrBgRv0fzQIDAQAB";
	private boolean licenseCheckValid;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);
		setContentView(R.layout.activity_login);
		visible = false;
		loginPrefs = getSharedPreferences("login_data", MODE_MULTI_PROCESS);
		String deviceId = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
		mLicenseCallback = new GeoXMPPLicenseCallback();
		mChecker = new LicenseChecker(this.getApplicationContext(), new ServerManagedPolicy(getApplicationContext(), new AESObfuscator(SALT, getPackageName(),
				deviceId)), BASE64_PUBLIC_KEY);
		mHandler = new Handler();
		licenseCheckValid = false;
		mChecker.checkAccess(mLicenseCallback);
		if (initDone == false) {
			SmackAndroid.init(getApplicationContext());
			Roster.setDefaultSubscriptionMode(SubscriptionMode.accept_all);
			initDone = true;
		}
		
		if (!loginPrefs.getBoolean("remember", false)) {
			loginFragment = new LoginFragment(this);
			freshLogin = true;
			getFragmentManager().beginTransaction().add(R.id.container, loginFragment).commit();
		} else {
			freshLogin = false;
			startLogin(loginPrefs.getString(USERNAME_KEY, ""), loginPrefs.getString(PASSWORD_KEY, ""));
		}
		
	}
	
	public XMPPLoginActivity() {
		
	}
	
	@Override
	protected void onDestroy() {
		critical.lock();
		super.onDestroy();
		critical.unlock();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// startLogin(loginPrefs.getString(USERNAME_KEY, ""),
		// loginPrefs.getString(PASSWORD_KEY, ""));
		
		if (requestCode == SETTINGS_REQUEST_CODE) {
			toggleServiceForeground(false);
			if (shouldServiceRun()) {
				toggleServiceForeground(true);
			}
			
		}
		/*
		 * Thread disconThrd = new Thread(new Runnable() {
		 * 
		 * @Override public void run() { try { if (conn != null) {
		 * conn.disconnect(); } } catch (NotConnectedException e) { // TODO
		 * Auto-generated catch block Log.e("XMPP UI onActivtyResult",
		 * "Not Connected " + e.getMessage()); } } }); disconThrd.start(); try {
		 * disconThrd.join(1000); startLogin(loginPrefs.getString(USERNAME_KEY,
		 * ""), loginPrefs.getString(PASSWORD_KEY, "")); } catch
		 * (InterruptedException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (actionMenu != null) {
			// actionMenu.findItem(R.id.action_logout).setVisible(visible);
			// actionMenu.findItem(R.id.action_settings).setVisible(visible);
			// actionMenu.findItem(R.id.action_add_user).setVisible(visible);
		}
		if (conn != null) {
			if (conn.isConnected()) {
				if (!conn.isAuthenticated()) {
					try {
						conn.login(loginPrefs.getString(USERNAME_KEY, ""), loginPrefs.getString(PASSWORD_KEY, ""), "GeoXMPP_AppUI");
					} catch (SaslException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					} catch (XMPPException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						if (e instanceof XMPPErrorException) {
							XMPPErrorException err = (XMPPErrorException) e;
							if (err.getXMPPError().getType() == XMPPError.Type.AUTH || err.getXMPPError().getType() == XMPPError.Type.CANCEL
									|| err.getXMPPError().getType() == XMPPError.Type.MODIFY) {
								logout();
							} else if (err.getXMPPError().getType() == XMPPError.Type.WAIT) {
								Thread thrd = new Thread(new Runnable() {
									
									@Override
									public void run() {
										try {
											Thread.sleep(10000);
											conn.login(loginPrefs.getString(USERNAME_KEY, ""), loginPrefs.getString(PASSWORD_KEY, ""), "GeoXMPP_AppUI");
										} catch (XMPPException | SmackException | IOException | InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
											logout();
										}
										
									}
								});
								thrd.start();
							}
						}
					} catch (SmackException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				startLogin(loginPrefs.getString(USERNAME_KEY, ""), loginPrefs.getString(PASSWORD_KEY, ""));
			}
		}
		
	}
	
	class AddUserDialog extends DialogFragment {
		EditText serverEdit;
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.app.DialogFragment#onCreateDialog(android.os.Bundle)
		 */
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = getActivity().getLayoutInflater();
			final View rootView = inflater.inflate(R.layout.dialog_add_user, null);
			serverEdit = (EditText) rootView.findViewById(R.id.editServerName);
			serverEdit.setText(loginPrefs.getString("server", "napalm2skynet.com"));
			builder.setView(rootView).setPositiveButton("Add", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					EditText jidEdit = (EditText) rootView.findViewById(R.id.userNameEditText);
					
					String jid = jidEdit.getText().toString() + "@" + serverEdit.getText().toString();
					try {
						conn.getRoster().createEntry(jid, jid, null);
					} catch (NotLoggedInException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoResponseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (XMPPErrorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NotConnectedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AddUserDialog.this.getDialog().cancel();
				}
			});
			return builder.create();
		}
		
	}
	
	Menu actionMenu;
	boolean visible;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		actionMenu = menu;
		menu.findItem(R.id.action_logout).setVisible(visible);
		menu.findItem(R.id.action_settings).setVisible(visible);
		menu.findItem(R.id.add_user).setVisible(visible);
		// menu.findItem(R.id.action_refresh).setVisible(visible);
		menu.findItem(R.id.check_service_enabled).setVisible(visible);
		menu.findItem(R.id.check_service_enabled).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				item.setChecked(!item.isChecked());
				toggleServiceForeground(item.isChecked());
				setServiceShouldRun(item.isChecked());
				return true;
			}
		});
		menu.findItem(R.id.check_service_enabled).setChecked(shouldServiceRun());
		return true;
	}
	
	public static final int SETTINGS_REQUEST_CODE = 111;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		super.onOptionsItemSelected(item);
		if (id == R.id.action_settings) {
			// toggleServiceForeground(false);
			/*
			 * if(mServiceIntent != null) { stopService(mServiceIntent); }
			 */
			Intent intent = new Intent(this, SettingsActivity.class);
			
			startActivityForResult(intent, SETTINGS_REQUEST_CODE);
			return true;
		} else if (id == R.id.add_user) {
			
			if (conn != null) {
				if (conn.isAuthenticated()) {
					AddUserDialog addUser = new AddUserDialog();
					addUser.show(getFragmentManager(), "AddUser");
				}
			}
			return true;
		} else if (id == R.id.action_logout) {
			logout();
			return true;
		} else if (id == R.id.action_map) {
			if (conn != null) {
				if (conn.isAuthenticated()) {
					Intent mapIntent = new Intent(this, XMPPMapActivity.class);
					startActivityForResult(mapIntent, 666);
				}
			}
			return true;
		}
		return false;
	}
	
	void logout() {
		// toggleServiceForeground(false);
		visible = false;
		if (actionMenu != null) {
			actionMenu.findItem(R.id.action_logout).setVisible(visible);
			actionMenu.findItem(R.id.action_settings).setVisible(visible);
			actionMenu.findItem(R.id.add_user).setVisible(visible);
			// actionMenu.findItem(R.id.action_refresh).setVisible(visible);
			actionMenu.findItem(R.id.check_service_enabled).setVisible(visible);
		}
		Thread thrd = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					conn.disconnect();
				} catch (NotConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				toggleServiceForeground(false);
				getSharedPreferences("app_default_prefs", MODE_MULTI_PROCESS).edit().clear().commit();
				getSharedPreferences("login_data", MODE_MULTI_PROCESS).edit().remove(USERNAME_KEY).remove(PASSWORD_KEY).remove("remember").commit();
				getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().clear().commit();
				// getSharedPreferences("contacts_locations",
				// MODE_MULTI_PROCESS).edit().clear().commit();
			}
		});
		thrd.start();
		
		loginPrefs.edit().putBoolean("remember", false);
		loginFragment = new LoginFragment(this);
		getFragmentManager().beginTransaction().replace(R.id.container, loginFragment).commit();
	}
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public class LoginFragment extends Fragment {
		
		class AddServerDialog extends DialogFragment {
			
			/*
			 * (non-Javadoc)
			 * 
			 * @see android.app.DialogFragment#onCreateDialog(android.os.Bundle)
			 */
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				LayoutInflater inflater = getActivity().getLayoutInflater();
				final View rootView = inflater.inflate(R.layout.dialog_add_server, null);
				final EditText input = (EditText) rootView.findViewById(R.id.userNameEditText);
				input.setHint("example.com");
				builder.setView(rootView).setPositiveButton("Add", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						String domain = input.getText().toString();
						TreeSet<String> servers = new TreeSet<>();
						servers.addAll(loginPrefs.getStringSet("user_servers", new TreeSet<String>()));
						servers.add(domain);
						loginPrefs.edit().putStringSet("user_servers", servers).commit();
						loginPrefs.edit().putString("server", domain).commit();
						adapter.notifyDataSetChanged();
						
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						AddServerDialog.this.getDialog().cancel();
					}
				});
				return builder.create();
			}
		}
		
		ServerSpinnerAdapter adapter;
		LoginControl callback;
		Button loginButton, registerButton;
		CheckBox advServer;
		Spinner serverList;
		
		public LoginFragment(LoginControl callback) {
			this.callback = callback;
		}
		
		public LoginFragment() {
			
		}
		
		ProgressBar activityIndicator;
		
		public void setProgressBarActivationStatus(boolean activ) {
			if (activityIndicator != null) {
				if (activ == false) {
					activityIndicator.setVisibility(View.INVISIBLE);
				} else {
					activityIndicator.setVisibility(View.VISIBLE);
				}
			}
		}
		
		public void activateButtons(boolean enabled) {
			if (loginButton != null && registerButton != null) {
				loginButton.setEnabled(enabled);
				registerButton.setEnabled(enabled);
			}
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_login, container, false);
			final SharedPreferences loginData = getActivity().getSharedPreferences("login_data", MODE_MULTI_PROCESS);
			final EditText user = (EditText) rootView.findViewById(R.id.usernameTxtIn);
			final CheckBox rememberBox = (CheckBox) rootView.findViewById(R.id.rememberTgl);
			advServer = (CheckBox) rootView.findViewById(R.id.advCheck);
			
			serverList = (Spinner) rootView.findViewById(R.id.serverSpinner);
			adapter = new ServerSpinnerAdapter(getActivity());
			serverList.setAdapter(adapter);
			serverList.setSelection(adapter.getPosition(loginPrefs.getString("server", "napalm2skynet.com")));
			serverList.setOnItemSelectedListener(new OnItemSelectedListener() {
				
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					if (position == parent.getCount() - 1) {
						AddServerDialog dialog = new AddServerDialog();
						dialog.show(getFragmentManager(), "AddServer");
						return;
					}
					String selcectedServer = (String) parent.getItemAtPosition(position);
					loginPrefs.edit().putString("server", selcectedServer).commit();
				}
				
				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
					
				}
			});
			advServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						serverList.setVisibility(View.VISIBLE);
					} else {
						serverList.setVisibility(View.INVISIBLE);
					}
				}
			});
			rememberBox.setChecked(loginData.getBoolean("remember", false));
			final EditText pass = (EditText) rootView.findViewById(R.id.passwordTxtIn);
			if (loginData.getBoolean("remember", false)) {
				user.setText(callback.getPrefs().getString(USERNAME_KEY, ""));
				pass.setText(callback.getPrefs().getString(PASSWORD_KEY, ""));
			}
			loginButton = (Button) rootView.findViewById(R.id.loginButton);
			registerButton = (Button) rootView.findViewById(R.id.registerBtn);
			loginButton.setEnabled(callback.isLicenseCheckValid());
			registerButton.setEnabled(callback.isLicenseCheckValid());
			activityIndicator = (ProgressBar) rootView.findViewById(R.id.progressSpinBar);
			activityIndicator.setIndeterminate(true);
			activityIndicator.setVisibility(View.INVISIBLE);
			loginButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					activityIndicator.setVisibility(View.VISIBLE);
					callback.startLogin(user.getText().toString(), pass.getText().toString());
					InputMethodManager mngr = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
					mngr.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
				}
			});
			registerButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getActivity(), RegisterActivity.class);
					intent.putExtra("server", getSelectedServer());
					startActivity(intent);
				}
			});
			rememberBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					loginData.edit().putBoolean("remember", isChecked).commit();
					
				}
			});
			
			return rootView;
		}
	}
	
	LoginTester checkLogin;
	
	@Override
	public void startLogin(String username, String password) {
		checkLogin = new LoginTester();
		checkLogin.addTestResultListener(this, getApplicationContext());
		String server = loginPrefs.getString("server", "napalm2skynet.com");
		checkLogin.execute(username, password, server);
	}
	
	RosterListFragment rlist;
	public static XMPPConnection conn;
	
	@Override
	public void onLoginSuccessful(String username, String password, final XMPPConnection conn) {
		critical.lock();
		// final String fusername = username, fpassword = password;
		loginPrefs.edit().putString(USERNAME_KEY, username).putString(PASSWORD_KEY, password).commit();
		// final XMPPConnection fconn = conn;
		final Activity current = this;
		final Roster2Activity r2a = this;
		
		XMPPLoginActivity.conn = conn;
		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				toggleServiceForeground(shouldServiceRun());
				visible = true;
				if (actionMenu != null) {
					actionMenu.findItem(R.id.action_logout).setVisible(visible);
					actionMenu.findItem(R.id.action_settings).setVisible(visible);
					actionMenu.findItem(R.id.add_user).setVisible(visible);
					// actionMenu.findItem(R.id.action_refresh).setVisible(visible);
					actionMenu.findItem(R.id.check_service_enabled).setVisible(visible);
				}
				
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
				if (loginFragment != null) {
					loginFragment.setProgressBarActivationStatus(false);
				}
				rlist = new RosterListFragment(conn.getRoster(), current.getApplicationContext(), r2a);
				XMPPLoginActivity.conn.addConnectionListener(rlist);
				getFragmentManager().beginTransaction().replace(R.id.container, rlist).commit();
			}
		});
		critical.unlock();
	}
	
	Lock critical = new ReentrantLock();
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		critical.lock();
		super.onSaveInstanceState(outState);
		// finish();
		
		critical.unlock();
	}
	
	@Override
	public void onLoginFailed() {
		critical.lock();
		final LoginControl callback = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (loginFragment != null) {
					loginFragment.setProgressBarActivationStatus(false);
				} else {
					loginFragment = new LoginFragment(callback);
					getFragmentManager().beginTransaction().replace(R.id.container, loginFragment).commit();
				}
			}
		});
		critical.unlock();
	}
	
	@Override
	public Roster getRoster() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void selectUser(int index) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void deleteEntry(String jid) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void requestUpdate(Object object) {
		if (conn != null) {
			Thread nwThrd = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						conn.disconnect();
					} catch (NotConnectedException e) {
						// TODO Auto-generated catch block
						Log.e("Reconnection.Disconnect", "Reconnect failed: not connected " + e.getMessage());
					}
				}
			});
			nwThrd.start();
			try {
				nwThrd.join(500);
			} catch (InterruptedException e) {
				Log.e("Reconnection.Disconnect", "Network thread join failed: " + e.getMessage());
			}
		}
		
		startLogin(loginPrefs.getString(USERNAME_KEY, ""), loginPrefs.getString(PASSWORD_KEY, ""));
	}
	
	Intent mServiceIntent;
	
	@Override
	public void toggleServiceForeground(boolean isChecked) {
		mServiceIntent = new Intent(this, de.napalm.geoxmpp_pp.RemoteGeoXMPPService.class); // new
																							// Intent(this,
																							// RemoteGeoXMPPService.class);
		
		SharedPreferences defaultPrefs = getSharedPreferences("app_default_prefs", MODE_MULTI_PROCESS);
		if (isChecked) {
			mServiceIntent.putExtra(USERNAME_KEY, loginPrefs.getString(USERNAME_KEY, ""));
			mServiceIntent.putExtra(PASSWORD_KEY, loginPrefs.getString(PASSWORD_KEY, ""));
			mServiceIntent.putExtra("sticky", defaultPrefs.getBoolean("foreground", true));
			startService(mServiceIntent);
		} else {
			stopService(mServiceIntent);
			getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().putBoolean("started", false).commit();
		}
		
	}
	
	@Override
	public String getOwnJid() {
		return loginPrefs.getString(USERNAME_KEY, "") + "@" + loginPrefs.getString("server", "napalm2skynet.com");
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
		// unregisterReceiver(receiver);
		critical.lock();
		if (checkLogin != null) {
			checkLogin.cancel(true);
		}
		critical.unlock();
		Thread disconThrd = new Thread(new Runnable() {
			
			@Override
			public void run() {
				critical.lock();
				try {
					if (conn != null) {
						conn.disconnect();
					}
				} catch (NotConnectedException e) {
					// TODO Auto-generated catch block
					Log.e("XMPP UI onPause", "Not Connected " + e.getMessage());
				}
				critical.unlock();
			}
		});
		disconThrd.start();
	}
	
	@Override
	public boolean isServiceRunning() {
		// TODO Auto-generated method stub
		return getSharedPreferences("service_data", MODE_MULTI_PROCESS).getBoolean("started", true);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.napalm.geoxmpp_pp.Roster2Activity#shouldServiceRun()
	 */
	@Override
	public boolean shouldServiceRun() {
		return getSharedPreferences("service_should_be_started", 0).getBoolean("should_be_started", true);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.napalm.geoxmpp_pp.Roster2Activity#setServiceShouldRun(boolean)
	 */
	@Override
	public void setServiceShouldRun(boolean running) {
		getSharedPreferences("service_should_be_started", 0).edit().putBoolean("should_be_started", running).commit();
		
	}
	
	@Override
	public SharedPreferences getPrefs() {
		// TODO Auto-generated method stub
		return loginPrefs;
	}
	
	@Override
	public boolean isLicenseCheckValid() {
		return licenseCheckValid;
	}
	
	@Override
	public void setLicenseCheckValid(final boolean licenseCheckValid) {
		this.licenseCheckValid = licenseCheckValid;
		mHandler.post(new Runnable() {
			
			@Override
			public void run() {
				
				if (licenseCheckValid) {
					if (loginFragment != null) {
						loginFragment.activateButtons(licenseCheckValid);
					}
					return;
				} else {
					loginFragment = new LoginFragment(XMPPLoginActivity.this);
					if (checkLogin != null) {
						checkLogin.cancel(true);
					}
					getFragmentManager().beginTransaction().replace(R.id.container, loginFragment).commit();
					loginFragment.activateButtons(licenseCheckValid);
				}
				
			}
		});
	}
	
	@Override
	public void logout(boolean deletePrefs) {
		visible = false;
		final LoginControl curObj = this;
		mHandler.post(new Runnable() {
			
			@Override
			public void run() {
				
				if (actionMenu != null) {
					actionMenu.findItem(R.id.action_logout).setVisible(visible);
					actionMenu.findItem(R.id.action_settings).setVisible(visible);
					actionMenu.findItem(R.id.add_user).setVisible(visible);
					// actionMenu.findItem(R.id.action_refresh).setVisible(visible);
					actionMenu.findItem(R.id.check_service_enabled).setVisible(visible);
				}
				loginPrefs.edit().putBoolean("remember", false);
				loginFragment = new LoginFragment(curObj);
				getFragmentManager().beginTransaction().replace(R.id.container, loginFragment).commit();
			}
		});
		
		Thread thrd = new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (conn != null) {
					try {
						conn.disconnect();
					} catch (NotConnectedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				toggleServiceForeground(false);
				getSharedPreferences("app_default_prefs", MODE_MULTI_PROCESS).edit().clear().commit();
				getSharedPreferences("login_data", MODE_MULTI_PROCESS).edit().clear().commit();
				getSharedPreferences("service_data", MODE_MULTI_PROCESS).edit().clear().commit();
				getSharedPreferences("contacts_locations", MODE_MULTI_PROCESS).edit().clear().commit();
			}
		});
		thrd.start();
		
	}
	
	@Override
	public String getSelectedServer() {
		return getSharedPreferences("login_data", MODE_MULTI_PROCESS).getString("server", "napalm2skynet.com");
	}
}
