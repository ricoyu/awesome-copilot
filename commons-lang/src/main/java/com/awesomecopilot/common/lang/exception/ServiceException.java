package com.awesomecopilot.common.lang.exception;

import com.awesomecopilot.common.lang.errors.ErrorType;
import com.awesomecopilot.common.lang.errors.ErrorTypes;

/**
 * 通用服务调用异常
 * <p>
 * Copyright: (C), 2020/5/17 19:11
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ServiceException extends RuntimeException {

	private String code;

	private String message;

	public ServiceException() {
	}

	public ServiceException(String message) {
		ErrorType errorType = ErrorTypes.FAIL;
		this.code = errorType.code();
		this.message = message;
	}

	public ServiceException(ErrorType errorType) {
		super(errorType.message());
		this.code = errorType.code();
		this.message = errorType.message();
	}

	public ServiceException(String code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}

	public ServiceException(String code, String messageTemplate, String defaultMesssage) {
		this.code = code;
		this.message = defaultMesssage;
	}

	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

}
