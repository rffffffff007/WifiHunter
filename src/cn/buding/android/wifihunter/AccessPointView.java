package cn.buding.android.wifihunter;

import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import cn.buding.android.wifihunter.AccessPoint.OnWifiInfoUpdateListener;

public class AccessPointView extends FrameLayout {
	private static final int[] STATE_HAS_PASSWORD =
			{ R.attr.state_encrypted, R.attr.state_stored };
	private static final int[] STATE_SECURED = { R.attr.state_encrypted };
	private static final int[] STATE_NONE = {};
	private ImageView ivSignal;
	private TextView tvSSID;
	private TextView tvDesc;
	private AccessPoint mAccessPoint;

	public AccessPointView(Context context) {
		super(context);
		final LayoutInflater layoutInflater =
				(LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		layoutInflater.inflate(R.layout.list_item_wifi, this);
	}

	public void setAccessPoint(AccessPoint point) {
		if (mAccessPoint != null)
			mAccessPoint.setOnWifiInfoUpdateListener(null);
		ivSignal = (ImageView) findViewById(R.id.iv_signal);
		tvSSID = (TextView) findViewById(R.id.tv_ssid);
		tvDesc = (TextView) findViewById(R.id.tv_desc);
		mAccessPoint = point;
		mAccessPoint
				.setOnWifiInfoUpdateListener(new OnWifiInfoUpdateListener() {
					@Override
					public void onWifiInfoUpdate() {
						refresh();
					}
				});
		refresh();
	}

	private void refresh() {
		if (mAccessPoint == null)
			return;
		tvSSID.setText(mAccessPoint.getSsid());
		if (mAccessPoint.getRssi() == Integer.MAX_VALUE) {
			ivSignal.setImageDrawable(null);
		} else {
			ivSignal.setImageResource(R.drawable.wifi_signal);
			int[] state = null;
			if(mAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE){
				state = STATE_NONE;
			}else if(mAccessPoint.hasStoredPwd()){
				state = STATE_HAS_PASSWORD;
			}else{
				state = STATE_SECURED;
			}
			ivSignal.setImageState(state, true);
		}
		Context context = getContext();
		ivSignal.setImageLevel(mAccessPoint.getLevel());
		DetailedState mState = mAccessPoint.getState();
		int mRssi = mAccessPoint.getRssi();
		int security = mAccessPoint.getSecurity();
		WifiConfiguration mConfig = mAccessPoint.getConfig();
		if (mState != null) {
			tvDesc.setText(get(context, mState));
		} else {
			String status = null;
			if (mRssi == Integer.MAX_VALUE) {
				status = context.getString(R.string.wifi_not_in_range);
			} else if (mConfig != null) {
				status =
						context
								.getString((mConfig.status == WifiConfiguration.Status.DISABLED)
										? R.string.wifi_disabled
										: R.string.wifi_remembered);
			}

			if (security == AccessPoint.SECURITY_NONE) {
				tvDesc.setText(status);
			} else {
				String format =
						context.getString((status == null)
								? R.string.wifi_secured
								: R.string.wifi_secured_with_status);
				String[] type =
						context.getResources().getStringArray(
								R.array.wifi_security);
				tvDesc.setText(String.format(format, type[security], status));
			}
		}
	}

	public static String get(Context context, DetailedState state) {
		String[] formats =
				context.getResources().getStringArray(R.array.wifi_status);
		int index = state.ordinal();
		if (index >= formats.length || formats[index].length() == 0) {
			return null;
		}
		return String.format(formats[index]);
	}

}
