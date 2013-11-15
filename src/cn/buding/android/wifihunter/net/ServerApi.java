package cn.buding.android.wifihunter.net;

/**
 * contain all apis.
 */
public enum ServerApi {
	AddWifiInfo("wifihunter.api.AddWifi"), GetWifiInfos(
			"wifihunter.api.GetWifiInfos");

	/** the param or url of the api. */
	private String api;

	ServerApi() {

	}

	ServerApi(String api) {
		this.api = api;
	}

	public String toString() {
		return api;
	}
	
	/**
	 * use {@link #api} to find out the host address of the api.
	 * 
	 * @return the host address
	 */
	public String getHostAddress() {
		if (api.startsWith("wifihunter.api"))
			return IProtocol.HTTP_WIFI_API;
		return null;
	}

}
