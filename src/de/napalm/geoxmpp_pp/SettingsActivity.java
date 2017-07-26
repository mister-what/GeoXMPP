package de.napalm.geoxmpp_pp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */

public class SettingsActivity extends Activity implements OnSeekBarChangeListener, OnEditorActionListener {
	
	Switch passiveSwitch;
	SeekBar timeRes, spaceRes;
	EditText timeOutlet, spaceOutlet;
	RadioGroup markerChooser;
	ImageView markerViewer;
	SharedPreferences defaultPrefs;
	// Intent parentActivityIntent;
	boolean started;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		started = getSharedPreferences("service_data", MODE_MULTI_PROCESS).getBoolean("started", false);
		setContentView(R.layout.settings);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		defaultPrefs = getSharedPreferences("app_default_prefs", MODE_MULTI_PROCESS);
		// defaultPrefs.edit().putBoolean("prefsLocked", true).commit();
		passiveSwitch = (Switch) findViewById(R.id.passiveSwitch);
		timeOutlet = (EditText) findViewById(R.id.timeOutlet);
		spaceOutlet = (EditText) findViewById(R.id.spaceOutlet);
		passiveSwitch.setChecked(defaultPrefs.getBoolean("passiveOn", true));
		markerViewer = (ImageView) findViewById(R.id.markerImage);
		markerChooser = (RadioGroup) findViewById(R.id.markerRadio);
		int choice = (int) defaultPrefs.getLong("marker_icon", 1);
		
		// preset
		switch (choice) {
			case 3:
				markerViewer.setImageResource(R.drawable.ic_marker_two);
				markerChooser.check(R.id.radio0);
				break;
			case 2:
				markerViewer.setImageResource(R.drawable.ic_marker_one);
				markerChooser.check(R.id.radio1);
				break;
			case 1:
				markerViewer.setImageResource(R.drawable.ic_marker_default);
				markerChooser.check(R.id.radio2);
				break;
			default:
				markerViewer.setImageResource(R.drawable.ic_marker_two);
				markerChooser.check(R.id.radio0);
				break;
		}
		
		// hier wird der marker mit einem fingertipp auf den radiobutton
		// ausgesucht.
		markerChooser.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				int choice = checkedId;
				switch (choice) {
					case R.id.radio0:
						markerViewer.setImageResource(R.drawable.ic_marker_two);
						defaultPrefs.edit().putLong("marker_icon", 3).commit();
						markerViewer.refreshDrawableState();
						break;
					case R.id.radio1:
						markerViewer.setImageResource(R.drawable.ic_marker_one);
						defaultPrefs.edit().putLong("marker_icon", 2).commit();
						markerViewer.refreshDrawableState();
						break;
					case R.id.radio2:
						markerViewer.setImageResource(R.drawable.ic_marker_default);
						defaultPrefs.edit().putLong("marker_icon", 1).commit();
						markerViewer.refreshDrawableState();
						break;
					default:
						markerViewer.setImageResource(R.drawable.ic_marker_two);
						defaultPrefs.edit().putLong("marker_icon", 3).commit();
						markerViewer.refreshDrawableState();
						break;
				}
			}
		});
		
		passiveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				defaultPrefs.edit().putBoolean("passiveOn", isChecked).commit();
			}
		});
		
		timeRes = (SeekBar) findViewById(R.id.timeSlider);
		timeRes.setMax(60);
		timeRes.setProgress(defaultPrefs.getInt("timeResValue", 15));
		timeOutlet.setText(String.valueOf(defaultPrefs.getInt("timeResValue", 15)));
		spaceOutlet.setText(String.valueOf(defaultPrefs.getInt("spaceResValue", 15)));
		spaceRes = (SeekBar) findViewById(R.id.spaceSlider);
		spaceRes.setMax(60);
		spaceRes.setProgress(defaultPrefs.getInt("spaceResValue", 15));
		spaceRes.setOnSeekBarChangeListener(this);
		timeRes.setOnSeekBarChangeListener(this);
		timeOutlet.setOnEditorActionListener(this);
		spaceOutlet.setOnEditorActionListener(this);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onNavigateUp()
	 */
	@Override
	public boolean onNavigateUp() {
		setResult(RESULT_OK);
		
		boolean result = super.onNavigateUp();
		// Intent intent = new Intent(this, RemoteGeoXMPPService.class);
		
		finish();
		return result;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		// Intent upIntent = new Intent(getApplicationContext(),
		// XMPPLoginActivity.class);
		// navigateUpTo(upIntent);
		setResult(RESULT_OK);
		finish();
		return;
		
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			if (seekBar.getId() == R.id.timeSlider) {
				defaultPrefs.edit().putInt("timeResValue", progress).commit();
				timeOutlet.setText(String.valueOf(progress));
			} else if (seekBar.getId() == R.id.spaceSlider) {
				defaultPrefs.edit().putInt("spaceResValue", progress).commit();
				spaceOutlet.setText(String.valueOf(progress));
			}
		}
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.controller, menu);
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		
		int id = item.getItemId();
		if (id == R.id.credits_menu_item) {
			Intent intent = new Intent(this, CreditsActivity.class);
			startActivity(intent);
			super.onOptionsItemSelected(item);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
		
	}
	
	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (v.getId() == R.id.timeOutlet) {
			int timeInt = Integer.parseInt(v.getText().toString());
			if (timeInt > 300) {
				timeInt = 300;
			}
			if (timeInt <= 0) {
				timeInt = 1;
			}
			defaultPrefs.edit().putInt("timeResValue", timeInt).commit();
			timeRes.setProgress(timeInt);
			return true;
		} else if (v.getId() == R.id.spaceOutlet) {
			int spaceInt = Integer.parseInt(v.getText().toString());
			if (spaceInt > 500) {
				spaceInt = 500;
			}
			if (spaceInt <= 0) {
				spaceInt = 1;
			}
			defaultPrefs.edit().putInt("spaceResValue", spaceInt).commit();
			spaceRes.setProgress(spaceInt);
			return true;
		} else {
			return false;
		}
		
	}
	
}
