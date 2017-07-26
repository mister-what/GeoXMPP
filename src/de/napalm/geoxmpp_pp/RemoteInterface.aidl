package de.napalm.geoxmpp_pp;
import de.napalm.geoxmpp_pp.GeoXMPPServiceCallbacks;

interface RemoteInterface {
	void registerCallback(in GeoXMPPServiceCallbacks mCallback);
	oneway void unregisterCallback();
	
	oneway void checkLogin();
	
	oneway void addUser(String jid);
	oneway void removeUser(String jid);
	boolean getOnlineStatus(String jid);
	List<String> getRosterEntries();
}
