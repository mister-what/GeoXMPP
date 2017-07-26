package de.napalm.geoxmpp_pp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

class RosterDataObject implements Comparable<RosterDataObject> {
	String jid;
	boolean online, sharingEnabled;
	
	public RosterDataObject(String jid) {
		this.jid = jid;
	}
	
	/**
	 * @return the online
	 */
	public boolean isOnline() {
		return online;
	}
	
	/**
	 * @param online
	 *            the online to set
	 */
	public void setOnline(boolean online) {
		this.online = online;
	}
	
	/**
	 * @return the sharingEnabled
	 */
	public boolean isSharingEnabled() {
		return sharingEnabled;
	}
	
	/**
	 * @param sharingEnabled
	 *            the sharingEnabled to set
	 */
	public void setSharingEnabled(boolean sharingEnabled) {
		this.sharingEnabled = sharingEnabled;
	}
	
	/**
	 * @return the jid
	 */
	public String getJid() {
		return jid;
	}
	
	public RosterDataObject(String jid, boolean online, boolean sharingEnabled) {
		this.jid = jid;
		this.online = online;
		this.sharingEnabled = sharingEnabled;
	}
	
	@Override
	public int compareTo(RosterDataObject b) {
		if (b.getJid() == null && this.getJid() == null) {
			return 0;
		}
		if (this.getJid() == null) {
			return 1;
		}
		if (b.getJid() == null) {
			return -1;
		}
		return this.getJid().compareTo(b.getJid());
	}
	
}

class ContextMenuDialog extends DialogFragment {
	
	RosterDataObject item;
	CheckBox enableSharingBox;
	Roster roster;
	String options[] = { "Delete", "Enable Sharing", "Disable Sharing", "Share contact with..." };
	
	public ContextMenuDialog(RosterDataObject item, Roster roster, CheckBox enableSharingBox) {
		this.item = item;
		this.roster = roster;
		this.enableSharingBox = enableSharingBox;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(StringUtils.parseName(item.getJid())).setItems(options, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0:
						try {
							roster.removeEntry(roster.getEntry(item.getJid()));
						} catch (NotLoggedInException | NoResponseException | XMPPErrorException | NotConnectedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case 1:
						// getActivity().getSharedPreferences("contacts_locations",
						// Context.MODE_MULTI_PROCESS).edit().putBoolean(item.getJid()+"_isSharingEnabled",
						// true).commit();
						enableSharingBox.setChecked(true);
						break;
					case 2:
						enableSharingBox.setChecked(false);
						break;
					default:
						break;
				}
				
			}
		}).setNeutralButton("Dismiss", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder.create();
	}
}

@SuppressLint("ViewHolder")
public class RosterCursorAdapter extends BaseAdapter implements RosterListener {
	
	ArrayList<RosterDataObject> listItems;
	ActionMode mActionMode;
	Roster roster;
	Context context;
	View parent;
	SharedPreferences contactsData;
	String ownJid;
	
	public RosterCursorAdapter(Roster roster, Context context, View parentView, String ownJid) {
		super();
		contactsData = context.getSharedPreferences("contacts_locations", Context.MODE_MULTI_PROCESS);
		this.roster = roster;
		this.context = context;
		parent = parentView;
		roster.addRosterListener(this);
		this.ownJid = ownJid;
		
		populate();
	}
	
	private void populate() {
		listItems = new ArrayList<RosterDataObject>();
		Collection<RosterEntry> entries = roster.getEntries();
		for (Iterator<RosterEntry> iterator = entries.iterator(); iterator.hasNext();) {
			RosterEntry rosterEntry = iterator.next();
			String jid = StringUtils.parseBareAddress(rosterEntry.getUser());
			boolean online = roster.getPresence(jid).isAvailable();
			roster.createGroup("sharingEnabled");
			boolean sharing = roster.getGroup("sharingEnabled").contains(jid);
			RosterDataObject rosterObj = new RosterDataObject(jid, online, sharing);
			listItems.add(rosterObj);
		}
		Collections.sort(listItems);
	}
	
