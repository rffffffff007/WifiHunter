package cn.buding.android.common.location.google;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.buding.android.common.exception.JSONParseException;
import cn.buding.android.common.json.JSONUtils;

/**
 * json model for google returned address.
 * see details in <a href='http://code.google.com/apis/maps/documentation/geocoding/#ReverseGeocoding'>api document</a>
 */
public class JGoogleAddress implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * street name.
	 * 
	 * @example 苏州街
	 */
	protected static final String TYPE_ROUTE = "route";
	/**
	 * area name
	 * 
	 * @example 海淀区
	 */
	protected static final String TYPE_SUBLOCALITY = "sublocality";
	protected static final String TYPE_CITY = "locality";
	protected static final String TYPE_PROVINCE = "administrative_area_level_1";
	protected static final String TYPE_COUNTRY = "country";

	public String[] types;
	public String formatted_address;
	public JGoogleAddressComponent[] address_components;

	public static JGoogleAddress parse(String result) throws JSONException,
			JSONParseException {
		JSONObject job = new JSONObject(result);
		String status = job.getString("status");
		if (!status.equals("OK"))
			return null;
		JSONArray results = job.getJSONArray("results");
		if (results.length() == 0)
			return null;
		JSONObject result0 = results.getJSONObject(0);
		JGoogleAddress res =
				(JGoogleAddress) JSONUtils.parseByClass(JGoogleAddress.class,
						result0);
		return res;
	}
}
