package cn.buding.android.common.asynctask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.AsyncTask;

/**
 * a base class for all task.
 */
public abstract class BaseTask<A, B, C> extends AsyncTask<A, B, C> {
	protected Context mContext;
	protected Resources mRes;
	/** whether to finish current activity when the ProgressDialog is canceled. */
	private boolean mFinishActivityOnCancel = false;
	/** whether show the progress dialog when task is running in background */
	private boolean mShowProgressDialog = false;

	private String mMessage;
	private String mTitle;
	private ProgressDialog mProgressDialog;

	public BaseTask(Context context) {
		this(context, false);
	}

	public BaseTask(Context context, boolean finishActivityOnCancel) {
		this.mFinishActivityOnCancel = finishActivityOnCancel;
		this.mContext = context;
		mRes = context.getResources();
		mTitle = "消息处理";
		mMessage = "连接服务器";
	}

	protected void setShowProgessDialog(boolean f) {
		mShowProgressDialog = f;
	}

	protected void setFinishActivityOnCancel(boolean f) {
		mFinishActivityOnCancel = f;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (mShowProgressDialog) {
			showDialog();
		}
	}

	protected void onPostExecute(C result) {
		dismissDialog();
	}

	public void dismissDialog() {
		try {
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
		} catch (Exception e) {
		}
	}

	private void showDialog() {
		if (mProgressDialog == null)
			createLoadingDialog();
		if (!mProgressDialog.isShowing())
			mProgressDialog.show();
	}

	private void createLoadingDialog() {
		mProgressDialog = new ProgressDialog(mContext);
		mProgressDialog.setTitle(mTitle);
		mProgressDialog.setMessage(mMessage);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (mFinishActivityOnCancel) {
					finishContext();
				}
			}
		});
	}

	public void finishContext() {
		if (mContext instanceof Activity) {
			((Activity) mContext).finish();
		}
	}

	public void setLoadingTitle(String title) {
		mTitle = title;
	}

	public void setLoadingMessage(String message) {
		mMessage = message;
	}

}
