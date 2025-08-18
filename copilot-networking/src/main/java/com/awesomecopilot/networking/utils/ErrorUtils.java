package com.awesomecopilot.networking.utils;

import com.awesomecopilot.common.lang.errors.ErrorTypes;
import com.awesomecopilot.common.lang.exception.BusinessException;

import java.awt.image.RescaleOp;

import static com.awesomecopilot.common.lang.errors.ErrorTypes.METHOD_NOT_ALLOWED;

public final class ErrorUtils {

	public static void checkError(int statusCode, String reason) {
		switch (statusCode) {
			case 200:
				break;
			case 405:
				throw new BusinessException(METHOD_NOT_ALLOWED, reason);
			case 500:
				throw new BusinessException(ErrorTypes.INTERNAL_SERVER_ERROR, reason);
			case 400:
				throw new BusinessException(ErrorTypes.BAD_REQUEST, reason);
		}
	}
}
