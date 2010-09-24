package uk.ac.cam.cl.gpsexperiments;

import android.app.Activity;
import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class GpsExperimentsActivity extends Activity {

	public static final String TAG = "GpsExperiments";
	
	ExperimentRunner runner;
	TextView textExperimentStatus;
	PowerManager.WakeLock wakeLock;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        attachUiEvents();
        
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "GpsExperiments");
        wakeLock.acquire();
        
        runner = new ExperimentRunner(this);
        runner.start();
    }
	
	protected void onDestroy() {
		super.onDestroy();
		
		wakeLock.release();		
	}

	public void attachUiEvents() {
		textExperimentStatus = (TextView)findViewById(R.id.experimentstatus);
	}
	
	public void updateExperimentStatus(String status) {
		textExperimentStatus.setText(status);
	}
}