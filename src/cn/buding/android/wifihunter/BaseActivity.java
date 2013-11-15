package cn.buding.android.wifihunter;

import android.app.Activity;
import android.os.Bundle;

public abstract class BaseActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(getLayout());
		initElements();
		super.onCreate(savedInstanceState);
	}

	protected abstract int getLayout();

	protected abstract void initElements();
}
