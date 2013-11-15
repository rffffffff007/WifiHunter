package cn.buding.android.common;

import cn.buding.android.common.location.LocationFactory;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.net.BaseHttpsManager;

public class Application extends android.app.Application {
	@Override
	public void onCreate() {
		super.onCreate();
		onAppStart();
	}

	private void onAppStart() {
		LogUtils.setLogTag("wifi");
		LogUtils.i("Application onAppStart");
		BaseHttpsManager.init(this);
	}

	public void onAppEnd() {
		LogUtils.i("Application onAppEnd");
	}
}
