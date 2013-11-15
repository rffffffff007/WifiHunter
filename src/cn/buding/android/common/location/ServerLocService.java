package cn.buding.android.common.location;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationListener;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import cn.buding.android.common.location.google.WifiTelToLoc;
import cn.buding.android.common.location.google.WifiTelEntity.TelInfo;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.net.BaseHttpsManager;

/**
 * custom location service. listen to the wifi/tele change and try to located by tele/wifi info.
 */
public class ServerLocService {
	/** we set cdma type here since android 1.5 sdk do not contain this value. */
	public static int PHONE_TYPE_CDMA = 2;
	private TelephonyManager telManager;
	/** listen to the cell info changed */
	private PhoneStateListener cellListener;

	/** listen to wifi scan result */
	private BroadcastReceiver wifiResultReceiver;
	/** listen to the wifi state changed */
	private BroadcastReceiver wifiStateReceiver;
	private List<WifiInfo> mWifiinfos;
	/** max wifi count restored in mWifiinfos */
	private static int MAX_WIFI_COUNT = 5;

	private Context context;
	private LocationListener mListener;
	/** the min time duration between two request. */
	private long mMinDuration = 2 * 60 * 1000;

	private static ServerLocService mInstance;

	private ServerLocService(Context context) {
		this.context = context.getApplicationContext();
	}

	public static ServerLocService getInstance(Context context) {
		if (mInstance == null)
			mInstance = new ServerLocService(context);
		return mInstance;
	}

	public void registerLocationUpdates(long minDuration,
			LocationListener listener) {
		mListener = listener;
		mMinDuration = minDuration;
		init();
	}

	private void init() {
		try {
			destroyService();
			registerTelInfoL();
			registerWifiInfo();
		} catch (Exception e) {
			LogUtils.e(e);
		}
	}

	public static void destroy() {
		if (mInstance != null) {
			mInstance.destroyService();
			mInstance = null;
		}
	}

	/**
	 * destroy the {@link #cellListener} {@link #wifiResultReceiver} and {@link #wifiStateReceiver}
	 */
	private void destroyService() {
		if (cellListener != null) {
			telManager.listen(cellListener, PhoneStateListener.LISTEN_NONE);
			cellListener = null;
		}
		try {
			if (wifiResultReceiver != null) {
				context.unregisterReceiver(wifiResultReceiver);
				wifiResultReceiver = null;
			}
		} catch (Exception e) {
			LogUtils.e(e);
		}
		try {
			if (wifiStateReceiver != null) {
				context.unregisterReceiver(wifiStateReceiver);
				wifiStateReceiver = null;
			}
		} catch (Exception e) {
			LogUtils.e(e);
		}

	}

