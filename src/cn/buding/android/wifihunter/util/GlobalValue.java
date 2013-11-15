package cn.buding.android.wifihunter.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.buding.android.wifihunter.AccessPoint;
import cn.buding.android.wifihunter.json.JWifiInfo;

public class GlobalValue {
	private static GlobalValue mInstance;

	public static GlobalValue getIns() {
		if (mInstance == null)
			mInstance = new GlobalValue();
		return mInstance;
	}

	private List<AccessPoint> mAccessPoints;

	private Map<String, JWifiInfo> mJWifiMap;
	
	public void clear(){
		mAccessPoints = null;
		mJWifiMap = null;
	}

	public List<AccessPoint> getAccessPoints() {
		if (mAccessPoints == null)
			mAccessPoints = new ArrayList<AccessPoint>();
		return mAccessPoints;
	}

	/** unsafe. ssid is not unique. */
	private AccessPoint getAccessPointBySSID(String ssid) {
		if (mAccessPoints == null || ssid == null)
			return null;
		for (AccessPoint point : mAccessPoints)
			if (ssid.equals(point.getSsid()))
				return point;
		return null;
	}
	
	public AccessPoint getAccessPointByBSSID(String bssid){
		if (mAccessPoints == null || bssid == null)
			return null;
		for (AccessPoint point : mAccessPoints)
			if(point.containBssid(bssid))
				return point;
		return null;
	}

	public void addJWifiInfo(List<JWifiInfo> wifis) {
		for (JWifiInfo wifi : wifis) {
			addJWifiInfo(wifi);
		}
	}

	/**
	 * add a wifiinfo to memory and set it to corresponding AccessPoint.
	 */
	public void addJWifiInfo(JWifiInfo wifi) {
		getJWifiMap().put(wifi.bssid, wifi);
		AccessPoint a = getAccessPointByBSSID(wifi.bssid);
		if (a != null)
			a.setStoredInfo(wifi);
	}

	public void putJWifiMapNull(String bssid) {
		getJWifiMap().put(bssid, null);
	}

	private Map<String, JWifiInfo> getJWifiMap() {
		if (mJWifiMap == null)
			mJWifiMap = new HashMap<String, JWifiInfo>();
		return mJWifiMap;
	}

	public JWifiInfo getJWifiInfo(String bssid) {
		return getJWifiMap().get(bssid);
	}

	public boolean containJWifi(String bssid) {
		return getJWifiMap().containsKey(bssid);
	}

}
