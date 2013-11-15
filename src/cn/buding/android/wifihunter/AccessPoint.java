package cn.buding.android.wifihunter;

import java.util.ArrayList;
import java.util.List;

import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import cn.buding.android.wifihunter.json.JWifiInfo;

public class AccessPoint implements Comparable<AccessPoint> {
	static final int SECURITY_NONE = 0;
	static final int SECURITY_WEP = 1;
	static final int SECURITY_PSK = 2;
	static final int SECURITY_EAP = 3;

	private final String ssid;
	private final int security;
	private int networkId;
	/** bssid of all wifis in range. aggregated by ssid*/
	private List<String> bssids;
	/** bssid of connected wifi. may be null */
	private String bssid;

	private WifiConfiguration mConfig;
	private int mRssi;
	private WifiInfo mInfo;
	private JWifiInfo mStoredInfo;
	private DetailedState mState;
	private String mUserInputPwd;

	private OnWifiInfoUpdateListener mOnWifiInfoUpdateListener;

	static int getSecurity(WifiConfiguration config) {
		if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
			return SECURITY_PSK;
		}
		if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP)
				|| config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
			return SECURITY_EAP;
		}
		return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
	}

	private static int getSecurity(ScanResult result) {
		if (result.capabilities.contains("WEP")) {
			return SECURITY_WEP;
		} else if (result.capabilities.contains("PSK")) {
			return SECURITY_PSK;
		} else if (result.capabilities.contains("EAP")) {
			return SECURITY_EAP;
		}
		return SECURITY_NONE;
	}

	public static String getSecurityString(int security) {
		switch (security) {
		case SECURITY_NONE:
			return "NONE";
		case SECURITY_WEP:
			return "WEP";
		case SECURITY_PSK:
			return "WPA";
		case SECURITY_EAP:
			return "WPA_EAP";
		}
		return null;
	}

	public AccessPoint(WifiConfiguration config) {
		ssid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
		security = getSecurity(config);
		networkId = config.networkId;
		mConfig = config;
		mRssi = Integer.MAX_VALUE;
	}

	public AccessPoint(ScanResult result) {
		ssid = result.SSID;
		security = getSecurity(result);
		networkId = -1;
		mRssi = result.level;
	}

	@Override
	public int compareTo(AccessPoint another) {
		if (!(another instanceof AccessPoint)) {
			return 1;
		}
		AccessPoint other = (AccessPoint) another;
		// Active one goes first.
		if (mInfo != other.mInfo) {
			return (mInfo != null) ? -1 : 1;
		}
		// Reachable one goes before unreachable one.
		if ((mRssi ^ other.mRssi) < 0) {
			return (mRssi != Integer.MAX_VALUE) ? -1 : 1;
		}
		// Configured one goes before unconfigured one.
		if ((networkId ^ other.networkId) < 0) {
			return (networkId != -1) ? -1 : 1;
		}
		// Sort by signal strength.
		int difference = WifiManager.compareSignalLevel(other.mRssi, mRssi);
		if (difference != 0) {
			return difference;
		}
		// Sort by ssid.
		return ssid.compareToIgnoreCase(other.ssid);
	}

	/** update by scanresult, merge */
	public boolean update(ScanResult result) {
		if (ssid.equals(result.SSID) && security == getSecurity(result)) {
			addBssid(result.BSSID);
			if (WifiManager.compareSignalLevel(result.level, mRssi) > 0) {
				mRssi = result.level;
			}
			if (mOnWifiInfoUpdateListener != null)
				mOnWifiInfoUpdateListener.onWifiInfoUpdate();
			return true;
		}
		return false;
	}

	public boolean update(WifiConfiguration config) {
		String configSSID =
				(config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
		if (configSSID.equals(ssid) && security == getSecurity(config)) {
			addBssid(config.BSSID);
			networkId = config.networkId;
			mConfig = config;
			mRssi = Integer.MAX_VALUE;
			return true;
		}
		return false;
	}

	/** update the connected wifi info, mainly update state and rssi. and clear the info of other accesspoints */
	public void update(WifiInfo info, DetailedState state) {
		if (info != null && networkId != -1 && networkId == info.getNetworkId()) {
			bssid = info.getBSSID();
			mRssi = info.getRssi();
			mInfo = info;
			mState = state;
			if (mOnWifiInfoUpdateListener != null)
				mOnWifiInfoUpdateListener.onWifiInfoUpdate();
		} else if (mInfo != null) {
			mInfo = null;
			mState = null;
		}
	}

	public int getLevel() {
		if (mRssi == Integer.MAX_VALUE) {
			return -1;
		}
		return WifiManager.calculateSignalLevel(mRssi, 4);
	}

	public WifiConfiguration getConfig() {
		return mConfig;
	}

	public WifiInfo getInfo() {
		return mInfo;
	}

	public DetailedState getState() {
		return mState;
	}

	public int getRssi() {
		return mRssi;
	}

	public int getSecurity() {
		return security;
	}

	public String getSecurityString() {
		return getSecurityString(security);
	}

	public String getSsid() {
		return ssid;
	}

	public int getNetWorkId() {
		return networkId;
	}

	public void setUserInputPwd(String pwd) {
		mUserInputPwd = pwd;
	}

	public String getUserInputPwd() {
		return mUserInputPwd;
	}
	
	public String getBssid(){
		return bssid;
	}

	public List<String> getBssids() {
		return bssids;
	}

	private void addBssid(String bssid) {
		if (bssids == null)
			bssids = new ArrayList<String>();
		if (!bssids.contains(bssid))
			bssids.add(bssid);
	}

	public void setStoredInfo(JWifiInfo wifi) {
		mStoredInfo = wifi;
	}
	
	public JWifiInfo getStoredInfo(){
		return mStoredInfo;
	}
	
	public String getStoredPwd(){
		if(mStoredInfo == null)
			return null;
		return mStoredInfo.pwd;
	}
	
	public boolean hasStoredPwd(){
		return mStoredInfo != null && mStoredInfo.pwd != null && mStoredInfo.pwd.length() > 0;
	}
	
	public void removeWifiConfiguration(){
		mConfig = null;
		networkId = -1;
	}
	
	public void setOnWifiInfoUpdateListener(OnWifiInfoUpdateListener o) {
		mOnWifiInfoUpdateListener = o;
	}

	public static interface OnWifiInfoUpdateListener {
		public void onWifiInfoUpdate();
	}

	public static String removeDoubleQuotes(String string) {
		int length = string.length();
		if ((length > 1) && (string.charAt(0) == '"')
				&& (string.charAt(length - 1) == '"')) {
			return string.substring(1, length - 1);
		}
		return string;
	}

	public static String convertToQuotedString(String string) {
		return "\"" + string + "\"";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AccessPoint) {
			AccessPoint a = (AccessPoint) o;
			return getSsid().equals(a.getSsid())
					&& getSecurity() == a.getSecurity() && equalBssid(a);
		}
		return false;
	}

	public boolean containBssid(String bssid) {
		if (bssids == null)
			return false;
		return bssids.contains(bssid);
	}

	private boolean equalBssid(AccessPoint a) {
		if (a.getBssids() == null)
			return false;
		for (String bssid : a.getBssids()) {
			if (containBssid(bssid))
				return true;
		}
		return false;
	}
}
