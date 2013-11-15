package cn.buding.android.common.location;

import android.net.wifi.ScanResult;

/**
 * a model class for wifi info.
 */
public class WifiInfo {
	/** basic service set identifier */
	private String mBSSID;
	/** service set identifier */
	private String mSSID;
	/** Received Signal Strength Indicator */
	private int mRssi;
	private int mFrequency;
	private String mCapabilities;

	public WifiInfo(ScanResult result) {
		this.mBSSID = result.BSSID;
		this.mSSID = result.SSID;
		this.mRssi = result.level;
		this.mFrequency = result.frequency;
		this.mCapabilities = result.capabilities;
	}

	public WifiInfo(android.net.wifi.WifiInfo info) {
		this.mBSSID = info.getBSSID();
		this.mSSID = info.getSSID();
		this.mRssi = info.getRssi();
		this.mFrequency = 0;
		mCapabilities = "";
	}

	/**
	 * parse the model a jsonobject
	 */
	public String getJSONString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{BSSID:\"");
		sb.append(mBSSID);
		sb.append("\",SSID:\"");
		sb.append(mSSID);
		sb.append("\",capabilities:'");
		sb.append(mCapabilities);
		sb.append("',level:'");
		sb.append(mRssi);
		sb.append("',frequency:'");
		sb.append(mFrequency);
		sb.append("'}");
		return sb.toString();
	}

	public String getBSSID() {
		return mBSSID;
	}

	public int getRSSI() {
		return mRssi;
	}
	
	public boolean isValid(){
		return mBSSID != null;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof WifiInfo) {
			return mBSSID.equals(((WifiInfo) o).getBSSID());
		}
		return false;
	}
}
