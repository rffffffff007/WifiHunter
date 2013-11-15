package cn.buding.android.common.asynctask;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import cn.buding.android.common.exception.CustomException;
import cn.buding.android.common.exception.ECode;
import cn.buding.android.common.log.LogTag;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.widget.MyToast;

/**
 * handle error and success messages.
 * the {@link #doInBackground(Void...)} will return a value in {@link ECode}, we will show correspond error messages for
 * the returned error code.
 */
public abstract class HandlerMessageTask extends
		HandlerTask<Void, Void, Object> {
	private boolean showCodeMsg = true;
	protected String codeMsg = null;
	private Map<Integer, Object> codeMsgs = new HashMap<Integer, Object>();
	private static Map<Integer, Object> defaultCodeMsgs;

	static {
		defaultCodeMsgs = new HashMap<Integer, Object>();
		defaultCodeMsgs.put(ECode.SUCCESS, "");
		defaultCodeMsgs.put(ECode.SUCCESS_LAST_TIME, "");
		defaultCodeMsgs.put(ECode.SERVER_RETURN_EMPTY_SET, "到头了");
		defaultCodeMsgs.put(ECode.SERVER_RETURN_EMPTY_SET_FIRST_TIME, "暂时没有数据");
		defaultCodeMsgs.put(ECode.FAIL, "网络连接失败，请稍候重试");
		defaultCodeMsgs.put(ECode.CANNOT_LOCATE, "暂时无法定位");
	}

	public HandlerMessageTask(Context context) {
		super(context);
	}

	public static void setDefaultCodeMsg(int code, Object value) {
		defaultCodeMsgs.put(code, value);
	}

	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);
		int code = 0;
		// doInBackground will return a CustomException or a Integer.
		if (result instanceof CustomException) {
			CustomException e = (CustomException) result;
			code = e.getCode();
			if (ECode.isServerError(code))
				codeMsg = e.getMessage();
		} else if (result instanceof Integer) {
			code = (Integer) result;
		} else if (result.equals(ECode.CANCELED)) {
			return;
		} else {
			code = ECode.FAIL;
		}

		if (!ECode.isServerError(code)) {
			// if code is not server error, and it is not in defaultCodeMsgs,
			// then we set it to Fail. In fact only SERVER_RETURN_NULL and
			// SERVER_RETURN_ERROR are treated as Fail.
			if (getDefaultCodeMsg(code) == null && getCodeMsg(code) == null) {
				code = ECode.FAIL;
			}
			codeMsg = getCodeMsg(code);
			if (codeMsg == null)
				codeMsg = getDefaultCodeMsg(code);
		}
		if (showCodeMsg && codeMsg != null && codeMsg.length() > 0)
			MyToast.makeText(mContext, codeMsg).show();
		if (code == ECode.SUCCESS || code == ECode.SUCCESS_LAST_TIME) {
			if (successHandler != null)
				successHandler.process(code);
		} else {
			if (codeMsg == null)
				LogUtils.e(LogTag.TASK, "Error should not be null."
						+ this.getClass().getSimpleName());
			// in activities, we only handle SERVER_RETURN_EMPTY_SET, discard first time.
			if (code == ECode.SERVER_RETURN_EMPTY_SET_FIRST_TIME)
				code = ECode.SERVER_RETURN_EMPTY_SET;
			if (failHandler != null)
				failHandler.process(code);
		}
		if (finallyHandler != null)
			finallyHandler.process(code);
	}

	private String getDefaultCodeMsg(int code) {
		Object o = defaultCodeMsgs.get(code);
		return convertCodeMsg(o);
	}

	private String getCodeMsg(int code) {
		Object o = codeMsgs.get(code);
		return convertCodeMsg(o);
	}

	private String convertCodeMsg(Object o) {
		if (o == null)
			return null;
		if (o instanceof String)
			return (String) o;
		if (o instanceof Integer)
			return mContext.getResources().getString((Integer) o);

		return null;
	}

	protected void setShowCodeMsg(boolean b) {
		showCodeMsg = b;
	}

	public void setCodeMsg(Integer code, String msg) {
		codeMsgs.put(code, msg);
	}

	public void setCodeMsg(Integer code, int res) {
		codeMsgs.put(code, res);
	}

	public void disableCodeMsg(Integer code) {
		codeMsgs.put(code, "");
	}
}
