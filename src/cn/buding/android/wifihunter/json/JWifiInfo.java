package cn.buding.android.wifihunter.json;

import cn.buding.android.common.json.Optional;

public class JWifiInfo {
	public String ssid;
	public String bssid;
	@Optional
	public String pwd;
	@Optional
	public String auth_type;
	@Optional
	public String shop_name;
}
