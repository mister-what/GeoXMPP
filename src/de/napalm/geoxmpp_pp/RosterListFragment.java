package de.napalm.geoxmpp_pp;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

interface Roster2Activity {
	Roster getRoster();
	
	String getOwnJid();
	
	boolean shouldServiceRun();
	
	void setServiceShouldRun(boolean running);
	
	boolean isServiceRunning();
	
	void selectUser(int index);
	
	void deleteEntry(String jid);
	
	void requestUpdate(Object object);
	
	void toggleServiceForeground(boolean isChecked);
}

public class RosterListFragment extends Fragment implements ConnectionListener {
	
	boolean running;
	public RosterCursorAdapter mRosterCursorAdapter;
	Roster2Activity mCallbacks;
	Roster roster;
	Context context;
	
	public RosterListFragment() {
		
	}
	
	public RosterListFragment(Roster roster, Context context, Roster2Activity mCallbacks) {
		this.roster = roster;
		this.context = context;
		this.mCallbacks = mCallbacks;
		if (roster.getEntryCount() == 0) {
			try {
				roster.createEntry(StringUtils.parseBareAddress(this.mCallbacks.getOwnJid()), StringUtils.parseBareAddress(this.mCallbacks.getOwnJid()), null);
			} catch (NotLoggedInException | NoResponseException | XMPPErrorException | NotConnectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater,
	 * android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_roster_list, container, false);
		final ListView rosterList = (ListView) rootView.findViewById(R.id.rosterList);
		// rosterList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		mRosterCursorAdapter = new RosterCursorAdapter(roster, context, rosterList, mCallbacks.getOwnJid());
		
		// ImageButton refreshButton =
		// (ImageButton)rootView.findViewById(R.id.refreshBtn);
		// Switch foregroundService =
		// (Switch)rootView.findViewById(R.id.foregroundTgl);
		rosterList.setAdapter(mRosterCursorAdapter);
		// running = mCallbacks.shouldServiceRun();
		getActivity().registerForContextMenu(rosterList);
		
		// mCallbacks.toggleServiceForeground(running);
		
		// foregroundService.setChecked(running);
		// foregroundService.setChecked(defaultPrefs.getBoolean("foreground_service",
		// false));
		// foregroundService.setOnCheckedChangeListener(new
		// CompoundButton.OnCheckedChangeListener() {
		// @Override
		// public void onCheckedChanged(CompoundButton buttonView, boolean
		// isChecked) {
		// mCallbacks.toggleServiceForeground(isChecked);
		// mCallbacks.setServiceShouldRun(isChecked);
		// //context.getSharedPreferences("service_data",
		// 0).edit().putBoolean("started", isChecked).commit();
		// }
		// });
		// refreshButton.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// mCallbacks.toggleServiceForeground(false);
		// mCallbacks.setServiceShouldRun(false);
		// mCallbacks.requestUpdate(this);
		// SharedPreferences persistent =
		// context.getSharedPreferences("cached_locations",
		// Context.MODE_MULTI_PROCESS);
		// persistent.edit().clear().commit();
		// mCallbacks.toggleServiceForeground(true);
		// mCallbacks.setServiceShouldRun(true);
		// }
		// });
		
		return rootView;
	}
	
	@Override
	public void connected(XMPPConnection connection) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void authenticated(XMPPConnection connection) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void connectionClosed() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void connectionClosedOnError(Exception e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void reconnectingIn(int seconds) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void reconnectionSuccessful() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void reconnectionFailed(Exception e) {
		// TODO Auto-generated method stub
		
	}
	
}
