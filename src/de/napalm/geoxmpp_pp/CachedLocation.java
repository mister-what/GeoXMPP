package de.napalm.geoxmpp_pp;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.google.android.gms.maps.model.LatLng;

import android.content.Intent;

public class CachedLocation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String jid;
	double lat, lng;
	int marker;
	long timestamp;
	
	public CachedLocation(String jid, double lat, double lng, int marker, long timestamp) {
		this.jid = jid;
		this.lat = lat;
		this.lng = lng;
		this.marker = marker;
		this.timestamp = timestamp;
	}

	/**
	 * @return the jid
	 */
	public String getJid() {
		return jid;
	}

	/**
	 * @return the marker
	 */
	public int getMarker() {
		return marker;
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	public LatLng getLatLng() {
		return new LatLng(lat, lng);
	}
}


/*try {
FileOutputStream fos = new FileOutputStream(cache);
ObjectOutputStream oos = new ObjectOutputStream(fos);
oos.writeObject(locationCache);
oos.close();
} catch (FileNotFoundException e) {
// TODO Auto-generated catch block
e.printStackTrace();
} catch (IOException e) {
// TODO Auto-generated catch block
e.printStackTrace();
}*/