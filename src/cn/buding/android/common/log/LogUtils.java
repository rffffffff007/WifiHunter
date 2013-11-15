package cn.buding.android.common.log;

import android.util.Log;

/**
 * a util class for log
 */
public class LogUtils {
	private static final boolean canLog = true;
	private static String LOG_TAG = "buding";

	public static void setLogTag(String tag) {
		LOG_TAG = tag;
	}

	public static void d(String msg) {
		d(null, msg);
	}

	public static void d(Object tag, String msg) {
		if (canLog)
			Log.d(LOG_TAG, getString(tag, msg));
	}

	public static void e(Object tag, String msg) {
		if (canLog)
			Log.e(LOG_TAG, getString(tag, msg));
	}

	public static void e(String msg, Exception e) {
		e(null, (msg != null ? msg + "\n" : "") + e.getMessage()
				+ Log.getStackTraceString(e));
	}

	public static void e(String msg) {
		e(null, msg != null ? msg + "\n" : "");
	}

	public static void e(Exception e) {
		e(null, e);
	}

	public static void i(String msg) {
		i(null, msg);
	}

	public static void i(Object tag, String msg) {
		if (canLog)
			Log.i(LOG_TAG, getString(tag, msg));
	}

	public static void v(String msg) {
		v(null, msg);
	}

	public static void v(Object tag, String msg) {
		if (canLog)
			Log.v(LOG_TAG, getString(tag, msg));
	}

	public static void w(String msg) {
		w(null, msg);
	}

	public static void w(Object tag, String msg) {
		if (canLog)
			Log.w(LOG_TAG, getString(tag, msg));
	}

	private static String getString(Object tag, String msg) {
		return (tag != null ? (tag.toString() + "\t") : "")
				+ (msg == null ? "null" : msg);
	}
}
