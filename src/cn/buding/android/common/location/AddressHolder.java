package cn.buding.android.common.location;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Parcel;
import cn.buding.android.common.location.IAddressFactory.OnAddressGetListener;
import cn.buding.android.common.location.google.GoogleAddressFactory;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.util.ParcelUtils;

/**
 * a cache that hold the the address of your recent location.
 */
public class AddressHolder {
	private static AddressHolder instance;
	public static final String PRE_KEY_ADDRESS_HOLDER =
			"pre_key_address_holder";
	private static final int MAX_LOC_COUNT = 15;
	private static final int MIN_DISTANCE_BETWEEN_LOCATION = 100;
	private List<Location> mLocs;
	private Context mContext;
	private IAddressFactory mFactory;

	public static AddressHolder getInstance(Context context) {
		if (instance == null)
			instance = new AddressHolder(context);
		return instance;
	}

	public AddressHolder(Context context) {
		mContext = context;
		init();
	}

	private void init() {
		mLocs = new ArrayList<Location>();
		List<Location> storedLocs =
				ParcelUtils.createArray(mContext, Location.class,
						PRE_KEY_ADDRESS_HOLDER);
		if (storedLocs != null)
			mLocs.addAll(storedLocs);
		mFactory = GoogleAddressFactory.getSingleton();
	}

	public void destroy() {
		int end = Math.min(mLocs.size(), MAX_LOC_COUNT);
		List<Location> locs = mLocs.subList(0, end);
		ParcelUtils.restoreArray(mContext, PRE_KEY_ADDRESS_HOLDER, locs);
		instance = null;
	}

	/**
	 * try to set the address to the newLoc.
	 */
	public void setAddress(final Location newLoc, final Location oldLoc) {
		if (newLoc == null)
			return;
		// find the nearest location in cache with the new location.
		double minD = Double.MAX_VALUE;
		Location minL = null;
		for (Location l : mLocs) {
			double dis = l.distanceTo(newLoc);
			if (dis < minD) {
				minD = dis;
				minL = l;
			}
		}
		// if the nearest loc in cache is in 100m, we just set the cached address to the new location. or we will keep
		// the new loc in cache and try to reverse gecode
		if (minL != null && minD < MIN_DISTANCE_BETWEEN_LOCATION
				&& minL.getAddress() != null) {
			newLoc.setAddress(minL.getAddress());
			onAddressChanged(oldLoc, newLoc);
		} else {
			mLocs.add(0, newLoc);
			mFactory.getAddress(mContext, newLoc, new OnAddressGetListener() {
				@Override
				public void onAddressGet(IAddress address) {
					onAddressChanged(oldLoc, newLoc);
				}
			});
		}
	}

	/**
	 * send broadcast to activities on address changed. use the newLoc and oldLoc to determine whether the city and the
	 * address is changed.
	 */
	private void onAddressChanged(Location oldLoc, Location newLoc) {
		String oldCity = oldLoc != null ? oldLoc.getCityName() : null;
		String newCity = newLoc.getCityName();
		boolean cityChanged = oldCity == null || !oldCity.equals(newCity);
		IAddress oldAddress = oldLoc != null ? oldLoc.getAddress() : null;
		IAddress newAddress = newLoc.getAddress();
		boolean addressChanged =
				oldAddress == null || !oldAddress.equals(newAddress);
		if (mOnAddressChangedListener != null)
			mOnAddressChangedListener.onAddressChanged(addressChanged,
					cityChanged);
	}

	private OnAddressChangedListener mOnAddressChangedListener;

	public void setOnAddressChangedListener(OnAddressChangedListener l) {
		mOnAddressChangedListener = l;
	}

	public interface OnAddressChangedListener {
		public void onAddressChanged(boolean addressChanged, boolean cityChanged);
	}
}
