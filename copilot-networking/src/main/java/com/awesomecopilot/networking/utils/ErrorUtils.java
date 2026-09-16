package com.awesomecopilot.networking.utils;

import com.awesomecopilot.common.lang.errors.ErrorTypes;
import com.awesomecopilot.common.lang.exception.BusinessException;
import com.awesomecopilot.networking.exception.HttpRequestException;

/**
 * HTTP 响应状态码检查
 * <p>
 * Copyright: (C), 2021-03-16
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ErrorUtils {

	private ErrorUtils() {
	}

	/**
	 * 检查响应状态码, 非 2xx 一律抛异常（评审报告 P0-2 修复）。
	 * <p>
	 * 旧实现只有一个只认识 400/405/500 三个码的 switch, 401/403/404/429/502 这些
	 * 同样代表"请求没成功"的状态码落到 switch 结尾什么都不做, 错误响应体会被当成
	 * 正常结果返回给调用方（实测: 404 的 {"error":"not found"} 原样返回）。
	 * <p>
	 * 现在的规则:
	 * <ul>
	 *     <li/>2xx(200~299): 成功, 放行。204 No Content、206 分段下载都是合法成功,
	 *     旧实现只认 200 会把它们误报为异常, 这里一并改为区间判断。
	 *     <li/>400/404/405/429/500: 抛 BusinessException, 带上语义最接近的 ErrorTypes
	 *     错误码（404→NOT_FOUND 等）, 供上层按业务异常分支处理。
	 *     <li/>其余状态码(401/403/409/502/503...): ErrorTypes 里没有一一对应的枚举值,
	 *     为不虚构错误码语义, 统一抛 HttpRequestException, 消息里带状态码和原因短语。
	 * </ul>
	 * <p>
	 * ⚠️ 行为变化（调用方需知）: 以前拿 401/404 等状态码还能正常拿到响应体字符串的调用
	 * （例如直接读 ES 404 响应 JSON 判断文档不存在）, 现在会收到异常, 需要用
	 * catch (BusinessException e) 或 catch (HttpRequestException e) 接住, 或改用 onError 回调。
	 *
	 * @param statusCode HTTP 响应状态码
	 * @param reason     状态码对应的原因短语(如 "Not Found")
	 */
	public static void checkError(int statusCode, String reason) {
		if (statusCode >= 200 && statusCode < 300) {
			return;
		}
		switch (statusCode) {
			case 400:
				throw new BusinessException(ErrorTypes.BAD_REQUEST, reason);
			case 404:
				throw new BusinessException(ErrorTypes.NOT_FOUND, reason);
			case 405:
				throw new BusinessException(ErrorTypes.METHOD_NOT_ALLOWED, reason);
			case 429:
				throw new BusinessException(ErrorTypes.TOO_MANY_REQUESTS, reason);
			case 500:
				throw new BusinessException(ErrorTypes.INTERNAL_SERVER_ERROR, reason);
			default:
				// 401/403/409/502/503 等: ErrorTypes 没有语义对应的枚举, 不硬凑, 直接带码抛出
				throw new HttpRequestException(
						"HTTP请求失败, 状态码: " + statusCode + (reason == null ? "" : ", 原因: " + reason));
		}
	}
}
