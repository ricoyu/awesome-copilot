package com.awesomecopilot.search8x.exception;

/**
 * 设置Mapping失败时抛出该异常
 * <p>
 * Copyright: (C), 2021-06-30 11:49
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class PutMappingException extends RuntimeException {
	
	public PutMappingException() {
	}
	
	public PutMappingException(String message) {
		super(message);
	}
	
	public PutMappingException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public PutMappingException(Throwable cause) {
		super(cause);
	}
	
	public PutMappingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}