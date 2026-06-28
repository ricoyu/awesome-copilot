package com.awesomecopilot.codec.exception;

/**
 * DES 加密失败异常
 * <p>
 * Copyright: Copyright (c) 2018-08-20 17:06
 * <p>
 * Company: DataSense
 * <p>
 * @author Rico Yu	ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public class DESEncryptionException extends RuntimeException {

	private static final long serialVersionUID = -7574574381366212189L;

	public DESEncryptionException() {
		super();
	}

	public DESEncryptionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DESEncryptionException(String message, Throwable cause) {
		super(message, cause);
	}

	public DESEncryptionException(String message) {
		super(message);
	}

	public DESEncryptionException(Throwable cause) {
		super(cause);
	}

}
