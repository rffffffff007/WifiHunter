package cn.buding.android.common.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import cn.buding.android.common.log.LogUtils;

public class ParcelUtils {
	private static final String PREFERENCE_NAME = "parcel_preference";
	/** char set to encode string . cannot be changed */
	private static final String CHARSET = "ASCII";

	public static void restore(Context context, String preKey, Parcelable object) {
		if(object == null)
			return;
		Parcel parcel = Parcel.obtain();
		object.writeToParcel(parcel, 0);
		byte[] bytes = parcel.marshall();
		try {
			String byteStr = new String(bytes, CHARSET);
			writePreference(context, preKey, byteStr);
		} catch (UnsupportedEncodingException e) {
			LogUtils.e(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Parcelable> T create(Context context,
			Class<T> klass, String preKey) {
		try {
			Parcelable.Creator<T> creator =
					(Creator<T>) klass.getField("CREATOR").get(null);
			String str = readPreference(context, preKey);
			if (str == null)
				return null;
			byte[] data = str.getBytes(CHARSET);
			Parcel p = Parcel.obtain();
			p.unmarshall(data, 0, data.length);
			p.setDataPosition(0);
			return creator.createFromParcel(p);
		} catch (Exception e) {
			LogUtils.e(e);
			return null;
		}
	}

	public static void restoreArray(Context context, String preKey, Collection<? extends Parcelable> object){
		if(object == null)
			return;
		Parcel parcel = Parcel.obtain();
		for(Parcelable p : object){
			p.writeToParcel(parcel, 0);
		}
		byte[] bytes = parcel.marshall();
		try {
			String byteStr = new String(bytes, CHARSET);
			writePreference(context, preKey, byteStr);
		} catch (UnsupportedEncodingException e) {
			LogUtils.e(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Parcelable> List<T> createArray(Context context,
			Class<T> klass, String preKey) {
		try {
			Parcelable.Creator<T> creator =
					(Creator<T>) klass.getField("CREATOR").get(null);
			String str = readPreference(context, preKey);
			if (str == null)
				return null;
			byte[] data = str.getBytes(CHARSET);
			Parcel p = Parcel.obtain();
			p.unmarshall(data, 0, data.length);
			p.setDataPosition(0);
			List<T> list = new ArrayList<T>();
			while (p.dataAvail() > 0) {
				try {
					T t = creator.createFromParcel(p);
					list.add(t);
				} catch (Exception e) {
					break;
				}
			}
			return list;
		} catch (Exception e) {
			LogUtils.e(e);
			return null;
		}
	}

	public static void writePreference(Context context, String key, String value) {
		SharedPreferences pre =
				context.getSharedPreferences(PREFERENCE_NAME, 0);
		pre.edit().putString(key, value).commit();
	}

	public static String readPreference(Context context, String key) {
		SharedPreferences pre =
				context.getSharedPreferences(PREFERENCE_NAME, 0);
		return pre.getString(key, null);
	}

}
