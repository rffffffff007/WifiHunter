package cn.buding.android.wifihunter;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import cn.buding.android.common.widget.MyToast;
import cn.buding.android.wifihunter.util.GlobalValue;

public class WebActivity extends BaseActivity {
	public static final int MENU_ID_COMMIT = Menu.FIRST;
	public static final int MENU_ID_REFRESH = Menu.FIRST + 1;
	public static final int DIALOG_ID_COMMIT = 1;
	public static final String EXTRA_ACCESS_POINT_BSSID = "extra_access_point_bssid";
	public static final String INIT_URL = "http://www.baidu.com";
	private WebView mWebView;
	private AccessPoint mAccessPoint;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String pointBSSID = getIntent().getStringExtra(EXTRA_ACCESS_POINT_BSSID);
		mAccessPoint = GlobalValue.getIns().getAccessPointByBSSID(pointBSSID);
		if (mAccessPoint == null) {
			finish();
			return;
		}
	}

	@Override
	protected int getLayout() {
		return R.layout.web_activity;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ID_COMMIT, 0, "提交").setIcon(
				android.R.drawable.ic_menu_add);
		menu.add(Menu.NONE, MENU_ID_REFRESH, 0, "刷新").setIcon(
				android.R.drawable.ic_menu_rotate);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ID_COMMIT:
			showDialog(DIALOG_ID_COMMIT);
			return true;
		case MENU_ID_REFRESH:
			mWebView.loadUrl(INIT_URL);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ID_COMMIT:
			return new CommitWifiInfoDialog(this, mAccessPoint);
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void initElements() {
		mWebView = (WebView) findViewById(R.id.web);
		mWebView.loadUrl(INIT_URL);
		WebSettings setting = mWebView.getSettings();
		setting.setBuiltInZoomControls(true);
		setting.setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
	}
}
