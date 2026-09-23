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

	//评审修复(2026-09-23): 本批新增 msgTemplate 字段使计算 UID 变化; 显式声明防止未来再加字段再次漂移
	private static final long serialVersionUID = 3645985928446181182L;

	private String code;

	private String message;
	
	/**
	 * P1-21: 原来三参构造(code, messageTemplate, defaultMesssage)里的 messageTemplate
	 * 参数无人接收、也没有 getter, 与 BusinessException 同名构造器行为不一致。补上字段与 getter。
	 */
	private String msgTemplate;

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
		super(defaultMesssage);
		this.code = code;
		this.msgTemplate = messageTemplate;
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
	
	public String getMsgTemplate() {
		return msgTemplate;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
