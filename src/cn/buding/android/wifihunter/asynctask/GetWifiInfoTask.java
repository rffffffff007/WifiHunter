package cn.buding.android.wifihunter.asynctask;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import cn.buding.android.common.asynctask.HandlerMessageTask;
import cn.buding.android.common.exception.ECode;
import cn.buding.android.common.exception.JSONParseException;
import cn.buding.android.wifihunter.json.JSONParser;
import cn.buding.android.wifihunter.json.JWifiInfo;
import cn.buding.android.wifihunter.net.HttpsManager;
import cn.buding.android.wifihunter.net.ServerApi;
import cn.buding.android.wifihunter.util.GlobalValue;

public class GetWifiInfoTask extends HandlerMessageTask {
	private List<String> mBssids;

	public GetWifiInfoTask(Context context, List<String> bssids) {
		super(context);
		mBssids = bssids;
		setShowCodeMsg(false);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object doInBackground(Void... params) {
		// recreate the request bssids. first try to check it in memory. do not need to load it if it already exists in
		// memory.
		List<String> reqBssids = new ArrayList<String>();
		List<JWifiInfo> wifiInfos = new ArrayList<JWifiInfo>();
		for (String bssid : mBssids) {
			if (GlobalValue.getIns().containJWifi(bssid)) {
				JWifiInfo info = GlobalValue.getIns().getJWifiInfo(bssid);
				if (info != null)
					wifiInfos.add(info);
			} else {
				reqBssids.add(bssid);
			}
		}
		if (reqBssids.size() > 0) {
			String result = null;
			result =
					HttpsManager.getWifiInfos(mContext, reqBssids
							.toArray(new String[0]));
			try {
				List<JWifiInfo> list =
						(List<JWifiInfo>) JSONParser.parseWithCodeMessage(
								ServerApi.GetWifiInfos, result);
				wifiInfos.addAll(list);
				// if the response result do not contain the request bssid. we just set it to null in memory, in order
				// to avoid loading it again.
				for (String bssid : reqBssids) {
					if (!containBssid(list, bssid))
						GlobalValue.getIns().putJWifiMapNull(bssid);
				}
			} catch (JSONParseException e) {
				return e;
			}
		}

		GlobalValue.getIns().addJWifiInfo(wifiInfos);
		return ECode.SUCCESS;
	}

	private boolean containBssid(List<JWifiInfo> list, String bssid) {
		for (JWifiInfo j : list) {
			if (j.bssid != null && j.bssid.equals(bssid))
				return true;
		}
		return false;
	}

}
