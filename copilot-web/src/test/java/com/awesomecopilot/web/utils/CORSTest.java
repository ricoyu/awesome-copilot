package com.awesomecopilot.web.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CORS 构建器生成的响应头测试。
 * <p>
 * 回归锚点（评审报告 P0-1）：allowAll() 之后 build() 写出的三个 Access-Control-* 头
 * 必须是 "*"。曾因 commons-lang StringUtils.joinWith 把 List 当成单个元素拼接，
 * 实际发出去的值是 "[*]"，浏览器不识别，跨域配置形同虚设。
 * <p>
 * Copyright: Copyright (c) 2019-10-14 17:22
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CORSTest {

	/**
	 * 用 JDK 动态代理记录 setHeader / setCharacterEncoding 的调用值，不引入 mock 依赖
	 */
	private static class Recording {
		final Map<String, String> headers = new HashMap<>();
		String characterEncoding;
	}

	private static HttpServletResponse response(Recording recording) {
		return (HttpServletResponse) Proxy.newProxyInstance(
				CORSTest.class.getClassLoader(),
				new Class<?>[]{HttpServletResponse.class},
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "setHeader":
							recording.headers.put((String) args[0], (String) args[1]);
							return null;
						case "setCharacterEncoding":
							recording.characterEncoding = (String) args[0];
							return null;
						default:
							return defaultValue(method.getReturnType());
					}
				});
	}

	private static Object defaultValue(Class<?> type) {
		if (type == boolean.class) return false;
		if (type == int.class) return 0;
		return null;
	}

	@Test
	public void testAllowAllProducesWildcardHeaders() {
		Recording recording = new Recording();
		CORS.builder().allowAll().build(response(recording));

		assertEquals("*", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("*", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_HEADERS));
	}

	@Test
	public void testMultipleValuesJoinedWithComma() {
		Recording recording = new Recording();
		CORS.builder()
				.allowedOrigins("http://a.com", " http://b.com ")
				.allowedMethods("GET", "POST")
				.allowedHeaders("Content-Type", "X-Token")
				.build(response(recording));

		// trim 后逐元素用 ", " 拼接，不允许出现方括号
		assertEquals("http://a.com, http://b.com",
				recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("GET, POST",
				recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("Content-Type, X-Token",
				recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_HEADERS));
	}

	@Test
	public void testBlankValuesIgnored() {
		Recording recording = new Recording();
		CORS.builder()
				.allowedOrigins("http://a.com", null, "  ")
				.allowedMethods("", "GET")
				.allowedHeaders((String) null)
				.build(response(recording));

		assertEquals("http://a.com", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("GET", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_METHODS));
		// allowedHeaders 一条都没加上时拼出空串（保持既有行为）
		assertEquals("", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_HEADERS));
	}

	@Test
	public void testBuildSetsUtf8CharacterEncoding() {
		Recording recording = new Recording();
		CORS.builder().allowAll().build(response(recording));
		assertEquals("UTF-8", recording.characterEncoding);
	}
}
