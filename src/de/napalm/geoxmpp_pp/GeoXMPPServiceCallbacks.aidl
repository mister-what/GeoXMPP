package de.napalm.geoxmpp_pp;

interface GeoXMPPServiceCallbacks {
	oneway void loginSuccessful();
	oneway void loginFailed();
	
	oneway void entryAdded(String jid);
	oneway void entryRemoved(String jid);
	oneway void presenceChanged(String jid, boolean isOnline);
}