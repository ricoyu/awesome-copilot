package com.awesomecopilot.cloud.sentinel.utils;


import com.awesomecopilot.common.lang.utils.ReflectionUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * <p>
 * Copyright: (C), 2022-08-25 20:59
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class BlockExceptionUtils {
	
	public static String responseBody(Throwable e) {
		Object responseBody = ReflectionUtils.getFieldValue("responseBody", e);
		if (responseBody != null) {
			byte[] data = (byte[])responseBody;
			return new String(data, UTF_8);
		}
		return null;
	}
}
