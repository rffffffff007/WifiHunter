package cn.buding.android.common.location.google;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.http.entity.BasicHttpEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.buding.android.common.location.WifiInfo;


/**
 * http entity for wifi and tele info.
 * look for more details in <a href='http://code.google.com/p/gears/wiki/GeolocationAPI'>GeolocationAPI</a>
 */
public class WifiTelEntity extends BasicHttpEntity {
	private List<WifiInfo> mWifis;
	/** Unique identifier of the cell. (CID for GSM, BID for CDMA) */
	protected int mainCellID;
	/** Location Area Code (LAC for GSM, NID for CDMA) */
	protected int mainLAC;
	private List<TelInfo> mCellTowers;
	/** Mobile Network Code (MNC for GSM, SID for CDMA) */
	protected String mnc;
	/** Mobile Country Code (MCC for GSM and CDMA) */
	protected String mcc;

	public WifiTelEntity(String mcc, String mnc, int mainCellID, int mainLAC,
			List<TelInfo> cellTowers, List<WifiInfo> wifis) {
		this.mainCellID = mainCellID;
		this.mainLAC = mainLAC;
		mCellTowers = cellTowers;
		this.mnc = mnc;
		this.mcc = mcc;
		mWifis = wifis;
		setContentType("application/binary");
		setContentBytes();
	}

	// if it return false, a exception may occur.
	// org.apache.http.client.NonRepeatableRequestException: Cannot retry request with a non-repeatable request entity
	@Override
	public boolean isRepeatable() {
		return true;
	}

	private void setContentBytes() {
		// example
		// String s =
		// "{" + "\"version\":\"1.1.0\","
		// + "\"host\":\"maps.google.com\"," + "\"cell_towers\":["
		// + "{" + "\"cell_id\":" + myCellID + ","
		// + "\"location_area_code\":" + this.myLAC + ","
		// + "\"mobile_country_code\":" + this.mcc + ","
		// + "\"mobile_network_code\":" + this.mnc + "}" + "]"
		// + "}";
		JSONObject job = new JSONObject();
		try {
			job.put("version", "1.1.0");
			job.put("host", "maps.google.com");
			JSONArray cellTowers = new JSONArray();
			JSONObject j = new JSONObject();
			j.put("cell_id", mainCellID);
			j.put("location_area_code", mainLAC);
			j.put("mobile_country_code", mcc);
			j.put("mobile_network_code", mnc);
			cellTowers.put(j);
			for (TelInfo t : mCellTowers) {
				JSONObject i = new JSONObject();
				job.put("cell_id", t.getCellId());
				job.put("location_area_code", t.getLAC());
				job.put("mobile_country_code", mcc);
				job.put("mobile_network_code", mnc);
				cellTowers.put(i);
			}
			job.put("cell_towers", cellTowers);

			JSONArray wifis = new JSONArray();
			for (WifiInfo w : mWifis) {
				JSONObject wifi = new JSONObject();
				wifi.put("mac_address", w.getBSSID());
				wifi.put("signal_strength", w.getRSSI());
				wifis.put(wifi);
			}
			job.put("wifi_towers", wifis);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		ByteArrayInputStream in =
				new ByteArrayInputStream(job.toString().getBytes());
		setContent(in);
	}

	/** data class for tele. */
	public static class TelInfo {
		/** Unique identifier of the cell. (CID for GSM, BID for CDMA) */
		private int mCellId;
		/** Location Area Code (LAC for GSM, NID for CDMA) */
		private int mLAC;

		public TelInfo(int cellId, int lac) {
			mCellId = cellId;
			lac = mLAC;
		}

		public int getCellId() {
			return mCellId;
		}

		public int getLAC() {
			return mLAC;
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("mcc:" + mcc + ",");
		sb.append("mnc:" + mnc + ",");
		sb.append("maincid:" + mainCellID + ",");
		sb.append("mainlac:" + mainLAC + ",");
		for (TelInfo t : mCellTowers) {
			sb.append("cid:" + t.mCellId + ",");
			sb.append("lac:" + t.mLAC + ",");
		}
		for (WifiInfo w : mWifis) {
			sb.append("BSSID:" + w.getBSSID() + ",");
			sb.append("Rssi:" + w.getRSSI() + ",");
		}
		return sb.toString();
	}
}
