package com.awesomecopilot.common.lang.errors;

/**
 * 标准错误对象接口, 建议将各种错误代码以实现本接口的enum形式提供
 * <p>
 * Copyright: Copyright (c) 2020-05-02 10:26
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public interface ErrorType {
	
	/**
	 * 错误码值。<p>
	 * 码值是对外响应契约: ResultSerializer 以 "0" 判定成功, 前端与测试断言 "4002"/"4041"/"5001" 等现有值,
	 * 因此内置 {@link ErrorTypes} 的码值长度不一且保持不变: "0"(成功), "4002"(参数错误), "4041", "5001"(内部错误)等。<p>
	 * 本接口对实现类(含 enum 实现)不做任何格式校验; 只有继承 {@link AbstractErrorType} 的类
	 * 由构造器强制 8 位格式: 前3位状态码类别 + 2位模块代码 + 3位自定义错误码。
	 *
	 * @return String
	 */
	String code();
	
	/**
	 * 设置默认消息
	 *
	 * @param message
	 */
	default void message(String message) {
	}
	
	/**
	 * 返回默认消息
	 *
	 * @return
	 */
	default String message() {
		return null;
	}
	
	/**
	 * 设置国际化消息模板
	 * @param msgTemplate
	 */
	default void msgTemplate(String msgTemplate) {
	}
	
	/**
	 * 返回国际化消息模板
	 *
	 * @return
	 */
	default String msgTemplate() {
		return null;
	}
	
}