	public void addUser(String jid) {
		try {
			if (contactsData.getBoolean(jid + "_isSharingEnabled", false)) {
				String[] groups = { "sharingEnabled" };
				roster.createEntry(jid, jid, groups);
			} else {
				roster.createEntry(jid, jid, null);
			}
			populate();
		} catch (NotLoggedInException e) {
			Log.e("Roster.Adapter", "NotLoggedInException " + e.getMessage());
		} catch (NoResponseException e) {
			Log.e("Roster.Adapter", "NoResponseException " + e.getMessage());
		} catch (XMPPErrorException e) {
			Log.e("Roster.Adapter", "XMPPErrorException " + e.getMessage());
		} catch (NotConnectedException e) {
			Log.e("Roster.Adapter", "NotConnectedException " + e.getMessage());
		}
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return listItems.size();
	}
	
	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return listItems.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	
	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		View rootView;// = convertView;
		// if(rootView == null) {
		LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		rootView = mInflater.inflate(R.layout.drawer_roster_list_item, null);
		
		// }
		
		final String jid = listItems.get(position).getJid();
		
		final CheckBox jidCheck = (CheckBox) rootView.findViewById(R.id.checkBox1);
		// final CheckBox enableSharingBox = (CheckBox)
		// rootView.findViewById(R.id.imageToggleSharingBtn);
		
		final boolean isOnline = listItems.get(position).isOnline();
		final RosterDataObject obj = listItems.get(position);
		if (context.getApplicationContext().getSharedPreferences("login_data", Context.MODE_MULTI_PROCESS).getString("server", "napalm2skynet.com")
				.equalsIgnoreCase(StringUtils.parseServer(jid))) {
			jidCheck.setText(StringUtils.parseName(jid));
		} else {
			jidCheck.setText(StringUtils.parseBareAddress(jid));
		}
		
		// roster.createGroup("sharingEnabled");
		// enableSharingBox.setChecked(roster.getGroup("sharingEnabled").contains(jid));
		if (isOnline) {
			jidCheck.setTextColor(Color.WHITE);
		} else {
			jidCheck.setTextColor(Color.DKGRAY);
		}
		jidCheck.setChecked(contactsData.getBoolean(jid + "_isSharingEnabled", false));
		jidCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				contactsData.edit().putBoolean(jid + "_isSharingEnabled", jidCheck.isChecked()).commit();
				Log.i("Roster.Sharing", jid + "_isSharingEnabled == " + jidCheck.isChecked());
			}
		});
		rootView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				MenuInflater mInflater = new MenuInflater(context);
				mInflater.inflate(R.menu.roster_context, menu);
				menu.setHeaderTitle(StringUtils.parseName(jid));
				menu.findItem(R.id.context_delete).setOnMenuItemClickListener(new OnMenuItemClickListener() {
					
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						try {
							roster.removeEntry(roster.getEntry(jid));
						} catch (NotLoggedInException | NoResponseException | XMPPErrorException | NotConnectedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return false;
					}
				});
				menu.findItem(R.id.context_sharing).setChecked(contactsData.getBoolean(jid + "_isSharingEnabled", false));
				menu.findItem(R.id.context_sharing).setOnMenuItemClickListener(new OnMenuItemClickListener() {
					
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						contactsData.edit().putBoolean(jid + "_isSharingEnabled", !contactsData.getBoolean(jid + "_isSharingEnabled", false)).commit();
						jidCheck.setChecked(contactsData.getBoolean(jid + "_isSharingEnabled", false));
						
						Log.i("Roster.Sharing", jid + "_isSharingEnabled == " + jidCheck.isChecked());
						return false;
					}
				});
			}
		});
		return rootView;
	}
	
	@Override
	public void entriesAdded(Collection<String> addresses) {
		
		parent.post(new Runnable() {
			@Override
			public void run() {
				populate();
				notifyDataSetChanged();
			}
		});
		
	}
	
	@Override
	public void entriesUpdated(Collection<String> addresses) {
		
		parent.post(new Runnable() {
			@Override
			public void run() {
				populate();
				notifyDataSetChanged();
			}
		});
	}
	
	@Override
	public void entriesDeleted(Collection<String> addresses) {
		
		parent.post(new Runnable() {
			@Override
			public void run() {
				populate();
				notifyDataSetChanged();
			}
		});
		
	}
	
	@Override
	public void presenceChanged(Presence presence) {
		/*
		 * boolean isOnline = presence.isAvailable(); String jid =
		 * StringUtils.parseBareAddress(presence.getFrom()); for(int i = 0; i <
		 * listItems.size(); i++) {
		 * if(jid.equalsIgnoreCase(listItems.get(i).getJid())) {
		 * listItems.get(i).setOnline(isOnline); break; } }
		 */
		
		parent.post(new Runnable() {
			@Override
			public void run() {
				populate();
				notifyDataSetChanged();
			}
		});
	}
}
