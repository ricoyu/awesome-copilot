package com.copilot.common.lang.utils;

/**
 * <p>
 * Copyright: (C), 2020/5/21 15:38
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class Assert {
	
	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}
	
	public static void notEmpty(String object, String message) {
		if (object == null || object.trim().equals("")) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void isTrue(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void hasText(String text, String message) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void notEmpty(Object[] array, String message) {
		boolean isEmpty = array == null || array.length == 0;
		if (isEmpty) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void hasLength(String text, String message) {
		boolean hasLength = text != null && !text.isEmpty();
		if (!hasLength) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void state(boolean expression, String message) {
		if (!expression) {
			throw new IllegalStateException(message);
		}
	}
}
