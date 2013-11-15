package cn.buding.android.common.log;

/**
 * this file contain all tag constant varibles
 */
public enum LogTag {
	LOCATION("[LOCATION]"),

	HTTP("[HTTP]"),

	REQUEST("[REQUEST]"),

	RESPONSE("[RESPONSE]"),

	GLOBALVALUE("[GLOBALVALUE]"),

	PROPERTY("[PROPERTY]"),

	JSONPARSER("[JSONPARSER]"),

	THREAD("[THREAD]"),

	IO("[IO]"),

	TASK("[TASK]"),

	ACTIVITY("[ACTIVITY]"),

	INTENT("[INTENT]"),

	SERVICE("[SERVICE]"),

	MESSAGE("[MESSAGE]"),

	DB("[DB]"),

	SENSOR("[SENSOR]"),

	BEHAVIOR("[BEHAVIOR]"),

	LOGDATA("[LOGDATA]"),

	DEBUG("[DEBUG]"),

	ERROR("[ERROR]"),

	CUSTOM("[CUSTOM]");

	private String s;

	LogTag(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
