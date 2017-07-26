package de.napalm.geoxmpp_pp;

import org.jivesoftware.smack.XMPPConnection;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

/**
 * Helper class for showing and canceling geo xmppforeground notifications.
 * <p>
 * This class makes heavy use of the {@link NotificationCompat.Builder} helper
 * class to create notifications in a backward-compatible way.
 */
public class GeoXMPPForeground {
	public static final int XMPP_STATE_OFFLINE = 0;
	public static final int XMPP_STATE_ONLINE = 1;
	public static final int XMPP_STATE_FAILED = 2;
	public static final int XMPP_STATE_RECONNECTING_IN = 3;
	private String username;
	private XMPPConnection conn;
	
	public GeoXMPPForeground(String username) {
		this.username = username;
		this.conn = null;
	}
	
	public GeoXMPPForeground(String username, XMPPConnection conn) {
		this.username = username;
		this.conn = conn;
	}
	
	/**
	 * Shows the notification, or updates a previously shown notification of
	 * this type, with the given parameters.
	 * <p>
	 * TODO: Customize this method's arguments to present relevant content in
	 * the notification.
	 * <p>
	 * TODO: Customize the contents of this method to tweak the behavior and
	 * presentation of geo xmppforeground notifications. Make sure to follow the
	 * <a
	 * href="https://developer.android.com/design/patterns/notifications.html">
	 * Notification design guidelines</a> when doing so.
	 *
	 * @see #cancel(Context)
	 */
	public Notification getNotification(final Context context, final int state, int reconnectingInSeconds) {
		final Resources res = context.getResources();
		Bitmap picture;
		int icon;
		String notificationText;
		String notificationTitle = "GeoXMPP++: " + username;
		switch (state) {
			case XMPP_STATE_OFFLINE:
				picture = BitmapFactory.decodeResource(res, R.drawable.ic_launcher_offline);
				icon = R.drawable.ic_stat_geo_xmppforeground_offline;
				notificationText = "Offline!";
				break;
			case XMPP_STATE_ONLINE:
				picture = BitmapFactory.decodeResource(res, R.drawable.ic_stat_geo_xmppforeground_online);
				icon = R.drawable.ic_stat_geo_xmppforeground_online;
				notificationText = "Online";
				if (conn != null) {
					if (conn.isSecureConnection()) {
						notificationText = notificationText + " (secure connection via TLS/SSL)";
					}
				}
				break;
			case XMPP_STATE_FAILED:
				picture = BitmapFactory.decodeResource(res, R.drawable.ic_launcher_offline);
				icon = R.drawable.ic_stat_geo_xmppforeground_offline;
				notificationText = "Connection failed";
				break;
			case XMPP_STATE_RECONNECTING_IN:
				picture = BitmapFactory.decodeResource(res, R.drawable.ic_launcher_offline);
				icon = R.drawable.ic_stat_geo_xmppforeground_offline;
				notificationText = "Trying to reconnect in " + reconnectingInSeconds + " seconds.";
				break;
			default:
				picture = BitmapFactory.decodeResource(res, R.drawable.ic_launcher);
				icon = R.drawable.ic_stat_geo_xmppforeground;
				notificationText = "???";
				break;
		}
		Intent intent = new Intent(context.getApplicationContext(), XMPPLoginActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(XMPPLoginActivity.class);
		stackBuilder.addNextIntent(intent);
		// PendingIntent pintent = stackBuilder.getPendingIntent(0,
		// PendingIntent.FLAG_UPDATE_CURRENT);
		Notification.Builder noteBuilder = new Notification.Builder(context);
		Intent restart = new Intent(context.getApplicationContext(), RemoteGeoXMPPService.class);
		restart.putExtra("restart", true);
		noteBuilder.setSmallIcon(icon);
		noteBuilder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context.getApplicationContext(), XMPPLoginActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT));
		noteBuilder.addAction(R.drawable.ic_action_update, "Restart",
				PendingIntent.getService(context.getApplicationContext(), 0, restart, PendingIntent.FLAG_UPDATE_CURRENT));
		noteBuilder.addAction(R.drawable.ic_action_map, "Map", PendingIntent.getActivity(context.getApplicationContext(), 666,
				new Intent(context.getApplicationContext(), XMPPMapActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
		noteBuilder.setContentTitle(notificationTitle).setContentText(notificationText);
		noteBuilder.setPriority(Notification.PRIORITY_HIGH);
		return noteBuilder.build();
	}
	
	public Notification getRosterEntryAddedNotification(String jid) {
		return null;
	}
}