package cn.buding.android.common.location.google;

import android.os.Parcel;
import android.os.Parcelable;
import cn.buding.android.common.location.IAddress;

/**
 * a model class for address returned by google reverse gecode.
 */
public class GoogleAddress implements IAddress {
	private static final long serialVersionUID = 1L;

	private JGoogleAddress data;
	private String mCountry;
	private String mProvince;
	private String mLocality;
	private String mSubLocality;
	private String mRoute;
	

	public GoogleAddress(JGoogleAddress data) {
		this.data = data;
	}

	public GoogleAddress(Parcel p) {
		mCountry = p.readString();
		mProvince = p.readString();
		mLocality = p.readString();
		mSubLocality = p.readString();
		mRoute = p.readString();
	}

	public String getCountry() {
		if (mCountry == null)
			mCountry = getLongNameByType(JGoogleAddress.TYPE_COUNTRY);
		return mCountry;
	}

	public String getProvince() {
		if (mProvince == null)
			mProvince = getLongNameByType(JGoogleAddress.TYPE_PROVINCE);
		return mProvince;
	}

	public String getLocality() {
		if (mLocality == null)
			mLocality = getLongNameByType(JGoogleAddress.TYPE_CITY);
		return mLocality;
	}

	public String getSubLocality() {
		if (mSubLocality == null)
			mSubLocality = getLongNameByType(JGoogleAddress.TYPE_SUBLOCALITY);
		return mSubLocality;
	}

	public String getRoute() {
		if (mRoute == null)
			mRoute = getLongNameByType(JGoogleAddress.TYPE_ROUTE);
		return mRoute;
	}

	public String getDetailAddress() {
		return getLocality() + getSubLocality() + getRoute();
	}

	private String getLongNameByType(String typeName) {
		if (data != null && data.address_components != null)
			for (JGoogleAddressComponent c : data.address_components) {
				if (typeName.equals(c.getFirstType()))
					return c.long_name;
			}
		return "";
	}

	@Override
	public String getCityName() {
		return getLocality();
	}

	public static final Parcelable.Creator<GoogleAddress> CREATOR =
			new Parcelable.Creator<GoogleAddress>() {
				public GoogleAddress createFromParcel(Parcel in) {
					GoogleAddress res = new GoogleAddress(in);
					return res;
				}

				public GoogleAddress[] newArray(int size) {
					return new GoogleAddress[size];
				}
			};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getCountry());
		dest.writeString(getProvince());
		dest.writeString(getLocality());
		dest.writeString(getSubLocality());
		dest.writeString(getRoute());
	}

	@Override
	public String toString() {
		return getDetailAddress();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		return toString().equals(o.toString());
	}
}
