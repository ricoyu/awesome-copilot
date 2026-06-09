package com.awesomecopilot.search8x.exception;

/**
 * PUT IndexTemplate 失败时抛出该异常
 * <p>
 * Copyright: (C), 2021-06-30 11:49
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class IndexTemplateException extends RuntimeException {
	
	public IndexTemplateException() {
	}
	
	public IndexTemplateException(String message) {
		super(message);
	}
	
	public IndexTemplateException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public IndexTemplateException(Throwable cause) {
		super(cause);
	}
	
	public IndexTemplateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}