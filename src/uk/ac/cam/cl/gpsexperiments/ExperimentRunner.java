package uk.ac.cam.cl.gpsexperiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ExperimentRunner implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener {

	public static String TAG = "GpsExperiments";
	
	// GPS Parameters
	static String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
	static long GPS_REQUEST_DELAY = 100;
	static float GPS_REQUEST_MIN_DISTANCE = 0.0f;
	
	// Experiment parameters	
	ArrayList<Long> sleepTimes = makeSleepTimes();
	boolean deleteAGPS = false;
	long eventCollectionPeriod = 30*1000;
	
	static ArrayList<Long> makeSleepTimes() {
		ArrayList<Long> millis = new ArrayList<Long>();
		final int repeats = 1; // Let's add everything multiple times
		
		for (int i = 0; i < repeats; ++i) {
			// Simple seconds
			//for (long s = 1; s < 5; s += 1)
			//	millis.add(s * 1000);
			
			// Interesting 6 second boundary
			for (long ms = 100; ms <= 20000; ms += 100)
				millis.add(ms);

			for (long s = 21; s < 60; s += 2)
				millis.add(s * 1000);
		}
		//Collections.reverse(millis);
		Collections.shuffle(millis);

		//millis.add(1 * 60 * 60 * 1000L);
		//millis.add(2 * 60 * 60 * 1000L);
		
		// 0-wait experiments at start to init the experiment cycle
		millis.add(0, 0L);
		millis.add(0, 0L);
		millis.add(0, 0L);
		return millis;
	}
	
	// Current state
	int currentExperiment;
	EventSaver eventSaver;
	
	// Other stuff
	GpsExperimentsActivity activity;
	LocationManager locationManager;
	Handler handler = new Handler();
	PowerManager.WakeLock wakeLock;
	WifiManager.WifiLock wifiLock;
	
	public ExperimentRunner(GpsExperimentsActivity activity) {
		this.activity = activity;
		this.locationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
		
		PowerManager powerManager = (PowerManager)activity.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsExperiments");
        wakeLock.setReferenceCounted(false);
        
        WifiManager wifiManager = (WifiManager)activity.getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "GpsExperiments");
        wifiLock.setReferenceCounted(false);
	}
	
	public void start() {
		try {
			wakeLock.acquire();
			wifiLock.acquire();
			eventSaver = new EventSaver(((TelephonyManager)activity.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
			eventSaver.experimentRunStartedOrStopped(true, false, sleepTimes.size(), Utils.airplaneMode(activity));
			currentExperiment = -1;
			scheduleNextExperiment();
		} catch (IOException e) {
			Log.e(TAG, "Cannot create EventSaver.", e);
		}
	}
	
	private void stop() {		
		eventSaver.experimentRunStartedOrStopped(false, false, sleepTimes.size(), Utils.airplaneMode(activity));
		eventSaver.close();		
		wakeLock.release();
		wifiLock.release();
		activity.updateDone(sleepTimes.size());
	}
	
	public void cancel() {
		eventSaver.experimentRunStartedOrStopped(false, false, sleepTimes.size(), Utils.airplaneMode(activity));
		eventSaver.close();
		handler.removeCallbacks(stopGpsTask);
		handler.removeCallbacks(startGpsTask);
		locationManager.removeUpdates(ExperimentRunner.this);
		locationManager.removeGpsStatusListener(ExperimentRunner.this);
		locationManager.removeNmeaListener(ExperimentRunner.this);
		wakeLock.release();
	}
	
	public long expectedTotalTime() {
		long sum = 0;
		for (long sleep : sleepTimes)
			sum += sleep;
		sum += eventCollectionPeriod * sleepTimes.size();
		sum += 1000 * sleepTimes.size(); // Extra seconds per experiment just to be sure.
		return sum;
	}
	
	
	void scheduleNextExperiment() {
		++currentExperiment;
		
		// if out of expriments, stop and do nothing.
		if (currentExperiment >= sleepTimes.size()) {
			stop();			
		} else {			
			activity.updateExperiment(currentExperiment, sleepTimes.size(), deleteAGPS, sleepTimes.get(currentExperiment), eventCollectionPeriod);
			eventSaver.experimentStartedOrStopped(true, currentExperiment, sleepTimes.get(currentExperiment), deleteAGPS, eventCollectionPeriod);
			handler.postDelayed(startGpsTask, sleepTimes.get(currentExperiment));
		}
	}
	
	Runnable stopGpsTask = new Runnable() {
		public void run() {
			locationManager.removeUpdates(ExperimentRunner.this);
			locationManager.removeGpsStatusListener(ExperimentRunner.this);
			locationManager.removeNmeaListener(ExperimentRunner.this);
			eventSaver.removedGps();
			
			// This experiment is over
			eventSaver.experimentStartedOrStopped(false, currentExperiment, sleepTimes.get(currentExperiment), deleteAGPS, eventCollectionPeriod);
			
			// Next step: start next experiment with new delay params
			scheduleNextExperiment();
		}
	};
	
	Runnable startGpsTask = new Runnable() {
		public void run() {
			if (deleteAGPS) {
				locationManager.sendExtraCommand("gps", "delete_aiding_data", null);
				eventSaver.deletedAgpsData();
			}

			locationManager.requestLocationUpdates(GPS_PROVIDER, GPS_REQUEST_DELAY, GPS_REQUEST_MIN_DISTANCE, ExperimentRunner.this);
			locationManager.addGpsStatusListener(ExperimentRunner.this);
			locationManager.addNmeaListener(ExperimentRunner.this);
			eventSaver.requestedGps(GPS_PROVIDER, GPS_REQUEST_DELAY, GPS_REQUEST_MIN_DISTANCE);
			
			// Next step: schedule GPS stop with current experiment delay params
			handler.postDelayed(stopGpsTask, eventCollectionPeriod);
		}		
	};


	public void onLocationChanged(Location location) {
		eventSaver.onLocationChanged(location);
		activity.updateTimeToAccuracyNow(location.getAccuracy());
		//beep();
	}
	
	public void onGpsStatusChanged(int event) {
		GpsStatus status = locationManager.getGpsStatus(null);
		eventSaver.onGpsStatusChanged(event, status);
		if (event == GpsStatus.GPS_EVENT_FIRST_FIX)
			activity.updateTimeToFirstFix(status.getTimeToFirstFix());
	}
	
	public void onNmeaReceived(long timestamp, String nmea) {
		eventSaver.onNmeaReceived(timestamp, nmea);		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Boring, ignoring
		Log.e(TAG, "Unexpected onStatusChanged");
	}

	public void onProviderDisabled(String provider) {
		// Boring, ignoring
		Log.e(TAG, "Unexpected onProviderDisabled");
	}


	public void onProviderEnabled(String provider) {
		// Boring, ignoring
		Log.e(TAG, "Unexpected onProviderEnabled");
	}

	public void beep() {
		Ringtone r = RingtoneManager.getRingtone(this.activity, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		r.play();
	}
	
}
