package cn.buding.android.common.location.google;

import java.io.Serializable;

public class JGoogleAddressComponent implements Serializable {
	private static final long serialVersionUID = 1L;
	public String long_name;
	public String[] types;

	public String getFirstType() {
		if (types != null && types.length > 0)
			return types[0];
		return null;
	}
}
