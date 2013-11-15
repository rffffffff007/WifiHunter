package cn.buding.android.common.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.buding.android.common.exception.ECode;
import cn.buding.android.common.exception.JSONParseException;
import cn.buding.android.common.log.LogTag;
import cn.buding.android.common.log.LogUtils;

/**
 * utils for JSONParser.
 */
public class JSONUtils {
	/**
	 * use the input class to new a instance. and use the input json data to fill the instance.
	 * 
	 * @param c the input class.
	 * @param jsObj the input data
	 */
	public static Object parseByClass(Class<?> c, JSONObject jsObj)
			throws JSONParseException {
		if (jsObj == null)
			return null;
		Object res = null;
		String fName;
		String value;
		boolean optional;
		try {
			res = c.newInstance();
			Field[] fields = c.getFields();
			for (Field f : fields) {
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				fName = f.getName();
				value = null;
				optional = f.getAnnotation(Optional.class) != null;
				try {
					value = jsObj.getString(fName);
				} catch (JSONException e) {
					if (!optional)
						LogUtils.e(LogTag.JSONPARSER, c.getName() + "." + fName
								+ " doesn't exist in json object");
					continue;
				}

				Class<?> type = f.getType();
				String typeName = type.getSimpleName();
				try {
					if (typeName.equals("int") || typeName.equals("Integer")) {
						f.setInt(res, Integer.valueOf(value));
					} else if (typeName.equals("boolean")
							|| typeName.equals("Boolean")) {
						f.setBoolean(res, Boolean.valueOf(value));
					} else if (typeName.equals("double")
							|| typeName.equals("Double")) {
						f.setDouble(res, Double.valueOf(value));
					} else if (typeName.equals("String")) {
						f.set(res, value);
					} else if (typeName.equals("Date")) {
						long time = Timestamp.valueOf(value).getTime();
						f.set(res, new Date(time));
					} else if (typeName.equals("String[]")) {
						String[] strs = null;
						JSONArray ja = new JSONArray(value);
						int count = ja.length();
						strs = new String[count];
						for (int i = 0; i < count; i++)
							strs[i] = ja.getString(i);
						f.set(res, strs);
					} else if (typeName.equals("int[]")) {
						int[] ints = null;
						JSONArray ja = new JSONArray(value);
						int count = ja.length();
						ints = new int[count];
						for (int i = 0; i < count; i++)
							ints[i] = ja.getInt(i);
						f.set(res, ints);
					} else if (type.isArray()) {
						String gName = f.getGenericType().toString();
						int s = gName.indexOf("[L") + 2;
						int e = gName.indexOf(";");
						gName = gName.substring(s, e);
						Class<?> subC = Class.forName(gName);
						JSONArray ja = new JSONArray(value);
						int count = ja.length();
						Object objs = Array.newInstance(subC, count);
						for (int i = 0; i < count; i++) {
							JSONObject jsob = ja.getJSONObject(i);
							Array.set(objs, i, parseByClass(subC, jsob));
						}
						f.set(res, objs);
					} else {
						Class<?> cla = type;
						JSONObject jsob = new JSONObject(value);
						Object obj = parseByClass(cla, jsob);
						f.set(res, obj);
					}
				} catch (Exception e) {
					LogUtils.e(LogTag.JSONPARSER, "Error in setting "
							+ typeName + ". Value: " + value + ", Field: "
							+ f.getName() + ", Error: " + e.getMessage());
				}
			}
		} catch (Exception e1) {
			throw new JSONParseException(ECode.CANNOT_INSTANTIATE, "Class: "
					+ c.getName() + "can not be instantiate.");
		}
		return res;
	}
}
