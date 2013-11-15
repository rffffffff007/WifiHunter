package cn.buding.android.wifihunter.net;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONArray;

import android.content.Context;
import cn.buding.android.common.net.BaseHttpsManager;

/**
 * contain all methods to use the apis in {@link ServerApi}.
 */
public class HttpsManager extends BaseHttpsManager {

	public static String addWifiInfo(Context context, String bssid,
			String ssid, String pwd, String authType, String shopName) {
		RequestParam request = new RequestParam(context, ServerApi.AddWifiInfo);
		request.putParam("bssid", bssid);
		request.putParamNotNull("ssid", ssid);
		request.putParamNotNull("pwd", pwd);
		request.putParamNotNull("auth_type", authType);
		request.putParamNotNull("shop_name", shopName);
		return postAPI(request);
	}

	public static String getWifiInfos(Context context, String[] bssids) {
		RequestParam request =
				new RequestParam(context, ServerApi.GetWifiInfos);
		request.putParam("bssids", new JSONArray(Arrays.asList(bssids)));
		return postAPI(request);
	}

	static class RequestParam extends BaseHttpsManager.RequestParam {
		private static final long serialVersionUID = 1L;

		public RequestParam(Context context, ServerApi api) {
			super(context, api.getHostAddress(), api.toString());
		}

	}

}
