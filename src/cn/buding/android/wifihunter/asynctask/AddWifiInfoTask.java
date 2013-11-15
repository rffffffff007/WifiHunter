package cn.buding.android.wifihunter.asynctask;

import android.content.Context;
import cn.buding.android.common.asynctask.HandlerMessageTask;
import cn.buding.android.common.exception.ECode;
import cn.buding.android.common.exception.JSONParseException;
import cn.buding.android.wifihunter.AccessPoint;
import cn.buding.android.wifihunter.R;
import cn.buding.android.wifihunter.json.JSONParser;
import cn.buding.android.wifihunter.net.HttpsManager;
import cn.buding.android.wifihunter.net.ServerApi;

public class AddWifiInfoTask extends HandlerMessageTask {
	private AccessPoint mPoint;
	private String mShopName;

	public AddWifiInfoTask(Context context, AccessPoint point, String shopName) {
		super(context);
		mPoint = point;
		// if shop name is "", set it to null to avoid cover the old data.
		if(shopName.length() == 0)
			shopName = null;
		mShopName = shopName;
		setShowProgessDialog(true);
		setCodeMsg(ECode.SUCCESS, R.string.toast_add_success);
	}

	@Override
	protected Object doInBackground(Void... params) {
		if (mPoint == null || mPoint.getInfo() == null) {
			// the connection wifi info is null.
			return ECode.FAIL;
		}
		String bssid = mPoint.getInfo().getBSSID();
		String ssid = mPoint.getInfo().getSSID();
		String pwd = mPoint.getUserInputPwd();
		String authType = mPoint.getSecurityString();
		String result =
				HttpsManager.addWifiInfo(mContext, bssid, ssid, pwd, authType,
						mShopName);
		try {
			Boolean res =
					(Boolean) JSONParser.parseWithCodeMessage(
							ServerApi.AddWifiInfo, result);
			if (res)
				return ECode.SUCCESS;
			return ECode.FAIL;
		} catch (JSONParseException e) {
			return e;
		}

	}
}
