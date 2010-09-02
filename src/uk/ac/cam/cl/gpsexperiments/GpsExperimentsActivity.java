package uk.ac.cam.cl.gpsexperiments;

import android.app.Activity;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class GpsExperimentsActivity extends Activity implements LocationListener, GpsStatus.Listener {

	private boolean running = false;
	private EditText fixPeriod = null;
	private Button startStop = null;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        attachUiEvents();
    }
    
    private void attachUiEvents() {
    	fixPeriod = (EditText)findViewById(R.id.fix_period);
    	startStop = (Button)findViewById(R.id.startstop);
    	startStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (running)
					stop();
				else
					start();
			}
		});
    }
    
    protected void start() {
    	startStop.setText("Stop");
    	fixPeriod.setEnabled(false);
    	running = true;
    }
    
    protected void stop() {
    	startStop.setText("Start");
    	fixPeriod.setEnabled(true);
    	running = false;
    }

    
    // LocationListener
    
	public void onLocationChanged(Location location) {
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	// GpsStatus.Listener
	public void onGpsStatusChanged(int event) {
	}
    
}