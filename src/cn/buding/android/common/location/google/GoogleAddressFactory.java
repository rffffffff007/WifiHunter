package cn.buding.android.common.location.google;

import android.content.Context;
import cn.buding.android.common.location.IAddress;
import cn.buding.android.common.location.IAddressFactory;
import cn.buding.android.common.location.Location;
import cn.buding.android.common.location.IAddressFactory.OnAddressGetListener;
import cn.buding.android.common.log.LogTag;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.net.BaseHttpsManager;
import cn.buding.android.wifihunter.net.HttpsManager;

public class GoogleAddressFactory implements IAddressFactory {
	private static GoogleAddressFactory mInstance;

	public static GoogleAddressFactory getSingleton() {
		if (mInstance == null)
			mInstance = new GoogleAddressFactory();
		return mInstance;
	}

	@Override
	public void getAddress(Context context, Location loc) {
		getAddress(context, loc, null);
	}

	@Override
	public void getAddress(Context context, Location loc,
			OnAddressGetListener callback) {
		new ReverseGecodeThread(context, loc, callback).start();
	}

	public static class ReverseGecodeThread extends Thread {
		public static final String GOOGLE_REVERSE_GECODE_API =
				"http://maps.googleapis.com/maps/api/geocode/json?sensor=true&region=cn&language=zh-CN&latlng=";
		private Context mContext;
		private Location mLoc;
		private OnAddressGetListener mListener;

		public ReverseGecodeThread(Context context, Location loc,
				OnAddressGetListener listener) {
			this.mContext = context;
			this.mLoc = loc;
			mListener = listener;
		}

		@Override
		public void run() {
			IAddress mAddress = null;
			try {
				String api =
						GOOGLE_REVERSE_GECODE_API + mLoc.getLatitude() + ","
								+ mLoc.getLongitude();
				String result = BaseHttpsManager.sendPost(api);
				JGoogleAddress jAddress = JGoogleAddress.parse(result);
				if (jAddress != null) {
					mAddress = new GoogleAddress(jAddress);
				}
				if (mAddress != null) {
					LogUtils.i(LogTag.LOCATION, "Address Geted: "
							+ mAddress.toString() + " " + mLoc.toString());
					mLoc.setAddress(mAddress);
				}
			} catch (Exception e) {
				LogUtils.e(e);
			}
			if (mListener != null)
				mListener.onAddressGet(mAddress);
		}
	}

}
