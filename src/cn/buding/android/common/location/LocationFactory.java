package cn.buding.android.common.location;

import android.content.Context;
import android.content.SharedPreferences;
import cn.buding.android.common.location.AddressHolder.OnAddressChangedListener;
import cn.buding.android.common.log.LogTag;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.util.ParcelUtils;

public class LocationFactory {
	private static LocationFactory mInstance;
	public static final String PRE_KEY_LAST_LOCATION = "pre_key_last_location";

	public static LocationFactory getSingleton(Context context) {
		if (mInstance == null)
			mInstance = new LocationFactory(context);
		return mInstance;
	}

	private LocationService mService;
	private Context mContext;
	private Location mLocation = null;
	private OnLocationChangedListener mOnLocationChangedListener;

	private LocationFactory(Context context) {
		mContext = context;
		mService = LocationService.getSingleInstance(context);
		initLocation();
	}

	private void initLocation() {
		mLocation = getLastLocation();
		if (mLocation == null)
			mLocation = mService.getLastLocatedLocation();
	}

	public void setmLocation(final Location newLocation) {
		if (newLocation == null || !newLocation.isValid())
			return;
		// new location could replace the old location only if it is more
		// accurate, newer .
		if (Location.isBetterLocation(newLocation, mLocation)) {
			AddressHolder.getInstance(mContext).setAddress(newLocation,
					mLocation);
			mLocation = newLocation;
			if (mOnLocationChangedListener != null)
				mOnLocationChangedListener.onLocationChanged(newLocation);
		}

		LogUtils.i(LogTag.LOCATION, "New loc: " + newLocation.toString()
				+ ". Location changed: " + (mLocation == newLocation));
	}

	public synchronized Location getmLocation() {
		if (mLocation == null)
			return Location.EMPTY_LOCATION;
		return mLocation;
	}

	public void onDestroy() {
		mService.destroy();
		restoreLastLocation();
		AddressHolder.getInstance(mContext).destroy();
		mInstance = null;
	}

	public void restoreLastLocation() {
		ParcelUtils.restore(mContext, PRE_KEY_LAST_LOCATION, mLocation);
	}

	public Location getLastLocation() {
		return ParcelUtils.create(mContext, Location.class,
				PRE_KEY_LAST_LOCATION);
	}

	public void setOnLocationChangedListener(OnLocationChangedListener l) {
		mOnLocationChangedListener = l;
	}
	
	public void setOnAddressChangedListener(OnAddressChangedListener l){
		AddressHolder.getInstance(mContext).setOnAddressChangedListener(l);
	}

	public interface OnLocationChangedListener {
		public void onLocationChanged(Location newLoc);
	}
}
