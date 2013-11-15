package cn.buding.android.common.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import cn.buding.android.common.log.LogTag;
import cn.buding.android.common.log.LogUtils;
import cn.buding.android.common.util.PackageUtils;

/**
 * contain the base methods to invoke api via http. only for android.
 */
public class BaseHttpsManager {
	/** connection timeout duration. */
	private static int TIMEOUT_CONNECTION = 20000;
	/** socket timeout duration */
	private static int TIMEOUT_SOCKET = 20000;

	protected static final String TAG_PARAM = "param";
	protected static final String PARAM_SERVER_API = "api";
	protected static final String PARAM_COMPRESS = "compress";

	private static Context mContext;

	private static HttpParams mHttpParams;

	/**
	 * must be called before invoke the class.
	 * 
	 * @param context
	 */
	public static void init(Context context) {
		mContext = context;
		initHttpParameters(context);
	}

	/**
	 * init the httpParam.
	 * add proxy to param if the network is cmwap.
	 */
	private static void initHttpParameters(Context context) {
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				TIMEOUT_CONNECTION);
		HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_SOCKET);
		if (NetUtil.isNetworkCmwap(context)
				&& httpParameters.getParameter(ConnRoutePNames.DEFAULT_PROXY) == null) {
			HttpHost proxy =
					new HttpHost(android.net.Proxy.getDefaultHost(),
							android.net.Proxy.getDefaultPort(), "http");
			httpParameters.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
	}

	public static String postAPI(RequestParam request) {
		return postAPI(request.getHost(), request.val());
	}

	public static final String REQUEST_LOG_FORMAT = "LEN:%.1f,%s";
	public static final String RESPONSE_LOG_FORMAT = "LEN:%.1f,%s return: %s";

	/**
	 * post a api to urlStr
	 * 
	 * @param urlStr the url to post the param
	 * @param params the request param.
	 * @param flag post flag. {@link #FLAG_UPLOADLOG}
	 * @return result of response
	 */
	public static String postAPI(String urlStr, RequestParam params) {
		boolean wheCompressed = params.isCompress();
		String responseStr = null;
		String API = params.getApi();

		try {
			LogUtils.v(LogTag.REQUEST, String.format(REQUEST_LOG_FORMAT, params
					.toString().length() / 1024f, params.toString()));
			responseStr = postAPI(urlStr, (List<NameValuePair>) params);
			float contentLength = responseStr.length() / 1024f;
			if (wheCompressed)
				responseStr = NetUtil.decodePostApi(responseStr);
			LogUtils.v(LogTag.RESPONSE, String.format(RESPONSE_LOG_FORMAT,
					contentLength, API, responseStr));
		} catch (Exception e) {
			LogUtils.e("Error in http request", e);
		}
		return responseStr;
	}

	public static String postAPI(String urlStr, List<NameValuePair> params)
			throws ParseException, IOException {
		HttpEntity requestEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
		return postAPI(urlStr, requestEntity);
	}

	public static String postAPI(String urlStr, HttpEntity requestEntity)
			throws ParseException, IOException {
		if (mContext == null)
			throw new RuntimeException("Must call init() first");
		if (urlStr == null || requestEntity == null)
			throw new RuntimeException("Param url or entity is null.");
		HttpPost httpRequest = new HttpPost(urlStr);
		httpRequest.setEntity(requestEntity);
		HttpResponse httpResponse =
				new DefaultHttpClient(mHttpParams).execute(httpRequest);
		String responseStr = null;
		if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			HttpEntity responseEntity = httpResponse.getEntity();
			responseStr = EntityUtils.toString(responseEntity);
		}
		return responseStr;
	}

	/**
	 * send a simple post request to url
	 * 
	 * @param url
	 * @return response
	 */
	public static String sendPost(String url) {
		if (mContext == null)
			throw new RuntimeException("must call init() first");
		LogUtils.d(LogTag.REQUEST, url);
		try {
			URL postUrl = new URL(url);
			HttpURLConnection connection =
					NetUtil.getHttpUrlConnection(mContext, postUrl);
			connection.setConnectTimeout(TIMEOUT_CONNECTION);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("GET");
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			connection.connect();
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(connection
							.getInputStream()));
			StringBuilder sb = new StringBuilder("");
			String line;
			while ((line = reader.readLine()) != null)
				sb.append(line).append("\n");
			reader.close();
			connection.disconnect();
			LogUtils.d(LogTag.RESPONSE, subString(url, 10) + " Return: "
					+ subString(sb.toString(), 100));
			return sb.toString();
		} catch (IOException e) {
			LogUtils.e(e);
		}
		return null;
	}

	private static String subString(String str, int maxSize) {
		int len = str.length();
		if (len <= maxSize)
			return str;
		return str.substring(0, maxSize).replace("\n", "\t") + "...";
	}

	/**
	 * get http response of a remote file.
	 * 
	 * @param fileUrl
	 * @return
	 */
	public static HttpResponse getHttpResponse(Context context, String fileUrl) {
		HttpGet httpRequest = new HttpGet(fileUrl);
		HttpResponse httpResponse = null;
		try {
			httpResponse =
					new DefaultHttpClient(mHttpParams).execute(httpRequest);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
		return httpResponse;
	}

	/**
	 * base class tools that wrap the params for {@link BaseHttpsManager#postAPI(Context, String, RequestParam, int)}
	 * add some global param. like version, app_name, channel.
	 */
	protected static class RequestParam extends ArrayList<NameValuePair> {
		private static final long serialVersionUID = 1L;
		protected JSONObject param;
		protected Context mContext;
		protected String mApi;
		protected String mHost;

		public RequestParam(Context context, String host, String api) {
			mContext = context;
			param = new JSONObject();
			mApi = api;
			mHost = host;
			addNameValuePair(PARAM_SERVER_API, api.toString());
			putParam("imei", PackageUtils.getIMEI(context));
			putParam("imsi", PackageUtils.getIMSI(context));
			putParam("version", PackageUtils.getVersionCode(context));
			putParam("app_name", "android_" + mContext.getPackageName());
			putParam("channel", PackageUtils.getUmengChannel(context));
		}

		protected void addNameValuePair(String key, String value) {
			add(new BasicNameValuePair(key, value));
		}

		public void setParams(JSONObject job) {
			param = job;
		}

		public void setCompress(boolean b) {
			putParam(PARAM_COMPRESS, b ? 1 : 0);
		}

		public boolean isCompress() {
			try {
				if (!param.has(PARAM_COMPRESS))
					return false;
				return param.getInt(PARAM_COMPRESS) == 1;
			} catch (JSONException e) {
				return false;
			}
		}

		public void setLocation(Location loc) {
			if (loc != null) {
				putParam("latitude", loc.getLatitude());
				putParam("longitude", loc.getLongitude());
			}
		}

		public void putParam(String key, Object value) {
			try {
				param.put(key, value);
			} catch (JSONException e) {
				LogUtils.e(e);
			}
		}

		public void putParamNotNull(String key, Object value) {
			if (value == null)
				return;
			putParam(key, value);
		}

		public RequestParam val() {
			add(new BasicNameValuePair(TAG_PARAM, param.toString()));
			return this;
		}

		public String getApi() {
			return mApi;
		}

		public String getHost() {
			return mHost;
		}

		@Override
		public String toString() {
			String api = getApi();
			String param = "";
			for (int i = 1; i < size(); i++) {
				BasicNameValuePair pair = ((BasicNameValuePair) get(i));
				param += pair.getName() + ":" + pair.getValue() + ", ";
			}
			return api + ": " + param;
		}
	}

}
