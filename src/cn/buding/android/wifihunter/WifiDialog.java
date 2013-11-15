package cn.buding.android.wifihunter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import cn.buding.android.common.util.Utils;
import cn.buding.android.wifihunter.json.JWifiInfo;

class WifiDialog extends AlertDialog implements View.OnClickListener,
		TextWatcher {
	static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
	static final int BUTTON_FORGET = DialogInterface.BUTTON_NEUTRAL;

	final boolean edit;
	private final DialogInterface.OnClickListener mListener;
	private final AccessPoint mAccessPoint;

	private View mView;
	private TextView mSsid;
	private int mSecurity;
	private TextView mPassword;

	WifiDialog(Context context, DialogInterface.OnClickListener listener,
			AccessPoint accessPoint, boolean edit) {
		super(context);
		this.edit = edit;
		mListener = listener;
		mAccessPoint = accessPoint;
		mSecurity =
				(accessPoint == null) ? AccessPoint.SECURITY_NONE : accessPoint
						.getSecurity();
	}

	WifiConfiguration getConfig() {
		// the access point has been connected, we have its WifiConfig and do not need to create a new one.
		if (mAccessPoint != null && mAccessPoint.getNetWorkId() != -1 && !edit) {
			return null;
		}

		WifiConfiguration config = new WifiConfiguration();

		if (mAccessPoint.getNetWorkId() == -1) {
			config.SSID =
					AccessPoint.convertToQuotedString(mAccessPoint.getSsid());
		} else {
			config.networkId = mAccessPoint.getNetWorkId();
		}

		switch (mSecurity) {
		case AccessPoint.SECURITY_NONE:
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			return config;

		case AccessPoint.SECURITY_WEP:
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
			if (mPassword.length() != 0) {
				int length = mPassword.length();
				String password = mPassword.getText().toString();
				// WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
				if ((length == 10 || length == 26 || length == 58)
						&& password.matches("[0-9A-Fa-f]*")) {
					config.wepKeys[0] = password;
				} else {
					config.wepKeys[0] = '"' + password + '"';
				}
				mAccessPoint.setUserInputPwd(password);
			}
			return config;

		case AccessPoint.SECURITY_PSK:
			config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
			if (mPassword.length() != 0) {
				String password = mPassword.getText().toString();
				if (password.matches("[0-9A-Fa-f]{64}")) {
					config.preSharedKey = password;
				} else {
					config.preSharedKey = '"' + password + '"';
				}
				mAccessPoint.setUserInputPwd(password);
			}
			return config;

		case AccessPoint.SECURITY_EAP:
			config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
			config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
			// TODO we do not support EAP
			return config;
		}
		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mView = getLayoutInflater().inflate(R.layout.wifi_dialog, null);
		setView(mView);
		setInverseBackgroundForced(true);

		Context context = getContext();
		Resources resources = context.getResources();

		if (mAccessPoint == null) {
		} else {
			setTitle(mAccessPoint.getSsid());
			ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);
			mPassword = (TextView) mView.findViewById(R.id.password);
			DetailedState state = mAccessPoint.getState();
			if (state != null) {
				addRow(group, R.string.wifi_status, AccessPointView.get(
						getContext(), state));
			}

			String[] type = resources.getStringArray(R.array.wifi_security);
			addRow(group, R.string.wifi_security, type[mAccessPoint
					.getSecurity()]);

			int level = mAccessPoint.getLevel();
			if (level != -1) {
				String[] signal = resources.getStringArray(R.array.wifi_signal);
				addRow(group, R.string.wifi_signal, signal[level]);
			}

			WifiInfo info = mAccessPoint.getInfo();
			if (info != null) {
				addRow(group, R.string.wifi_speed, info.getLinkSpeed()
						+ WifiInfo.LINK_SPEED_UNITS);
				// TODO: fix the ip address for IPv6.
				int address = info.getIpAddress();
				if (address != 0) {
					addRow(group, R.string.wifi_ip_address, Formatter
							.formatIpAddress(address));
				}
			}

			JWifiInfo storedInfo = mAccessPoint.getStoredInfo();
			if (storedInfo != null && Utils.notNullEmpty(storedInfo.shop_name)) {
				addRow(group, R.string.wifi_shop_name, storedInfo.shop_name);
			}

			if (mAccessPoint.getNetWorkId() == -1 || edit) {
				if (mAccessPoint.hasStoredPwd())
					mPassword.setText(mAccessPoint.getStoredPwd());
				showSecurityFields();
			}

			if (edit) {
				setButton(BUTTON_SUBMIT, context.getString(R.string.wifi_save),
						mListener);
			} else {
				if (state == null && level != -1) {
					setButton(BUTTON_SUBMIT, context
							.getString(R.string.wifi_connect), mListener);
				}
				if (mAccessPoint.getNetWorkId() != -1) {
					setButton(BUTTON_FORGET, context
							.getString(R.string.wifi_forget), mListener);
				}
			}
		}

		setButton(DialogInterface.BUTTON_NEGATIVE, context
				.getString(R.string.wifi_cancel), mListener);

		super.onCreate(savedInstanceState);

		if (getButton(BUTTON_SUBMIT) != null) {
			validate();
		}
	}

	private void addRow(ViewGroup group, int name, String value) {
		View row =
				getLayoutInflater().inflate(R.layout.wifi_dialog_row, group,
						false);
		((TextView) row.findViewById(R.id.name)).setText(name);
		((TextView) row.findViewById(R.id.value)).setText(value);
		group.addView(row);
	}

	private void validate() {
		// TODO: make sure this is complete.
		if ((mSsid != null && mSsid.length() == 0)
				|| ((mAccessPoint == null || mAccessPoint.getNetWorkId() == -1) && ((mSecurity == AccessPoint.SECURITY_WEP && mPassword
						.length() == 0) || (mSecurity == AccessPoint.SECURITY_PSK && mPassword
						.length() < 8)))) {
			getButton(BUTTON_SUBMIT).setEnabled(false);
		} else {
			getButton(BUTTON_SUBMIT).setEnabled(true);
		}
	}

	public void onClick(View view) {
		mPassword.setInputType(InputType.TYPE_CLASS_TEXT
				| (((CheckBox) view).isChecked()
						? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
						: InputType.TYPE_TEXT_VARIATION_PASSWORD));
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	public void afterTextChanged(Editable editable) {
		validate();
	}

	private void showSecurityFields() {
		if (mSecurity == AccessPoint.SECURITY_NONE) {
			mView.findViewById(R.id.fields).setVisibility(View.GONE);
			return;
		}
		mView.findViewById(R.id.fields).setVisibility(View.VISIBLE);

		mPassword.addTextChangedListener(this);
		((CheckBox) mView.findViewById(R.id.show_password))
				.setOnClickListener(this);

		if (mAccessPoint != null && mAccessPoint.getNetWorkId() != -1) {
			mPassword.setHint(R.string.wifi_unchanged);
		}

	}

}
