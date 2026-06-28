package com.awesomecopilot.codec.exception;

/**
 * DES 解密时抛出异常
 * <p>
 * Copyright: Copyright (c) 2018-08-20 17:09
 * <p>
 * Company: DataSense
 * <p>
 * @author Rico Yu	ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public class DESDecryptionException extends RuntimeException{

	private static final long serialVersionUID = 1178059898844962695L;

	public DESDecryptionException() {
		super();
	}

	public DESDecryptionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DESDecryptionException(String message, Throwable cause) {
		super(message, cause);
	}

	public DESDecryptionException(String message) {
		super(message);
	}

	public DESDecryptionException(Throwable cause) {
		super(cause);
	}

	
}
