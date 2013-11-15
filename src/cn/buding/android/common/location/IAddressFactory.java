package cn.buding.android.common.location;

import android.content.Context;

/** address factory to get address for input locaton */
public interface IAddressFactory {
	public void getAddress(Context context, Location loc,
			OnAddressGetListener callback);

	public void getAddress(Context context, Location loc);

	public static interface OnAddressGetListener {
		public void onAddressGet(IAddress address);
	}
}
