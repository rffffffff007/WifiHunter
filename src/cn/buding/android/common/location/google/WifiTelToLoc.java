package cn.buding.android.common.location.google;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.Context;
import cn.buding.android.common.location.Location;
import cn.buding.android.common.location.WifiInfo;
import cn.buding.android.common.location.google.WifiTelEntity.TelInfo;
import cn.buding.android.common.log.LogTag;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.net.BaseHttpsManager;

/**
 * use wifi and tel info to request location.
 */
public class WifiTelToLoc {
	private int cell_id;
	private int lac_id;
	private String mnc;
	private String mcc;
	private List<TelInfo> mTelInfos;
	private List<WifiInfo> mWifis;

	public WifiTelToLoc(String mcc, String mnc, int aCellID, int aLAC,
			List<TelInfo> telInfos, List<WifiInfo> wifis) {
		this.cell_id = aCellID;
		this.lac_id = aLAC;
		this.mnc = mnc;
		this.mcc = mcc;
		mTelInfos = telInfos;
		mWifis = wifis;
	}

	public Location locate() {
		try {
			String baseURL = "http://www.google.com/loc/json";
			HttpEntity entity =
					new WifiTelEntity(mcc, mnc, cell_id, lac_id, mTelInfos,
							mWifis);
			String response = BaseHttpsManager.postAPI(baseURL, entity);
			JSONObject jo = new JSONObject(response);
			JSONObject jofile = jo.getJSONObject("location");
			double latitude = jofile.getDouble("latitude");
			double longitude = jofile.getDouble("longitude");
			double accuracy = jofile.getDouble("accuracy");
			Location loc =
					new Location(longitude, latitude, Location.PROVIDER_GOOGLE);
			loc.setAccuracy((float) accuracy);
			return loc;
		} catch (Exception e) {
			LogUtils.e(e);
			return null;
		}
	}

	public static final String MCC_CHINA = "460";

	public static void main(String[] args) {
		List<TelInfo> list = new ArrayList<TelInfo>();
		List<WifiInfo> wList = new ArrayList<WifiInfo>();
		// wList.add(new WifiInfo("00:3a:98:ee:f6:c1", -53));
		WifiTelToLoc cto =
				new WifiTelToLoc("460", "00", 25395, 4569, list, wList);
		cto.locate();
	}
}
