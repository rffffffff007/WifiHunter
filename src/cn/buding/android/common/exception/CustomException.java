package cn.buding.android.common.exception;

public class CustomException extends Exception{
	private static final long serialVersionUID = 6839324101177543912L;
	private int code;
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public CustomException(String msg, Throwable cause) {
		super(msg, cause);
		code = ECode.FAIL;
	}
	
	public CustomException(Throwable cause) {
		super(cause.getMessage(), cause);
		code = ECode.FAIL;
	}
	
	public CustomException(int code, String msg, Throwable cause) {
		super(msg, cause);
		
		this.code = code;
	}
	
	public CustomException(String msg) {
		super(msg);
		this.code = ECode.FAIL;
	}

	public CustomException(int code) {
		super("");
		this.code = code;
	}

	public CustomException(int code, Throwable e) {
		super(e.getMessage(), e);
		this.code = code;
	}

	public CustomException(int code, String msg) {
		super(msg);
		this.code = code;
	}
}
