package com.awesomecopilot.common.lang.exception;


import com.awesomecopilot.common.lang.errors.ErrorType;

/**
 * <p>
 * Copyright: (C), 2020-08-17 17:37
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ApplicationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	
	private String code = "500";
	
	private String message = "Internal Server Error";
	
	public ApplicationException() {
		super();
	}
	
	public ApplicationException(ErrorType errorType) {
		super(errorType.message());
		this.code = errorType.code();
		this.message = errorType.message();
	}
	
	public ApplicationException(String code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}
	
	public ApplicationException(String message) {
		super(message);
		this.message = message;
	}
	
	public ApplicationException(String message, Throwable cause) {
		super(message, cause);
		this.message = message;
	}
	
	public ApplicationException(Throwable cause) {
		//P1-21: 原来只 super(cause), 被子类 message 字段(初始值 "Internal Server Error")
		//和 getMessage() 覆盖挡住, 被包裹异常的真实原因只能翻堆栈
		super(cause);
		//评审修复(2026-09-23): NullPointerException 等无消息 cause 的 getMessage() 返回 null,
		//直接赋值会让本类 getMessage() 仍是 null(正是 P1-21 要消除的 NPE 形态), 无消息时改用 cause.toString() 保留类型信息
		if (cause == null) {
			this.message = "Internal Server Error";
		} else {
			String causeMessage = cause.getMessage();
			this.message = causeMessage != null ? causeMessage : cause.toString();
		}
	}
	
	protected ApplicationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.message = message;
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
