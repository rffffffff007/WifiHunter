/**
 * 
 */
package cn.buding.android.common.location;

import java.io.Serializable;
import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;
import cn.buding.android.common.location.google.GoogleAddress;
import cn.buding.android.common.location.google.WifiTelToLoc;
import cn.buding.android.wifihunter.net.ServerApi;

/**
 * custom class for location. a wrapper of android.loation.Location
 */
public class Location extends android.location.Location implements Serializable {
	private static final long serialVersionUID = 8107895935379992181L;
	/**
	 * our own location provider.
	 * 
	 * @see ServerApi#QueryPosition
	 */
	public final static String PROVIDER_SERVER = "server";
	/**
	 * google location provider.
	 * 
	 * @see WifiTelToLoc
	 */
	public final static String PROVIDER_GOOGLE = "google";
	/**
	 * the address of the location.
	 * 
	 * @see GoogleAddress
	 */
	private IAddress mAddress;

	public static Location EMPTY_LOCATION = new Location(0, 0);

	public Location() {
		this(0, 0);
	}

	public Location(double longi, double lati) {
		this(longi, lati, PROVIDER_SERVER);
	}

	public Location(android.location.Location loc) {
		super(loc);
	}
	
	public Location(Location loc){
		super(loc);
		mAddress = loc.getAddress();
	}

	public Location(double longi, double lati, String type) {
		this(longi, lati, System.currentTimeMillis(), type);
	}

	public Location(double longi, double lati, long time) {
		this(longi, lati, time, PROVIDER_SERVER);
	}

	public Location(double longi, double lati, long time, String provider) {
		super(provider);
		setTime(time);
		setLongitude(longi);
		setLatitude(lati);
	}

	public String toString() {
		return getLatitude() + "," + getLongitude() + ", " + getAccuracy()
				+ "," + getProvider() + ", " + new Date(getTime()).toString();
	}

	public String getLocStr() {
		return String.format("%.4f,%.4f", getLatitude(), getLongitude());
	}

	public void setAddress(IAddress address) {
		mAddress = address;
	}

	public IAddress getAddress() {
		return mAddress;
	}

	public String getAddressDetail() {
		if (mAddress != null)
			return mAddress.getDetailAddress();
		return getLocStr();
	}

	public String getCityName() {
		if (mAddress == null)
			return null;
		return mAddress.getCityName();
	}

	private static final float DEFAULT_ACCURACY = 2000;

	@Override
	public float getAccuracy() {
		if (hasAccuracy())
			return super.getAccuracy();
		return DEFAULT_ACCURACY;
	}

	/**
	 * @return is this location valid?
	 */
	public boolean isValid() {
		if (Math.abs(this.getLongitude()) < 0.0001
				&& Math.abs(this.getLatitude()) < 0.0001)
			return false;
		if (Math.abs(this.getLongitude() - (-1.0)) < 0.0001
				&& Math.abs(this.getLatitude() - (-1.0)) < 0.0001)
			return false;
		if (Math.abs(this.getLongitude()) > 180.0
				|| Math.abs(this.getLatitude()) > 90.0)
			return false;
		return true;
	}

	/**
	 * @return is the located time is in {@link #VALID_TIME_DURATION}
	 */
	public boolean isTimeValid() {
		return System.currentTimeMillis() - getTime() < VALID_TIME_DURATION;
	}

	private static final long VALID_TIME_DURATION = 15 * 60 * 1000;

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current Location fix
	 * 
	 * @param location The new Location that you want to evaluate
	 * @param currentBestLocation The current Location fix, to which you want to compare the new one
	 */
	public static boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (location == null)
			return false;
		if (currentBestLocation == null) {
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta =
				(int) (location.getAccuracy() - currentBestLocation
						.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider =
				isSameProvider(location.getProvider(), currentBestLocation
						.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public static final Parcelable.Creator<Location> CREATOR =
			new Parcelable.Creator<Location>() {
				public Location createFromParcel(Parcel in) {
					android.location.Location l =
							android.location.Location.CREATOR
									.createFromParcel(in);
					Location loc = new Location(l);
					String addressClass = in.readString();
					if (addressClass == null) {
					} else if (addressClass.equals(GoogleAddress.class
							.getName())) {
						loc.setAddress(new GoogleAddress(in));
					}
					return loc;
				}

				public Location[] newArray(int size) {
					return new Location[size];
				}
			};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		super.writeToParcel(parcel, flags);
		if (mAddress != null) {
			parcel.writeString(mAddress.getClass().getName());
			mAddress.writeToParcel(parcel, flags);
		}
	}
}
