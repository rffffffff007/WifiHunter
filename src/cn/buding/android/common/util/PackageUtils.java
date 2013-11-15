package cn.buding.android.common.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;

/**
 * provide utils for package information operation..
 */
public class PackageUtils {
	private static String versionName;
	private static int versionCode = -1;

	/** whether the pkg is installed */
	public static boolean isPackageInstalled(final Context context,
			final String pkgName) {
		PackageManager pm = context.getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(pkgName, 0);
			return pi != null;
		} catch (Exception e) {
			return false;
		}
	}

	/** whether the app is installed in rom */
	public static boolean isInstalledOnROM(final Context context, String pkgName) {
		ApplicationInfo appInfo;
		try {
			appInfo =
					context.getPackageManager().getApplicationInfo(pkgName, 0);
			if (appInfo != null && appInfo.sourceDir != null
					&& appInfo.sourceDir.startsWith("/system/app")) {
				return true;
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static String getVersionName(Context ctx) {
		try {
			if (versionName == null) {
				ComponentName comp = new ComponentName(ctx, "");
				PackageInfo pinfo =
						ctx.getPackageManager().getPackageInfo(
								comp.getPackageName(), 0);
				versionName = pinfo.versionName;
			}
		} catch (NameNotFoundException e) {
		}
		return versionName;
	}

	public static int getVersionCode(Context ctx) {
		try {
			if (versionCode < 0) {
				ComponentName comp = new ComponentName(ctx, "");
				PackageInfo pinfo =
						ctx.getPackageManager().getPackageInfo(
								comp.getPackageName(), 0);
				versionCode = pinfo.versionCode;
			}
		} catch (NameNotFoundException e) {
		}
		return versionCode;
	}

	/** get meta data in manifest.xml */
	public static String getMetaData(Context ctx, String name) {
		String data = null;
		try {
			ApplicationInfo ai =
					ctx.getPackageManager().getApplicationInfo(
							ctx.getPackageName(), PackageManager.GET_META_DATA);
			data = ai.metaData.getString(name);
		} catch (Exception e) {
		}
		return data;

	}

	public static String getUmengChannel(Context ctx) {
		return getMetaData(ctx, "UMENG_CHANNEL");
	}

	public static String getIMEI(Context context) {
		TelephonyManager tm =
				(TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getSubscriberId();
	}

	public static String getIMSI(Context context) {
		TelephonyManager tm =
				(TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}

	public static String getPhoneNo(Context context) {
		TelephonyManager tm =
				(TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getLine1Number();
	}

}
