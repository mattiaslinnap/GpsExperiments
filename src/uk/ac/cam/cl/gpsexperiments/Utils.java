package uk.ac.cam.cl.gpsexperiments;

import java.text.SimpleDateFormat;
import java.util.Date;

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
}
