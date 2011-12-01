package uk.ac.cam.cl.gpsexperiments;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

public class EventSaver {
	public static String TAG = "GpsExperiments";
	
	private FileWriter file;
	
	public EventSaver(String phoneId) throws IOException {
		new File("/sdcard/gpsexperiments").mkdirs();
		file = new FileWriter("/sdcard/gpsexperiments/gpsevents-" + phoneId + "-" + Utils.currentTimestampString(), true);
	}
	
	public synchronized void close() {
		try {
			file.close();
		} catch (IOException e) {
			Log.e(TAG, "Cannot close file.", e);
		}
	}
	
	synchronized void save(String eventType, Object extras) throws JSONException {
		try {
			// If extras is null, the data field is not added.
			// If extras is JSONObject.NULL, the data field is "data": null.
			JSONObject json = new JSONObject();
			json.put("log_version", "2010-09-24");
			json.put("time_phone", System.currentTimeMillis());
			json.put("event", eventType);
			json.put("extras", extras);
			// Note: with int argument json.toString prettyprints! Remove argument to save space.
			file.write(json.toString(2) + '\0');
		} catch (IOException e) {
			Log.e(TAG, "Cannot write to file.", e);
		}
	}
	
	public void experimentStartedOrStopped(boolean started, int id, long sleepTime, boolean deleteAGPS, long eventCollectionTime) {
		try {
			JSONObject json = new JSONObject();
			json.put("experiment_id", id);
			json.put("sleep_time", sleepTime);
			json.put("delete_agps", deleteAGPS);
			json.put("events_time", eventCollectionTime);
	
			if (started)
				save("experiment_started", json);
			else
				save("experiment_stopped", json);
		} catch (JSONException e) {
			Log.e(TAG, "Cannot serialize to JSON", e);
		}
	}
	
	public void experimentRunStartedOrStopped(boolean started, boolean cancelled, int numExperiments, int airplaneMode) {
		try {
			JSONObject json = new JSONObject();
			json.put("num_experiments", numExperiments);
			json.put("airplane_mode", airplaneMode);
			if (started)
				save("experimentrun_started", json);
			else if (cancelled)
				save("experimentrun_cancelled", json);
			else
				save("experimentrun_finished", json);
		} catch (JSONException e) {
			Log.e(TAG, "Cannot serialize to JSON", e);
		}
	}
	
	public void requestedGps(String provider, long delayPeriod, float minDistance) {
		try {
			JSONObject json = new JSONObject();
			json.put("provider", provider);
			json.put("request_delay", delayPeriod);
			json.put("request_min_distance", minDistance);
			save("gps_requested", json);
		} catch (JSONException e) {
			Log.e(TAG, "Cannot serialize to JSON", e);
		}
	}
	
	public void removedGps() {
		try {
			save("gps_removed", null);
		} catch (JSONException e) {
			Log.e(TAG, "Cannot serialize to JSON", e);
		}
	}
	
	public void deletedAgpsData() {
		try {
			save("agps_delete", null);
		} catch (JSONException e) {
			Log.e(TAG, "Cannot serialize to JSON", e);
		}
	}
	
	public void onLocationChanged(Location location) {
		try {
			if (location != null) {
				JSONObject json = new JSONObject();
				json.put("latitude", location.getLatitude());
				json.put("longitude", location.getLongitude());
				json.put("time", location.getTime());
				json.put("provider", location.getProvider());
				if (location.hasAccuracy()) json.put("accuracy", location.getAccuracy());
				if (location.hasAltitude()) json.put("altitude", location.getAltitude());
				if (location.hasBearing()) json.put("bearing", location.getBearing());
				if (location.hasSpeed()) json.put("speed", location.getSpeed());
				json.put("extras", bundleJson(location.getExtras())); // Omitted if extras == null
				save("location_changed", json);
			} else {
				save("location_changed", null);
			}
		} catch (JSONException e) {
			Log.e(TAG, "Cannot serialize to JSON", e);
		}
	}
	
	public void onGpsStatusChanged(int event, GpsStatus status) {
		try {
			JSONObject json = new JSONObject();
			switch (event) {
				case GpsStatus.GPS_EVENT_FIRST_FIX: json.put("gpsevent", "first_fix"); break;
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS: json.put("gpsevent", "satellite_status"); break;
				case GpsStatus.GPS_EVENT_STARTED: json.put("gpsevent", "started"); break;
				case GpsStatus.GPS_EVENT_STOPPED: json.put("gpsevent", "stopped"); break;
				default: json.put("gpsevent", event);
			}
			if (status != null) {
				JSONObject statj = new JSONObject();
				statj.put("timetofirstfix", status.getTimeToFirstFix());				
				JSONArray birds = new JSONArray();				
				for (GpsSatellite sat : status.getSatellites()) {
					JSONObject birdj = new JSONObject();					
					birdj.put("azimuth", sat.getAzimuth());
					birdj.put("elevation", sat.getElevation());
					birdj.put("prn", sat.getPrn());
					birdj.put("snr", sat.getSnr());
					birdj.put("almanac", sat.hasAlmanac());
					birdj.put("ephermis", sat.hasEphemeris());
					birdj.put("usedinfix", sat.usedInFix());
					birds.put(birdj);
				}				
				statj.put("satellites", birds);
				json.put("gpsstatus", statj);
			}
			save("gpsstatus_changed", json);
		} catch (JSONException e) {
			Log.e(TAG, "Cannot serialize to JSON", e);
		}
	}
	
	public void onNmeaReceived(long timestamp, String nmea) {
		try {
			JSONObject json = new JSONObject();
			json.put("time", timestamp);
			json.put("nmea", nmea);
			save("nmeareceived", json);
		} catch (JSONException e) {
			Log.e(TAG, "Cannot serialize to JSON", e);
		}
	}
	
	public static JSONObject bundleJson(Bundle bundle) throws JSONException {
		if (bundle == null)
			return null; // Real nulls are omitted from JSON objects.
		else {
			JSONObject json = new JSONObject();
			for (String key : bundle.keySet()) {
				json.put(key, bundle.get(key));
			}
			return json;
		}
	}
}
