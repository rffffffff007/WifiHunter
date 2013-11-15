package cn.buding.android.common.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Base64;
import cn.buding.android.common.log.LogTag;
import cn.buding.android.common.log.LogUtils;

/**
 * utils for handle network operation.
 */
public class NetUtil {
	private static final String CONTENT_CHARSET = "UTF-8";

	/**
	 * is at least one network connected
	 * 
	 * @return
	 */
	public static boolean isNetworkAvailable(Context ctx) {
		if (ctx == null)
			return false;
		ConnectivityManager cm =
				(ConnectivityManager) ctx
						.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();

		return (info != null && info.isConnected());
	}

	/**
	 * is current connected network is wifi
	 * 
	 * @return
	 */
	public static boolean isNetworkWifi(Context ctx) {
		if (ctx == null)
			return false;
		ConnectivityManager cm =
				(ConnectivityManager) ctx
						.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();

		return (info != null && info.getType() == ConnectivityManager.TYPE_WIFI);
	}

	/**
	 * is current connected network is cmwap
	 * 
	 * @return
	 */
	public static boolean isNetworkCmwap(Context ctx) {
		// 以下为支持cmwap部分，移动和联通的cmwap代理貌似都是10.0.0.172，有待测试
		String proxyHost = android.net.Proxy.getDefaultHost();
		// 判断网络状况，如果有其它通路的话就不要再使用cmwap的代理
		if (proxyHost != null && !isNetworkWifi(ctx) && onlyOneNetWork(ctx)) {
			ConnectivityManager manager =
					(ConnectivityManager) ctx
							.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = manager.getActiveNetworkInfo();
			String extraInfo = null;
			if (info != null)
				extraInfo = info.getExtraInfo();
			if (extraInfo != null) {
				String extraTrim = extraInfo.trim().toLowerCase();
				if (extraTrim.equals("cmwap") || extraTrim.equals("uniwap")
						|| extraTrim.equals("ctnet")
						|| extraTrim.equals("ctwap"))
					return true;
			}
		}
		return false;
	}

	/**
	 * whether there is only one network connected. {@link ConnectivityManager#TYPE_WIFI}
	 * {@link ConnectivityManager#TYPE_MOBILE}
	 * 
	 * @param ctx
	 * @return
	 */
	public static boolean onlyOneNetWork(Context ctx) {
		if (ctx == null)
			return false;
		ConnectivityManager manager =
				(ConnectivityManager) ctx
						.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (manager == null)
			return false;
		NetworkInfo[] infos = manager.getAllNetworkInfo();
		int count = 0;
		if (infos == null)
			return false;
		for (NetworkInfo info : infos)
			if (info.isConnected())
				count++;
		return count == 1;
	}

	/**
	 * create a connection from a url. and add proxy if network is cmwap
	 * 
	 * @param mUrl the url to build connection
	 * @return the connection of mUrl
	 * @throws IOException
	 */
	public static synchronized HttpURLConnection getHttpUrlConnection(
			Context mContext, URL mUrl) throws IOException {
		HttpURLConnection conn = null;
		if (NetUtil.isNetworkCmwap(mContext)) {
			java.net.Proxy p =
					new java.net.Proxy(java.net.Proxy.Type.HTTP,
							new InetSocketAddress(android.net.Proxy
									.getDefaultHost(), android.net.Proxy
									.getDefaultPort()));
			conn = (HttpURLConnection) mUrl.openConnection(p);
		} else
			conn = (HttpURLConnection) mUrl.openConnection();
		return conn;
	}

