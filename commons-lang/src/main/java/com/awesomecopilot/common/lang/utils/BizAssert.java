package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.exception.BusinessException;

import java.util.Collection;

/**
 * 业务逻辑上的断言工具类, 一般断言失败都抛出BusinessException,
 * 这样可以保证被统一的异常处理
 * <p>
 * Copyright: (C), 2020/5/21 15:38
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class BizAssert {
	
	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new BusinessException(message);
		}
	}
	
	public static void notEmpty(String object, String message) {
		if (object == null || object.trim().equals("")) {
			throw new BusinessException(message);
		}
	}

	public static void isTrue(boolean expression, String message) {
		if (!expression) {
			throw new BusinessException(message);
		}
	}

	public static void hasText(String text, String message) {
		if (!StringUtils.hasText(text)) {
			throw new BusinessException(message);
		}
	}

	public static void notEmpty(Object[] array, String message) {
		boolean isEmpty = array == null || array.length == 0;
		if (isEmpty) {
			throw new BusinessException(message);
		}
	}

	public static void notEmpty(Collection list, String message) {
		boolean isEmpty = list == null || list.size() == 0;
		if (isEmpty) {
			throw new BusinessException(message);
		}
	}

	public static void hasLength(String text, String message) {
		boolean hasLength = text != null && !text.isEmpty();
		if (!hasLength) {
			throw new BusinessException(message);
		}
	}

	public static void state(boolean expression, String message) {
		if (!expression) {
			throw new BusinessException(message);
		}
	}
}
