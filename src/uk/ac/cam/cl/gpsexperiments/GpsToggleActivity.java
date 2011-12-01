package uk.ac.cam.cl.gpsexperiments;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GpsToggleActivity extends Activity implements LocationListener {
	
	public static final String TAG = "GpsExperiments";
	
	Button startstop;
	TextView textlog;
	String logContent = "";
	
	boolean listening;
	LocationManager locationManager;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gpstoggle);
        
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        	
        attachUiEvents();        
    }
	
	void attachUiEvents() {
        startstop = (Button)findViewById(R.id.startstop);
        textlog = (TextView)findViewById(R.id.textlog);
        
        final GpsToggleActivity me = this;
        startstop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (listening) {
					locationManager.removeUpdates(me);					
					log("Stopped.");
					startstop.setText("Start");
				} else {
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, me);
					log("Started.");
					startstop.setText("Stop");					
				}
				listening = !listening;
			}
		});
	}
	
	void log(String message) {
		logContent = "* " + message + "\n" + logContent;
		textlog.setText(logContent);
	}

	public void onLocationChanged(Location location) {
		log(String.format("lat %.4f, lng %.4f, acc %.4f",
				location.getLatitude(),
				location.getLongitude(),
				location.getAccuracy()));
	}

	public void onProviderDisabled(String provider) {
		log(provider + " disabled");
	}

	public void onProviderEnabled(String provider) {
		log(provider + " enabled");
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		log(provider + " status now " + status);
	}
	
}
