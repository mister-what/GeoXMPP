package de.napalm.geoxmpp_pp;

import java.io.IOException;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity implements OnClickListener {
	
	class PureConnector extends AsyncTask<Void, Void, XMPPConnection> {
		
		RegisterActivity parent;
		String username;
		String password;
		
		/**
		 * @param parent
		 */
		public PureConnector(RegisterActivity parent, String... userCreds) {
			this.parent = parent;
			this.username = userCreds[0];
			this.password = userCreds[1];
		}
		
		@Override
		protected XMPPConnection doInBackground(Void... params) {
			ConnectionConfiguration conf;
			XMPPConnection conn;
			conf = new ConnectionConfiguration(server, 5222);
			// conf.setSecurityMode(SecurityMode.enabled);
			conf.setKeystoreType("AndroidCAStore");
			conf.setKeystorePath(null);
			conn = new XMPPTCPConnection(conf);
			try {
				conn.connect();
			} catch (SmackException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			return conn;
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(XMPPConnection result) {
			if (result != null) {
				AccountManager mngr = AccountManager.getInstance(result);
				try {
					mngr.createAccount(username, password);
				} catch (NoResponseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					parent.creationFailed("No response from server.");
					return;
				} catch (XMPPErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					parent.creationFailed("Name not available.");
					return;
				} catch (NotConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					parent.creationFailed("Server unreachable.");
					return;
				}
				parent.creationSuccessful();
			}
		}
		
	}
	
	XMPPConnection mConn;
	EditText usernameEdit, passwordEdit, passwordConfEdit;
	Button regBtn, cancelBtn;
	TextView errorTextDisplay, serverName;
	ProgressBar regProgress;
	String server;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_register);
		server = getIntent().getStringExtra("server");
		if (server == null) {
			server = "napalm2skynet.com";
		}
		serverName = (TextView) findViewById(R.id.server_name);
		serverName.setText(server);
		usernameEdit = (EditText) findViewById(R.id.registerUsernameEditText);
		passwordEdit = (EditText) findViewById(R.id.registerPasswordEditText);
		passwordConfEdit = (EditText) findViewById(R.id.registerPasswordConfirmEditText);
		regBtn = (Button) findViewById(R.id.confirmRegisterBtn);
		regBtn.setOnClickListener(this);
		cancelBtn = (Button) findViewById(R.id.cancelRegisterBtn);
		cancelBtn.setOnClickListener(this);
		errorTextDisplay = (TextView) findViewById(R.id.registerErrorText);
		regProgress = (ProgressBar) findViewById(R.id.registerProgressCircle);
		regProgress.setVisibility(View.INVISIBLE);
	}
	
	void creationSuccessful() {
		Toast tmsg = Toast.makeText(getApplicationContext(), "User created.", Toast.LENGTH_SHORT);
		tmsg.show();
		finish();
	}
	
	void creationFailed(String msg) {
		regProgress.setVisibility(View.INVISIBLE);
		errorTextDisplay.setText(msg);
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == regBtn.getId()) {
			if (passwordEdit.getText().toString().equals(passwordConfEdit.getText().toString())) {
				if (usernameEdit.getText().length() >= 3) {
					PureConnector pConn = new PureConnector(this, usernameEdit.getText().toString(), passwordEdit.getText().toString());
					regProgress.setVisibility(View.VISIBLE);
					pConn.execute();
				} else {
					creationFailed("Username too short!");
				}
			} else {
				creationFailed("Passwords don't match!");
			}
		} else {
			finish();
		}
		
	}
	
}
