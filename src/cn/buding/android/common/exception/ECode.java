package cn.buding.android.common.exception;

import java.lang.reflect.Field;

import cn.buding.android.wifihunter.json.JSONParser;

/**
 * ECode is class for error code. ECode has 2 part<br/>
 * The first part is Server Error. Error codes in this part shares the same value with server. when we receive a error
 * code from server, if it
 * is not in Server Error {@link #isServerError(int)}, we assign {@link #SERVER_RETURN_ERROR} to error code. this part
 * can be found in {@link JSONParser#parseWithCodeMessage(iw.avatar.net.ServerApi, String)}<br/>
 * The second part is Local Error. which is used by {@link HandlerMessageTask}
 */
public class ECode {
	/************************ Server Error ***************************/
	/**
	 * return success
	 */
	public final static int SERVER_RETURN_SUCCESS = 0;
	/**
	 * directly show error message
	 */
	private final static int SERVER_ERROR_SHOW_ERROR_MSG_START = 10100;
	private final static int SERVER_ERROR_SHOW_ERROR_MSG_END = 10200;

	/************************ Local Error ***************************/
	public final static int CANCELED = -100;
	public final static int SUCCESS = 1;
	public final static int FAIL = -1;
	/**
	 * error occurs in json parsing
	 */
	public final static int JSON_PARSER_ERROR = 10;
	/**
	 * server return null, mostly caused by socket timeout
	 */
	public final static int SERVER_RETURN_NULL = 11;
	/**
	 * server return not success
	 */
	public final static int SERVER_RETURN_ERROR = 12;
	/**
	 * the json array returned by server is empty
	 */
	public final static int SERVER_RETURN_EMPTY_SET = 15;
	/**
	 * server return empty set in the first time.
	 */
	public final static int SERVER_RETURN_EMPTY_SET_FIRST_TIME = 16;

	/**
	 * error in create a instance of a class
	 */
	public final static int CANNOT_INSTANTIATE = 17;
	/**
	 * cannot get location
	 */
	public final static int CANNOT_LOCATE = 18;
	/**
	 * success return. but the count return is less than request. which means
	 * that it should be the last page.
	 */
	public final static int SUCCESS_LAST_TIME = 2;

	/**
	 * whether error code is server error. if error code is in a specified arrange, we will show the error message
	 * return by server to user.
	 */
	public static boolean isServerError(int code) {
		return code >= SERVER_ERROR_SHOW_ERROR_MSG_START
				&& code <= SERVER_ERROR_SHOW_ERROR_MSG_END;
	}

	/**
	 * return field name of the code
	 */
	public static String codeStr(int code) {
		Field[] fs = ECode.class.getFields();
		for (Field f : fs) {
			try {
				if (f.getInt(null) == code)
					return f.getName();
			} catch (Exception e) {
			}
		}
		return null;
	}
}
