package uk.ac.cam.cl.gpsexperiments;

import java.io.IOException;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ExperimentRunner implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener {

	public static String TAG = "GpsExperiments";
	
	// GPS Parameters
	static String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
	static long GPS_REQUEST_DELAY = 100;
	static float GPS_REQUEST_MIN_DISTANCE = 0.0f;
	
	// Experiment parameters	
	long[] sleepTimes = {0, 0, 1000, 5000, 10000, 20000, 30000, 60000, 120000, 300000};
	boolean deleteAGPS = true;
	long eventCollectionPeriod = 60*1000;
	
	// Current state
	int currentExperiment;
	EventSaver eventSaver;
	
	// Other stuff
	GpsExperimentsActivity activity;
	LocationManager locationManager;
	Handler handler = new Handler();
	
	public ExperimentRunner(GpsExperimentsActivity activity) {
		this.activity = activity;
		this.locationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public void start() {
		try {
			eventSaver = new EventSaver(((TelephonyManager)activity.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
			eventSaver.experimentRunStartedOrStopped(true, sleepTimes.length);
			currentExperiment = -1;
			scheduleNextExperiment();
		} catch (IOException e) {
			Log.e(TAG, "Cannot create EventSaver.", e);
		}
	}
	
	void stop() {
		eventSaver.experimentRunStartedOrStopped(false, sleepTimes.length);
		eventSaver.close();
		activity.updateExperimentStatus("Finished!");
	}
	
	void scheduleNextExperiment() {
		++currentExperiment;
		
		// if out of expriments, stop and do nothing.
		if (currentExperiment >= sleepTimes.length) {
			stop();			
		} else {
			eventSaver.experimentStartedOrStopped(true, currentExperiment, sleepTimes[currentExperiment], deleteAGPS, eventCollectionPeriod);
			activity.updateExperimentStatus("Starting experiment " + currentExperiment);
			handler.postDelayed(startGpsTask, sleepTimes[currentExperiment]);			
		}
	}
	
	Runnable stopGpsTask = new Runnable() {
		public void run() {
			Log.i(TAG, "Stopping GPS for experiment " + currentExperiment);
			if (deleteAGPS) {
				locationManager.sendExtraCommand("gps", "delete_aiding_data", null);
				eventSaver.deletedAgpsData();
			}

			locationManager.removeUpdates(ExperimentRunner.this);
			locationManager.removeGpsStatusListener(ExperimentRunner.this);
			locationManager.removeNmeaListener(ExperimentRunner.this);
			eventSaver.removedGps();
			
			// This experiment is over
			eventSaver.experimentStartedOrStopped(false, currentExperiment, sleepTimes[currentExperiment], deleteAGPS, eventCollectionPeriod);
			
			// Next step: start next experiment with new delay params
			scheduleNextExperiment();
		}
	};
	
	Runnable startGpsTask = new Runnable() {
		public void run() {
			Log.i(TAG, "Starting GPS for experiment " + currentExperiment);
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
	}
	
	public void onGpsStatusChanged(int event) {
		eventSaver.onGpsStatusChanged(event, locationManager.getGpsStatus(null));
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

	
	
	
	
}
