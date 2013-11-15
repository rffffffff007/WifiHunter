package cn.buding.android.common.location;

import org.json.JSONObject;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import cn.buding.android.common.log.LogTag;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.net.BaseHttpsManager;

/**
 * Location service try to update location from 3 service.<br/>
 * 1 GPS, {@link LocationManager#GPS_PROVIDER}.<br/>
 * 2 Google Service in Android, {@link LocationManager#NETWORK_PROVIDER}<br/>
 * 3 3rd part location service {@link Location#PROVIDER_SERVER} {@link Location#PROVIDER_GOOGLE}
 */
public class LocationService {
	private Context context;

	private LocationManager locationServiceManager = null;
	private static LocationService instance;

	private static final int MIN_UPDATE_DISTANCE = 100;
	private static final int MIN_UPDATE_DURATION = 2 * 60 * 1000;

	public static LocationService getSingleInstance(Context context) {
		if (instance == null) {
			instance = new LocationService(context);
		}
		return instance;
	}

	private LocationService(Context context) {
		this.context = context;
		initLocationService();
	}

	/**
	 * init the 3 location service.
	 */
	private void initLocationService() {
		try {
			registerGoogleLocService();
		} catch (Exception e) {
			LogUtils.e(e);
		}
		try {
			registerGpsService();
		} catch (Exception e) {
			LogUtils.e(e);
		}
		try {
			registerServerLocService();
		} catch (Exception e) {
			LogUtils.e(e);
		}
	}

	/**
	 * get last located location of best loc service.
	 */
	public Location getLastLocatedLocation() {
		try {
			if (locationServiceManager == null)
				locationServiceManager =
						(LocationManager) context
								.getSystemService(Context.LOCATION_SERVICE);
			String provider =
					locationServiceManager
							.getBestProvider(new Criteria(), true);
			android.location.Location loc =
					locationServiceManager.getLastKnownLocation(provider);
			if (loc == null)
				return null;
			return new Location(loc);
		} catch (Exception e) {
			return null;
		}
	}

	private void onLocationUpdate(android.location.Location loc) {
		onLocationUpdate(new Location(loc));
	}

	private static final String CONVERSE_LOCATION_API =
			"http://www.wandouquan.com/avatar?api=avatar.api.ConverseLocation&param={latitude:%f,longitude:%f}";

	/**
	 * try to converse location and set to memory. location from server do not need to converse since the server has
	 * done this part.
	 */
	private void onLocationUpdate(final Location loc) {
		if (!loc.getProvider().equals(Location.PROVIDER_SERVER)) {
			new Thread() {
				@Override
				public void run() {
					try {
						String api =
								String.format(CONVERSE_LOCATION_API, loc
										.getLatitude(), loc.getLongitude());
						String result = BaseHttpsManager.sendPost(api);
						JSONObject job = new JSONObject(result);
						double latitude = job.getDouble("latitude");
						double longitude = job.getDouble("longitude");
						Location resLoc = new Location(loc);
						resLoc.setLatitude(latitude);
						resLoc.setLongitude(longitude);
						if (loc != null) {
							LogUtils.v(LogTag.LOCATION, loc.getLocStr()
									+ " convert to " + resLoc.getLocStr());
							LocationFactory.getSingleton(context).setmLocation(
									loc);
						}
					} catch (Exception e) {
						LogUtils.e(e);
						LocationFactory.getSingleton(context).setmLocation(loc);
					}
				}
			}.start();
		} else {
			LocationFactory.getSingleton(context).setmLocation(loc);
		}
	}

	private void registerServerLocService() {
		ServerLocService.getInstance(context).registerLocationUpdates(
				MIN_UPDATE_DURATION, new LocationListener() {
					@Override
					public void onStatusChanged(String provider, int status,
							Bundle extras) {
					}

					@Override
					public void onProviderEnabled(String provider) {
					}

					@Override
					public void onProviderDisabled(String provider) {
					}

					@Override
					public void onLocationChanged(
							android.location.Location location) {
						onLocationUpdate(location);
					}
				});
	}

	private LocationListener googleLocListener;

	private boolean registerGoogleLocService() {
		if (locationServiceManager == null)
			locationServiceManager =
					(LocationManager) context
							.getSystemService(Context.LOCATION_SERVICE);
		if (!locationServiceManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			return false;
		destroyGoogleLocListener();
		googleLocListener = new LocationListener() {
			public void onLocationChanged(android.location.Location location) {
				onLocationUpdate(location);
			}

			public void onProviderDisabled(String arg0) {
			}

			public void onProviderEnabled(String arg0) {
			}

			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			}
		};
		locationServiceManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, MIN_UPDATE_DURATION,
				MIN_UPDATE_DISTANCE, googleLocListener);
		return true;
	}

	private LocationListener gpsLocListener;

	private boolean registerGpsService() {
		if (locationServiceManager == null)
			locationServiceManager =
					(LocationManager) context
							.getSystemService(Context.LOCATION_SERVICE);
		if (!locationServiceManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER))
			return false;
		destroyGpsLocListener();
		gpsLocListener = new LocationListener() {
			public void onLocationChanged(android.location.Location location) {
				onLocationUpdate(location);
			}

			public void onProviderDisabled(String provider) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}
		};

		locationServiceManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, MIN_UPDATE_DURATION,
				MIN_UPDATE_DISTANCE, gpsLocListener);
		return true;
	}

	public void destroy() {
		destroyGoogleLocListener();
		destroyGpsLocListener();
		destroyServerLocListener();
		instance = null;
	}

	private void destroyGoogleLocListener() {
		if (googleLocListener != null) {
			locationServiceManager.removeUpdates(googleLocListener);
			googleLocListener = null;
		}
	}

	private void destroyGpsLocListener() {
		if (gpsLocListener != null) {
			locationServiceManager.removeUpdates(gpsLocListener);
			gpsLocListener = null;
		}
	}

	private void destroyServerLocListener() {
		ServerLocService.destroy();
	}
}
