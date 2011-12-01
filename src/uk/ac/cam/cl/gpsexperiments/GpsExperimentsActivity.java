package uk.ac.cam.cl.gpsexperiments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GpsExperimentsActivity extends Activity {

	public static final String TAG = "GpsExperiments";
	
	ExperimentRunner runner = null;
	
	// Current experiment run timing state. Used for display only.
	long currentExperimentRunStartTime;
	
	// Current experiment timing state. Used for display only.
	long currentExperimentStartTime;
	long currentExperimentFirstEventTime;
	float currentExperimentBestAccuracy;
	long currentExperimentBestAccuracyTime;
	
	Handler handler = new Handler();
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        attachUiEvents();
        resetEverything();
    }
	
	protected void onResume() {
		super.onResume();
		switch (Utils.airplaneMode(this)) {
			case -1: setText(R.id.airplane_mode, "Airplane Mode Unknown"); break;
			case 0: setText(R.id.airplane_mode, "Airplane Mode OFF"); break;
			default: setText(R.id.airplane_mode, "Airplane Mode ON"); break;
		}
		timeRemainingTask.run();
	}
	
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(timeRemainingTask);
	}


	protected void onDestroy() {
		super.onDestroy();
		
		resetEverything();
	}
	
	void resetEverything() {
		if (runner != null) {
			runner.cancel();
			runner = null;
		}
		
		
		
		((ProgressBar)findViewById(R.id.progressbar)).setProgress(0);
		((Button)findViewById(R.id.startstop)).setText("Start");
		currentExperimentRunStartTime = 0;
		setText(R.id.time_remaining, "");
		
		setText(R.id.experiment_number, "Experiments not running");
		setText(R.id.experiment_agps, "");
		setText(R.id.experiment_sleep, "");
		setText(R.id.experiment_collect, "");
		
		resetExperimentTimes();
		// Set the results strings empty
		setText(R.id.results_timetofirstfix, "");
		setText(R.id.results_timetofirstevent, "");
		setText(R.id.results_accuracy, "");
	}

	public void attachUiEvents() {
		((Button)findViewById(R.id.startstop)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				startStopClick();
			}
		});		
	}
	
	void startStopClick() {
		if (runner == null) {
			runner = new ExperimentRunner(this);
			runner.start();
			((Button)findViewById(R.id.startstop)).setText("Cancel");
			currentExperimentRunStartTime = System.currentTimeMillis();
		} else {
			resetEverything();
		}
	}
	
	void resetExperimentTimes() {
		currentExperimentStartTime = System.currentTimeMillis();
		currentExperimentBestAccuracyTime = 0;
		currentExperimentFirstEventTime = 0;
		currentExperimentBestAccuracy = Float.MAX_VALUE;
		setText(R.id.results_timetofirstfix, "Time To First Fix: pending");
		setText(R.id.results_timetofirstevent, "Time To First Event: pending");
		setText(R.id.results_accuracy, "Time To Accuracy: pending");
	}
	
	public void updateExperiment(int id, int numExperiments, boolean deleteAGPS, long sleepTime, long eventCollectionTime) {
		setText(R.id.experiment_number, String.format("Experiment %d of %d", id + 1, numExperiments));
		setText(R.id.experiment_agps, "Delete AGPS: " + deleteAGPS);
		setTimeText(R.id.experiment_sleep, "Sleep before GPS", sleepTime);
		setTimeText(R.id.experiment_collect, "Collect events for", eventCollectionTime);
		resetExperimentTimes();
	}
	
	public void updateDone(int numExperiments) {
		if (runner != null) {
			((ProgressBar)findViewById(R.id.progressbar)).setMax(100);
			((ProgressBar)findViewById(R.id.progressbar)).setProgress(100);
			setText(R.id.time_remaining, "Finished!");
			
			runner = null;
			resetEverything();
			setText(R.id.time_remaining, "Finished!");
		} else {
			Log.w(TAG, "updateDone received with no ExperimentRunner in progress.");
		}
	}
	
	public void updateTimeToFirstFix(long timeToFirstFix) {
		if (runner != null) {
			setTimeText(R.id.results_timetofirstfix, "Time To First Fix", timeToFirstFix);
		} else {
			Log.w(TAG, "updateTimeToFirstFix received with no ExperimentRunner in progress.");
		}
	}
		
	public void updateTimeToAccuracyNow(float accuracy) {
		if (runner != null) {
			if (currentExperimentFirstEventTime == 0) {
				currentExperimentFirstEventTime = System.currentTimeMillis();
				setTimeText(R.id.results_timetofirstevent, "Time To First Event", currentExperimentFirstEventTime - currentExperimentStartTime);
			}
			
			if (accuracy < currentExperimentBestAccuracy) {
				currentExperimentBestAccuracy = accuracy;
				currentExperimentBestAccuracyTime = System.currentTimeMillis();
				setTimeText(R.id.results_accuracy, String.format("Time To Accuracy %.0fm", accuracy), currentExperimentBestAccuracyTime - currentExperimentStartTime);
			}
		} else {
			Log.w(TAG, "updateTimeToAccuracyNow received with no ExperimentRunner in progress.");
		}
	}		
	
	void setText(int rid, String text) {
		((TextView)findViewById(rid)).setText(text);
	}
	
	void setTimeText(int rid, String prefix, long millis) {
		if (millis == 0)
			setText(rid, prefix + ": pending");
		else
			setText(rid, prefix + ": " + niceTime(millis));
	}
	
	String niceTime(long millis) {
		return String.format("%.3f sec", millis / 1000.0);
	}
	
	String hmsTime(long millis) {
		long sec = (millis / 1000) % 60;
		long min = (millis / 1000 / 60) % 60;
		long hour = (millis / 1000 / 3600);
		return String.format("%02d:%02d:%02d", hour, min, sec);
	}
	
	Runnable timeRemainingTask = new Runnable() {
		public void run() {
			if (runner != null) {
				long total = runner.expectedTotalTime();
				long elapsed = System.currentTimeMillis() - currentExperimentRunStartTime;
				long remaining = total - elapsed;
				
				((ProgressBar)findViewById(R.id.progressbar)).setMax((int)(total / 1000));
				((ProgressBar)findViewById(R.id.progressbar)).setProgress((int)(elapsed / 1000));
				
				setText(R.id.time_remaining, "Time Left: " + hmsTime(remaining));			
			}
			
			handler.postDelayed(this, 1000);
		}
	};
}