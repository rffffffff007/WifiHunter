package cn.buding.android.wifihunter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.NetworkInfo.DetailedState;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.util.Utils;
import cn.buding.android.common.widget.MyToast;
import cn.buding.android.wifihunter.asynctask.AddWifiInfoTask;
import cn.buding.android.wifihunter.json.JWifiInfo;

class CommitWifiInfoDialog extends AlertDialog implements
		DialogInterface.OnClickListener {
	static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
	static final int BUTTON_CANCEL = DialogInterface.BUTTON_NEGATIVE;

	private final AccessPoint mAccessPoint;

	private EditText etShopName;
	private TextView tvError;

	CommitWifiInfoDialog(Context context, AccessPoint accessPoint) {
		super(context);
		mAccessPoint = accessPoint;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		View mView =
				getLayoutInflater().inflate(R.layout.dialog_commit_wifi, null);
		setView(mView);
		setInverseBackgroundForced(true);
		tvError = (TextView) mView.findViewById(R.id.tv_error);
		etShopName = (EditText) mView.findViewById(R.id.et_shopname);

		Context context = getContext();
		Resources resources = context.getResources();

		if (mAccessPoint != null) {
			setTitle(mAccessPoint.getSsid());
			ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);

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

			JWifiInfo storedInfo = mAccessPoint.getStoredInfo();
			String shopname = "";
			if (storedInfo != null && Utils.notNullEmpty(storedInfo.shop_name)) {
				shopname = storedInfo.shop_name;
			}else{
				etShopName.setVisibility(View.VISIBLE);
			}
			CompoundButton btShopName =
					(CompoundButton) mView.findViewById(R.id.shopname);
			
			btShopName.setText(shopname);
			btShopName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleShopNameField();
				}
			});

			setButton(BUTTON_SUBMIT, context.getString(R.string.wifi_submit),
					this);
			setButton(BUTTON_CANCEL, context.getString(R.string.wifi_cancel),
					this);
		}
		super.onCreate(savedInstanceState);
		validate();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case BUTTON_SUBMIT:
			String shopName = etShopName.getText().toString();
			new AddWifiInfoTask(getContext(), mAccessPoint, shopName).execute();
			break;
		case BUTTON_CANCEL:
			dialog.cancel();
			break;
		}
	}

	private void toggleShopNameField() {
		if (etShopName.getVisibility() == View.GONE)
			etShopName.setVisibility(View.VISIBLE);
		else
			etShopName.setVisibility(View.GONE);
	}

	private View addRow(ViewGroup group, int name, String value) {
		View row =
				getLayoutInflater().inflate(R.layout.wifi_dialog_row, group,
						false);
		((TextView) row.findViewById(R.id.name)).setText(name);
		((TextView) row.findViewById(R.id.value)).setText(value);
		group.addView(row);
		return row;
	}

	private void validate() {
		if (mAccessPoint == null) {
			getButton(BUTTON_SUBMIT).setEnabled(false);
			LogUtils.e("AccessPoint is null in CommintWifiInfoDialog");
			return;
		}
		String ssid = mAccessPoint.getSsid();
		int security = mAccessPoint.getSecurity();
		String pwd = mAccessPoint.getUserInputPwd();
		if (mAccessPoint.getNetWorkId() == -1 || mAccessPoint.getInfo() == null
				|| mAccessPoint.getState() != DetailedState.CONNECTED) {
			setErrorText(R.string.wifi_not_connected);
		} else if ((ssid != null && ssid.length() == 0)) {
			setErrorText(R.string.wifi_ssid_empty);
		} else if (pwd != null
				&& ((security == AccessPoint.SECURITY_WEP && pwd.length() == 0) || (security == AccessPoint.SECURITY_PSK && pwd
						.length() < 8))) {
			setErrorText(R.string.wifi_password_error);
		} else {
			getButton(BUTTON_SUBMIT).setEnabled(true);
			tvError.setVisibility(View.GONE);
		}
	}

	private void setErrorText(int res) {
		tvError.setText(res);
		tvError.setVisibility(View.VISIBLE);
		getButton(BUTTON_SUBMIT).setEnabled(false);
	}
}
