package com.awesomecopilot.common.lang.exception;

import com.awesomecopilot.common.lang.errors.ErrorType;
import com.awesomecopilot.common.lang.errors.ErrorTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 通用业务异常
 * <p>
 * Copyright: (C), 2020/5/17 19:11
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class BusinessException extends RuntimeException {
	
	private String code;
	
	private String msgTemplate;
	
	private String message;
	
	private List<Object> messageParams = new ArrayList<>();
	
	public BusinessException() {
	}

	public BusinessException(String message) {
		ErrorType errorType = ErrorTypes.FAIL;
		this.code = errorType.code();
		this.message = message;
	}

	public BusinessException(ErrorType errorType) {
		super(errorType.message());
		this.code = errorType.code();
		this.msgTemplate = errorType.msgTemplate();
		this.message = errorType.message();
	}
	
	public BusinessException(ErrorType errorType, Object... messageParams) {
		super(errorType.message());
		this.code = errorType.code();
		this.msgTemplate = errorType.msgTemplate();
		this.message = errorType.message();
		this.messageParams = Arrays.asList(messageParams);
	}
	
	public BusinessException(String code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}
	
	public BusinessException(String code, String messageTemplate, String defaultMesssage) {
		this.code = code;
		this.msgTemplate = messageTemplate;
		this.message = defaultMesssage;
	}
	
	public BusinessException(String code, String messageTemplate, List<Object> messageParams, String defaultMesssage) {
		//P1-21: 原来从未给 message 赋值(还把 msgTemplate 赋了两遍, 第4个参数整个丢弃),
		//getMessage() 返回 null, 全局异常处理器一 trim 就 NPE
		super(defaultMesssage);
		this.code = code;
		this.msgTemplate = messageTemplate;
		this.messageParams = messageParams;
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
	
	public String getMsgTemplate() {
		return msgTemplate;
	}
	
	public void setMsgTemplate(String msgTemplate) {
		this.msgTemplate = msgTemplate;
	}
	
	public List<Object> getMessageParams() {
		return messageParams;
	}
	
	public void setMessageParams(List<Object> messageParams) {
		this.messageParams = messageParams;
	}
	
	
}
