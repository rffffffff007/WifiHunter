package cn.buding.android.wifihunter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import cn.buding.android.common.Application;
import cn.buding.android.common.asynctask.HandlerTask.MethodHandler;
import cn.buding.android.wifihunter.asynctask.GetWifiInfoTask;
import cn.buding.android.wifihunter.util.GlobalValue;

public class WifiSettings extends BaseActivity implements
		DialogInterface.OnClickListener, OnItemClickListener {
	private static final int MENU_ID_SCAN = Menu.FIRST;
	private static final int MENU_ID_CONNECT = Menu.FIRST + 2;
	private static final int MENU_ID_FORGET = Menu.FIRST + 3;
	private static final int MENU_ID_MODIFY = Menu.FIRST + 4;

	private final IntentFilter mFilter;
	private final BroadcastReceiver mReceiver;
	private final Scanner mScanner;

	private WifiManager mWifiManager;
	private List<AccessPoint> mAccessPoints;

	private DetailedState mLastState;
	private WifiInfo mLastInfo;
	private int mLastPriority;
	private boolean mResetNetworks = false;
	private AccessPoint mSelected;
	private AccessPoint mCurrentConnecting;
	private WifiDialog mDialog;

	private ListView mListView;
	private BaseAdapter mAdapter;

	public WifiSettings() {
		mFilter = new IntentFilter();
		mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				handleEvent(intent);
			}
		};
		mScanner = new Scanner();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mAccessPoints = GlobalValue.getIns().getAccessPoints();
		mAdapter = new AccessPointAdapter(this);
		mListView.setAdapter(mAdapter);
	}

	@Override
	protected void onDestroy() {
		GlobalValue.getIns().clear();
		((Application) getApplication()).onAppEnd();
		super.onDestroy();
	}

	@Override
	protected int getLayout() {
		return R.layout.wifi_info_list;
	}

	@Override
	protected void initElements() {
		mListView = (ListView) findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		registerForContextMenu(mListView);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mReceiver, mFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
		mScanner.pause();
		if (mResetNetworks) {
			enableNetworks();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ID_SCAN, 0, R.string.wifi_menu_scan).setIcon(
				R.drawable.ic_menu_scan_network);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ID_SCAN:
			if (mWifiManager.isWifiEnabled()) {
				mScanner.resume();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo info) {
		if (info instanceof AdapterContextMenuInfo) {
			int position = ((AdapterContextMenuInfo) info).position;
			AccessPoint preference = mAccessPoints.get(position);
			mSelected = preference;
			menu.setHeaderTitle(mSelected.getSsid());
			if (mSelected.getLevel() != -1 && mSelected.getState() == null) {
				menu.add(Menu.NONE, MENU_ID_CONNECT, 0,
						R.string.wifi_menu_connect);
			}
			if (mSelected.getNetWorkId() != -1) {
				menu.add(Menu.NONE, MENU_ID_FORGET, 0,
						R.string.wifi_menu_forget);
				if (mSelected.getSecurity() != AccessPoint.SECURITY_NONE) {
					menu.add(Menu.NONE, MENU_ID_MODIFY, 0,
							R.string.wifi_menu_modify);
				}
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (mSelected == null) {
			return super.onContextItemSelected(item);
		}
		switch (item.getItemId()) {
		case MENU_ID_CONNECT:
			if (mSelected.getNetWorkId() != -1) {
				connect(mSelected.getNetWorkId());
			} else if (mSelected.getSecurity() == AccessPoint.SECURITY_NONE) {
				// Shortcut for open networks.
				WifiConfiguration config = new WifiConfiguration();
				config.SSID =
						AccessPoint.convertToQuotedString(mSelected.getSsid());
				config.allowedKeyManagement.set(KeyMgmt.NONE);
				int networkId = mWifiManager.addNetwork(config);
				mWifiManager.enableNetwork(networkId, false);
				connect(networkId);
			} else {
				showDialog(mSelected, false);
			}
			return true;
		case MENU_ID_FORGET:
			forget(mSelected.getNetWorkId());
			return true;
		case MENU_ID_MODIFY:
			showDialog(mSelected, true);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		mSelected = (AccessPoint) parent.getItemAtPosition(position);
		showDialog(mSelected, false);
	}

	public void onClick(DialogInterface dialogInterface, int button) {
		if (button == WifiDialog.BUTTON_FORGET && mSelected != null) {
			forget(mSelected.getNetWorkId());
		} else if (button == WifiDialog.BUTTON_SUBMIT && mDialog != null) {
			WifiConfiguration config = mDialog.getConfig();

			if (config == null) {
				// the access point has been saved, we have its WifiConfig and do not need to create a new one.
				if (mSelected != null) {
					connect(mSelected.getNetWorkId());
				}
			} else if (config.networkId != -1) {
				// modify the pwd of saved WifiConfig
				if (mSelected != null) {
					mWifiManager.updateNetwork(config);
					saveNetworks();
				}
			} else {
				int networkId = mWifiManager.addNetwork(config);
				if (networkId != -1) {
					mWifiManager.enableNetwork(networkId, false);
					config.networkId = networkId;
					if (mDialog.edit) {
						saveNetworks();
					} else {
						connect(networkId);
					}
				}
			}
		}
	}

	private void showDialog(AccessPoint accessPoint, boolean edit) {
		if (mDialog != null) {
			mDialog.dismiss();
		}
		mDialog = new WifiDialog(this, this, accessPoint, edit);
		mDialog.show();
	}

	private void forget(int networkId) {
		mWifiManager.removeNetwork(networkId);
		AccessPoint point = null;
		for (AccessPoint p : mAccessPoints) {
			if (p.getNetWorkId() == networkId) {
				point = p;
				break;
			}
		}
		if (point != null)
			point.removeWifiConfiguration();
		saveNetworks();
	}

	private void connect(int networkId) {
		if (networkId == -1) {
			return;
		}

		// Reset the priority of each network if it goes too high.
		if (mLastPriority > 1000000) {
			for (int i = mAccessPoints.size() - 1; i >= 0; --i) {
				AccessPoint accessPoint = mAccessPoints.get(i);
				if (accessPoint.getNetWorkId() != -1) {
					WifiConfiguration config = new WifiConfiguration();
					config.networkId = accessPoint.getNetWorkId();
					config.priority = 0;
					mWifiManager.updateNetwork(config);
				}
			}
			mLastPriority = 0;
		}

		mCurrentConnecting = mSelected;

		// Set to the highest priority and save the configuration.
		WifiConfiguration config = new WifiConfiguration();
		config.networkId = networkId;
		config.priority = ++mLastPriority;
		mWifiManager.updateNetwork(config);
		saveNetworks();

		// Connect to network by disabling others.
		mWifiManager.enableNetwork(networkId, true);
		mWifiManager.reconnect();
		mResetNetworks = true;

	}

	private void enableNetworks() {
		for (int i = mAccessPoints.size() - 1; i >= 0; --i) {
			WifiConfiguration config =
					((AccessPoint) mAccessPoints.get(i)).getConfig();
			if (config != null && config.status != Status.ENABLED) {
				mWifiManager.enableNetwork(config.networkId, false);
			}
		}
		mResetNetworks = false;
	}

	private void saveNetworks() {
		// Always save the configuration with all networks enabled.
		enableNetworks();
		mWifiManager.saveConfiguration();
		updateAccessPoints();
	}

	List<AccessPoint> oldAccessPoints = new ArrayList<AccessPoint>();
	List<AccessPoint> newAccessPoints = new ArrayList<AccessPoint>();

	private void updateAccessPoints() {
		oldAccessPoints.clear();
		newAccessPoints.clear();
		oldAccessPoints.addAll(mAccessPoints);
		mAccessPoints.clear();

		List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
		if (configs != null) {
			mLastPriority = 0;
			for (WifiConfiguration config : configs) {
				if (config.priority > mLastPriority) {
					mLastPriority = config.priority;
				}

				// Shift the status to make enableNetworks() more efficient.
				if (config.status == Status.CURRENT) {
					config.status = Status.ENABLED;
				} else if (mResetNetworks && config.status == Status.DISABLED) {
					config.status = Status.CURRENT;
				}

				AccessPoint samePoint = null;
				for (AccessPoint accessPoint : oldAccessPoints) {
					if (accessPoint.update(config)) {
						samePoint = accessPoint;
					}
				}
				if (samePoint == null) {
					samePoint = new AccessPoint(config);
					oldAccessPoints.add(samePoint);
					newAccessPoints.add(samePoint);
				}
				samePoint.update(mLastInfo, mLastState);
				if (!mAccessPoints.contains(samePoint))
					mAccessPoints.add(samePoint);

			}
		}

		List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {
				// Ignore hidden and ad-hoc networks.
				if (result.SSID == null || result.SSID.length() == 0
						|| result.capabilities.contains("[IBSS]")) {
					continue;
				}

				AccessPoint samePoint = null;
				for (AccessPoint accessPoint : oldAccessPoints) {
					if (accessPoint.update(result)) {
						samePoint = accessPoint;
					}
				}
				if (samePoint == null) {
					samePoint = new AccessPoint(result);
					oldAccessPoints.add(samePoint);
					newAccessPoints.add(samePoint);
				}
				if (!mAccessPoints.contains(samePoint))
					mAccessPoints.add(samePoint);
			}
		}

		Collections.sort(mAccessPoints);
		mAdapter.notifyDataSetChanged();

		List<String> reqBssids = new ArrayList<String>();
		for (AccessPoint p : newAccessPoints)
			if (p.getBssids() != null)
				reqBssids.addAll(p.getBssids());
		new GetWifiInfoTask(this, reqBssids).setOnExecuteSuccessHandler(
				new MethodHandler<Object>() {
					@Override
					public void process(Object para) {
						mAdapter.notifyDataSetChanged();
					}
				}).execute();
	}

	private void handleEvent(Intent intent) {
		String action = intent.getAction();
		if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
			updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
					WifiManager.WIFI_STATE_UNKNOWN));
		} else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
			updateAccessPoints();
		} else if (WifiManager.NETWORK_IDS_CHANGED_ACTION.equals(action)) {
			if (mSelected != null && mSelected.getNetWorkId() != -1) {
				mSelected = null;
			}
			updateAccessPoints();
		} else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
			updateConnectionState(WifiInfo
					.getDetailedStateOf((SupplicantState) intent
							.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
		} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
			updateConnectionState(((NetworkInfo) intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO))
					.getDetailedState());
		} else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
			updateConnectionState(null);
		}
	}

	private void updateConnectionState(DetailedState state) {
		/* sticky broadcasts can call this when wifi is disabled */
		if (!mWifiManager.isWifiEnabled()) {
			mScanner.pause();
			return;
		}

		if (state == DetailedState.OBTAINING_IPADDR) {
			mScanner.pause();
		} else {
			mScanner.resume();
		}

		mLastInfo = mWifiManager.getConnectionInfo();
		if (state != null) {
			mLastState = state;
		}

		for (int i = mAccessPoints.size() - 1; i >= 0; --i) {
			((AccessPoint) mAccessPoints.get(i)).update(mLastInfo, mLastState);
		}

		if (mLastInfo != null && mCurrentConnecting != null
				&& mCurrentConnecting.getSsid() != null
				&& mCurrentConnecting.getSsid().equals(mLastInfo.getSSID())
				&& state == DetailedState.CONNECTED) {
			// wifi connected. and start next web activity.
			Intent intent = new Intent(this, WebActivity.class);
			intent.putExtra(WebActivity.EXTRA_ACCESS_POINT_BSSID,
					mCurrentConnecting.getBssid());
			startActivity(intent);
			mCurrentConnecting = null;
		}

		if (mResetNetworks
				&& (state == DetailedState.CONNECTED
						|| state == DetailedState.DISCONNECTED || state == DetailedState.FAILED)) {
			updateAccessPoints();
			enableNetworks();
		}
	}

	private void updateWifiState(int state) {
		if (state == WifiManager.WIFI_STATE_ENABLED) {
			mScanner.resume();
			updateAccessPoints();
		} else {
			mScanner.pause();
			mAccessPoints.clear();
			mAdapter.notifyDataSetChanged();
		}
	}

	private class Scanner extends Handler {
		private int mRetry = 0;

		void resume() {
			if (!hasMessages(0)) {
				sendEmptyMessage(0);
			}
		}

		void pause() {
			mRetry = 0;
			// mAccessPoints.setProgress(false);
			// TODO set progress
			removeMessages(0);
		}

		@Override
		public void handleMessage(Message message) {
			if (mWifiManager.startScan()) {
				mRetry = 0;
			} else if (++mRetry >= 3) {
				mRetry = 0;
				Toast.makeText(WifiSettings.this, R.string.wifi_fail_to_scan,
						Toast.LENGTH_LONG).show();
				return;
			}
			// mAccessPoints.setProgress(mRetry != 0);
			// TODO set progress
			sendEmptyMessageDelayed(0, 6000);
		}
	}

	class AccessPointAdapter extends BaseAdapter {
		private Context mContext;

		public AccessPointAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return mAccessPoints.size();
		}

		@Override
		public AccessPoint getItem(int position) {
			return mAccessPoints.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = new AccessPointView(mContext);
			AccessPointView apView = (AccessPointView) convertView;
			apView.setAccessPoint(getItem(position));
			return convertView;
		}

	}
}