	/**
	 * return current ip address
	 * 
	 * @return
	 */
	public static String getIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en =
					NetworkInterface.getNetworkInterfaces(); en
					.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr =
						intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String ipaddress =
								inetAddress.getHostAddress().toString();
						return ipaddress;
					}
				}
			}
		} catch (SocketException ex) {
			LogUtils.e("Socket exception in GetIP Address of Utilities", ex);
		}
		return null;
	}

	/**
	 * return the wifi address of the cellphone
	 * 
	 * @return
	 */
	public static String getWifiMacAddress(Context context) {
		WifiManager wifimanager =
				(WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiinfo = wifimanager.getConnectionInfo();
		if (wifiinfo != null && wifiinfo.getMacAddress() != null
				&& wifiinfo.getMacAddress().length() > 0)
			return wifiinfo.getMacAddress();
		return null;
	}

	/**
	 * this part is can not be compiled in 1.5, so we just return null currently.
	 * 
	 * @return
	 */
	public static String getBluetoothMacAddress(Context context) {
		// BluetoothAdapter bluetoothDefaultAdapter = BluetoothAdapter.getDefaultAdapter();
		// String mac = null;
		// if ((bluetoothDefaultAdapter != null) && (bluetoothDefaultAdapter.isEnabled()))
		// mac = BluetoothAdapter.getDefaultAdapter().getAddress();
		// if(mac != null && mac.length() > 0)
		// return mac;
		return null;
	}

	/**
	 * decode the api response.
	 * first decode by {@link Base64},
	 * then decode by {@link Deflater}
	 * 
	 * @param encodeStr
	 * @return
	 */
	public static String decodePostApi(String encodeStr) {
		if(encodeStr == null)
			return null;
		byte[] strBytes;
		String decodeStr = null;
		try {
			strBytes =
					Base64.decode(encodeStr.getBytes(CONTENT_CHARSET),
							Base64.DEFAULT);
			strBytes = decompress(strBytes);
			decodeStr = new String(strBytes, CONTENT_CHARSET);
		} catch (Exception e) {
			LogUtils.e("Error in decode", e);
		}

		return decodeStr;
	}

	/**
	 * compress file to outputFile
	 * 
	 * @param file
	 * @param outputFile
	 * @throws IOException
	 */
	public static void compress(File file, File outputFile) throws IOException {
		byte[] bs = compress(file);
		BufferedOutputStream writer =
				new BufferedOutputStream(new FileOutputStream(outputFile));
		writer.write(bs);
		writer.flush();
	}

	/**
	 * compress a file to byte array
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static byte[] compress(File file) throws IOException {
		byte[] bytes = new byte[(int) file.length()];
		BufferedInputStream in =
				new BufferedInputStream(new FileInputStream(file));
		int len = in.read(bytes);
		in.close();
		return compress(bytes, len);
	}

	/**
	 * compress a byte array to a new byte array
	 * 
	 * @param data
	 * @return
	 */
	public static byte[] compress(byte[] data) {
		return compress(data, data.length);
	}

	/**
	 * compress a byte array to a new byte array by {@link Deflater}
	 * 
	 * @param data input data to compress
	 * @param len the 0..length in data is for compress
	 * @return compressed byte array
	 */
	public static byte[] compress(byte[] data, int len) {
		byte[] output = new byte[0];
		Deflater compresser = new Deflater();
		compresser.reset();
		compresser.setInput(data, 0, len);
		compresser.finish();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(len);
		try {
			byte[] buf = new byte[1024];
			while (!compresser.finished()) {
				int i = compresser.deflate(buf);
				bos.write(buf, 0, i);
			}
			output = bos.toByteArray();
		} catch (Exception e) {
			output = data;
			e.printStackTrace();
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		compresser.end();
		return output;
	}

	public static byte[] decompress(byte[] data) {
		return decompress(data, data.length);
	}

	/**
	 * decompress 0..len in data
	 * 
	 * @param data
	 * @param len length of data for decompress in data[]
	 * @return
	 */
	public static byte[] decompress(byte[] data, int len) {
		byte[] output = new byte[0];

		Inflater decompresser = new Inflater();
		decompresser.reset();
		decompresser.setInput(data, 0, len);

		ByteArrayOutputStream o = new ByteArrayOutputStream(len);
		try {
			byte[] buf = new byte[1024];
			while (!decompresser.finished()) {
				int i = decompresser.inflate(buf);
				o.write(buf, 0, i);
			}
			output = o.toByteArray();
		} catch (Exception e) {
			output = data;
			e.printStackTrace();
		} finally {
			try {
				o.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		decompresser.end();
		return output;
	}

}
