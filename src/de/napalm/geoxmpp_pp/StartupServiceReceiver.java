package de.napalm.geoxmpp_pp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupServiceReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		boolean serviceEnabled = context.getApplicationContext().getSharedPreferences("service_should_be_started", 0).getBoolean("should_be_started", true);
		if (!serviceEnabled) {
			return;
		}
		Log.i("broadcast.reciever.xmpp", "WAKEUP!!!");
		Intent xmppServiceIntent = new Intent(context, RemoteGeoXMPPService.class);
		context.startService(xmppServiceIntent);
	}
	
}
