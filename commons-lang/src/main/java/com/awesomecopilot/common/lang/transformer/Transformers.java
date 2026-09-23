package com.awesomecopilot.common.lang.transformer;

import com.awesomecopilot.common.lang.exception.NoSuitableValueHandlerException;

import java.util.Objects;

import static java.text.MessageFormat.format;

/**
 * 负责数据类型转换
 * <p>
 * Copyright: (C), 2020-12-05 10:22
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class Transformers {
	
	/**
	 * 将value转成targetType类型, 如果value为null则返回null, 如果无法转换的话抛IllegalArgumentException
	 *
	 * @param value
	 * @param targetType
	 * @param <T>
	 * @return
	 */
	public static <T> T convert(Object value, Class<T> targetType) {
		if (value == null) {
			return null;
		}
		Objects.requireNonNull(targetType, "targetType cannot be null!");
		
		ValueHandlerFactory.ValueHandler valueHandler = ValueHandlerFactory.determineAppropriateHandler(targetType);
		if (valueHandler == null) {
			//P2-27(CODE_REVIEW_REPORT): 修复前模板写 {3} 但只传 3 个参数(下标 0~2)——
			//实测异常文本以字面量 "to expected type[{3}]" 结尾, 真正要转的目标类型没打出来。
			String msg = format("Cannot transform value[{0}] of type[{1}] to expected type[{2}]", value, value.getClass(), targetType);
			throw new NoSuitableValueHandlerException(msg);
		}
		return (T) valueHandler.convert(value);
	}
	
	/**
	 * 转成字符串类型
	 * @param value
	 * @return String
	 */
	public static String toString(Object value) {
		if (value == null) {
			return null;
		}
		
		ValueHandlerFactory.ValueHandler<String> valueHandler = ValueHandlerFactory.StringValueHandler.INSTANCE;
		return valueHandler.convert(value);
	}
}