	/**
	 * register cellListener for cell location change
	 */
	private void registerTelInfoL() {
		telManager =
				(TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
		cellListener = new PhoneStateListener() {
			public void onCellLocationChanged(CellLocation location) {
				if (onTelInfoChanged())
					onWifiTelStateChanged();
			}
		};
		telManager
				.listen(cellListener, PhoneStateListener.LISTEN_CELL_LOCATION);
	}

	private String mLastBSSID;

	/**
	 * register a listener for wifi scan result, and restore wifiinfo in memory. register a listener for wifi state
	 * change, and check if the connected wifi is changed.
	 */
	private void registerWifiInfo() {
		final WifiManager wifimanager =
				(WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mWifiinfos = new ArrayList<WifiInfo>();
		WifiInfo connWifiInfo = new WifiInfo(wifimanager.getConnectionInfo());
		if (connWifiInfo.isValid())
			mWifiinfos.add(connWifiInfo);

		wifiResultReceiver = new BroadcastReceiver() {
			public void onReceive(Context c, Intent intent) {
				List<ScanResult> wifis = wifimanager.getScanResults();
				if (null == wifis)
					return;
				// order by level desc
				Collections.sort(wifis, new Comparator<ScanResult>() {
					@Override
					public int compare(ScanResult object1, ScanResult object2) {
						return object2.level - object1.level;
					}
				});
				mWifiinfos.clear();
				int len = Math.min(wifis.size(), MAX_WIFI_COUNT);
				for (int i = 0; i < len; i++) {
					WifiInfo info = new WifiInfo(wifis.get(i));
					if (info.isValid())
						mWifiinfos.add(info);
				}
			}
		};
		context.registerReceiver(wifiResultReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		wifiStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// if wifi changed.
				String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
				if (bssid != null && bssid.equals(mLastBSSID)) {
					mLastBSSID = bssid;
					onWifiTelStateChanged();
				}
			}
		};
		context.registerReceiver(wifiStateReceiver, new IntentFilter(
				WifiManager.NETWORK_STATE_CHANGED_ACTION));
	}

	private long lastExecuteTime;

	/**
	 * use wifi and tele info to invoke {@link GetLocationThread}. time duration between two request can not within
	 * {@link #mMinDuration}
	 */
	private void onWifiTelStateChanged() {
		long timeDiffer = System.currentTimeMillis() - lastExecuteTime;
		long MinTimeDuration = mMinDuration;
		if (timeDiffer < mMinDuration)
			return;
		lastExecuteTime = System.currentTimeMillis();
		new GetLocationThread(context).start();
	}

	/** connected tele info in gsm */
	private GsmInfo gsmInfo;
	/** connected tele info in cdma */
	private CDMAInfo cdmaInfo;

	/**
	 * collected tele info on tele changed.
	 * 
	 * @return whether the current tele location is different.
	 */
	private boolean onTelInfoChanged() {
		boolean telChanged = false;
		CellLocation location = telManager.getCellLocation();
		if (location == null)
			return false;
		try {
			if (telManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
				GsmCellLocation gsmL = (GsmCellLocation) location;
				telChanged = gsmInfo == null || gsmInfo.cid != gsmL.getCid();
				gsmInfo = new GsmInfo();
				gsmInfo.cid = gsmL.getCid();
				gsmInfo.lac = gsmL.getLac();
			} else if (telManager.getPhoneType() == PHONE_TYPE_CDMA) {
				Class<?> cdma = Class.forName("android.telephony.cdma");
				int bid = getMethodResult(cdma, "getBaseStationId", location);
				telChanged = cdmaInfo == null || cdmaInfo.bid != bid;
				cdmaInfo = new CDMAInfo();
				cdmaInfo.bid = bid;
				cdmaInfo.bid = getMethodResult(cdma, "getNetworkId", location);
				cdmaInfo.sid = getMethodResult(cdma, "getSystemId", location);
			}
		} catch (Exception e) {
			LogUtils.e(e);
			return false;
		}
		return telChanged;
	}

	private int getMethodResult(Class<?> cla, String methodName,
			CellLocation location) throws Exception {
		Method method =
				cla.getMethod(methodName, new Class<?>[] { Void.class });
		return Integer.valueOf(method.invoke(location, new Object[] {})
				.toString());
	}

	public class GsmInfo {
		public int lac;
		public int cid;
	}

	public class CDMAInfo {
		public int bid;
		public int nid;
		public int sid;
	}

	public class GetLocationThread extends Thread {
		private Context mContext;
		private String mccmnc;
		private int phoneType;
		private List<NeighboringCellInfo> neighbors;

		public GetLocationThread(Context mContext) {
			this.mContext = mContext;
			mccmnc = telManager.getNetworkOperator();
			phoneType = telManager.getPhoneType();
			neighbors = telManager.getNeighboringCellInfo();
		}

		@Override
		public void run() {
			Location res;
			res = locateFromGoogle();
			if (res == null) {
				res = locateFromServer();
			}
			if (res != null) {
				if (mListener != null)
					mListener.onLocationChanged(res);
			}
		}

		public static final String MCC_CHINA = "460";
		public static final String MNC_MOBILE = "00";

		private Location locateFromGoogle() {
			try {
				String mcc = MCC_CHINA;
				String mnc = MNC_MOBILE;
				int cid = 0;
				int lac = 0;
				if (mccmnc != null && mccmnc.length() == 5) {
					mcc = mccmnc.substring(0, 3);
					mnc = mccmnc.substring(3);
				}
				if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
					cid = gsmInfo.cid;
					lac = gsmInfo.lac;
				} else if (phoneType == ServerLocService.PHONE_TYPE_CDMA) {
					mnc = "" + cdmaInfo.sid;
					cid = cdmaInfo.bid;
					lac = cdmaInfo.nid;
				}
				List<TelInfo> telInfos = new ArrayList<TelInfo>();
				for (NeighboringCellInfo info : neighbors) {
					int i = info.getCid();
					int l = getLac(info);
					TelInfo t = new TelInfo(i, l);
					telInfos.add(t);
				}
				WifiTelToLoc loc =
						new WifiTelToLoc(mcc, mnc, cid, lac, telInfos,
								mWifiinfos);
				return loc.locate();
			} catch (Exception e) {
				LogUtils.e(e);
			}
			return null;
		}

		private static final String QUERY_LOCATION_API =
				"http://www.wandouquan.com/avatar";

		private Location locateFromServer() {
			String param = getPositionInfoJsonString();
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params
					.add(new BasicNameValuePair("api",
							"avatar.api.QueryPosition"));
			params.add(new BasicNameValuePair("param", param));
			try {
				String result =
						BaseHttpsManager.postAPI(QUERY_LOCATION_API, params);
				JSONObject jsObj = new JSONObject(result);
				double longi = jsObj.getDouble("longitude");
				double lati = jsObj.getDouble("latitude");
				Location loc =
						new Location(longi, lati, Location.PROVIDER_SERVER);
				double accuracy = 0;
				try {
					accuracy = jsObj.getDouble("accuracy");
					loc.setAccuracy((float) accuracy);
				} catch (Exception e) {
				}
				return loc;
			} catch (Exception e) {
				LogUtils.e(e);
			}
			return null;
		}

		public String getPositionInfoJsonString() {
			try {
				JSONObject res = new JSONObject();
				// res.put("imei", GlobalValue.getIMEI(mContext));
				// res.put("imsi", GlobalValue.getIMSI(mContext));
				JSONObject teleinfo = new JSONObject();
				if (mccmnc == null || mccmnc.length() == 0)
					mccmnc = "0";
				teleinfo.put("mccmnc", mccmnc);
				teleinfo.put("phonetype", phoneType);
				if (gsmInfo != null) {
					teleinfo.put("maincellid", gsmInfo.cid);
					teleinfo.put("mainlac", gsmInfo.lac);
				}
				if (cdmaInfo != null) {
					teleinfo.put("basestationid", cdmaInfo.bid);
					teleinfo.put("networkid", cdmaInfo.nid);
					teleinfo.put("systemid", cdmaInfo.sid);
				}
				JSONArray neighborArray = new JSONArray();
				for (NeighboringCellInfo info : neighbors) {
					JSONObject i = new JSONObject();
					i.put("cellid", info.getCid());
					i.put("lac", getLac(info));
				}
				teleinfo.put("neighbors", neighborArray);
				res.put("teleinfo", teleinfo);
				JSONArray wifiinfo = new JSONArray();
				for (WifiInfo w : mWifiinfos)
					wifiinfo.put(new JSONObject(w.getJSONString()));
				res.put("wifiinfo", wifiinfo);
				res.put("gpsinfo", new JSONObject());
				res.put("googleservice", new JSONObject(
						"{latitude:0, longitude:0}"));
				return res.toString();
			} catch (JSONException e) {
				return "";
			}
		}
	}

	private int getLac(NeighboringCellInfo nei) {
		if (Integer.valueOf(android.os.Build.VERSION.SDK) > 3) {
			try {
				Method m =
						NeighboringCellInfo.class.getMethod("getLac",
								new Class[] {});
				m.invoke(nei);
			} catch (Exception e) {
			}
		}
		return 0;
	}

}
