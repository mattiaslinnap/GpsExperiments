package uk.ac.cam.cl.gpsexperiments;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

public class Utils {
	public static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS");
	
	// This returns time string in the current Locale, including DST!
	// android_time is always UTC.
	public static String timestampString(long android_time) {
		Date date = new Date(android_time);
		return timestampFormat.format(date);
	}
	
	public static String currentTimestampString() {
		return timestampString(System.currentTimeMillis());
	}
	
	public static int airplaneMode(Context context) {
		try {
			return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON);
		} catch (SettingNotFoundException e) {
			return -1;
        }
	}
}
