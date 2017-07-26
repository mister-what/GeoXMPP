package de.napalm.geoxmpp_pp;

import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;

public class ServerSpinnerAdapter extends ArrayAdapter<String> {
	
	SharedPreferences loginPrefs;
	Context context;
	
	public ServerSpinnerAdapter(Context context) {
		super(context, android.R.layout.simple_spinner_item);
		this.context = context;
		loginPrefs = context.getApplicationContext().getSharedPreferences("login_data", Context.MODE_MULTI_PROCESS);
		TreeSet<String> defaultServers = new TreeSet<>();
		Set<String> savedServers = loginPrefs.getStringSet("user_servers", defaultServers);
		this.addAll(savedServers);
		this.add("napalm2skynet.com");
		this.add("<new server>");
		this.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	}
	
	@Override
	public void notifyDataSetChanged() {
		this.setNotifyOnChange(false);
		this.clear();
		TreeSet<String> defaultServers = new TreeSet<>();
		Set<String> savedServers = loginPrefs.getStringSet("user_servers", defaultServers);
		this.addAll(savedServers);
		this.add("napalm2skynet.com");
		this.add("<new server>");
		super.notifyDataSetChanged();
		this.setNotifyOnChange(true);
	}
	
}
