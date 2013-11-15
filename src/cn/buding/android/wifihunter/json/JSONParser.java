package cn.buding.android.wifihunter.json;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.buding.android.common.exception.ECode;
import cn.buding.android.common.exception.JSONParseException;
import cn.buding.android.common.json.JSONUtils;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.wifihunter.net.ServerApi;

/**
 * parse the json result from server.
 */
public class JSONParser {

	public final static String CODE = "code";
	public final static String MESSAGE = "msg";

	/**
	 * parse the json result from server for each api. the returned object is determined by each function.
	 * TODO we can make a method for each API. and use reflection to invoke.
	 */
	public static Object parseWithCodeMessage(ServerApi api, String result)
			throws JSONParseException {
		Object res = null;
		try {
			if (result == null)
				throw new JSONParseException(ECode.SERVER_RETURN_NULL,
						"api return null");
			JSONObject jsObj = new JSONObject(result);
			int code = jsObj.getInt(CODE);
			if (code != ECode.SERVER_RETURN_SUCCESS) {
				String errMsg = "";
				try {
					errMsg = jsObj.getString(MESSAGE);
				} catch (Exception e) {
				}
				int errCode = code;
				// if the error code is not server error, we assign SERVER_RETURN_ERROR to it.
				if (!ECode.isServerError(code))
					errCode = ECode.SERVER_RETURN_ERROR;
				throw new JSONParseException(errCode, errMsg);
			}
			String methodName = "parse" + api.name();
			Method method =
					JSONParser.class.getDeclaredMethod(methodName,
							new Class<?>[] { JSONObject.class });
			JSONObject data = jsObj.getJSONObject("data");
			res = method.invoke(null, data);
			if (res == null)
				throw new JSONParseException(ECode.SERVER_RETURN_NULL,
						"parse result is null, how can it be?");
		} catch (Exception e) {
			LogUtils.e("Error in parsing api:" + api, e);
			throw new JSONParseException(ECode.JSON_PARSER_ERROR, e);
		}
		return res;
	}

	private static boolean parseOnlyCode(JSONObject job) throws JSONException {
		return job.getInt(CODE) == ECode.SERVER_RETURN_SUCCESS;
	}

	protected static boolean parseAddWifiInfo(JSONObject job)
			throws JSONException {
		return true;
	}

	protected static List<JWifiInfo> parseGetWifiInfos(JSONObject job)
			throws JSONException, JSONParseException {
		JSONArray array = job.getJSONArray("wifis");
		List<JWifiInfo> res = new ArrayList<JWifiInfo>();
		for (int i = 0, len = array.length(); i < len; i++) {
			JSONObject j = array.getJSONObject(i);
			JWifiInfo wifi =
					(JWifiInfo) JSONUtils.parseByClass(JWifiInfo.class, j);
			res.add(wifi);
		}
		return res;
	}

}
