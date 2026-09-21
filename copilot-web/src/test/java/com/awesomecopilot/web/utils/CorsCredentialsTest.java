package com.awesomecopilot.web.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CORS 跨域响应头构建测试（评审报告 P2-7：带凭证的跨域支持）。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CorsCredentialsTest {

	/**
	 * 用 JDK 动态代理记录 setHeader / addHeader / setCharacterEncoding 的调用值，不引入 mock 依赖
	 */
	private static class Recording {
		final Map<String, String> headers = new HashMap<>();

		HttpServletResponse response() {
			return (HttpServletResponse) Proxy.newProxyInstance(
					CorsCredentialsTest.class.getClassLoader(),
					new Class<?>[]{HttpServletResponse.class},
					(proxy, method, args) -> {
						switch (method.getName()) {
							case "setHeader":
							case "addHeader":
								headers.put((String) args[0], (String) args[1]);
								return null;
							case "getHeader":
								return headers.get(args[0]);
							default:
								Class<?> rt = method.getReturnType();
								if (rt == boolean.class) return false;
								if (rt == int.class) return 0;
								return null;
						}
					});
		}
	}

	@Test
	public void testAllowCredentialsWithConcreteOriginWritesCredentialsAndVary() {
		Recording recording = new Recording();
		CORS.builder()
				.allowedOrigins("https://app.example.com")
				.allowedMethods("GET", "POST")
				.allowedHeaders("Content-Type")
				.allowCredentials(true)
				.build(recording.response());

		assertEquals("https://app.example.com", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("true", recording.headers.get("Access-Control-Allow-Credentials"));
		assertEquals("Origin", recording.headers.get("Vary"), "回显具体Origin时必须加 Vary: Origin 防缓存串用");
	}

	@Test
	public void testAllowAllIgnoresCredentialsBecauseWildcardConflicts() {
		Recording recording = new Recording();
		// 浏览器规则: Allow-Origin:* 与 Allow-Credentials:true 不能共存, 配置了也要拒绝写出
		CORS.builder().allowAll().allowCredentials(true).build(recording.response());

		assertEquals("*", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertNull(recording.headers.get("Access-Control-Allow-Credentials"),
				"通配符 Origin 下不允许写出 Allow-Credentials: true");
	}

	@Test
	public void testNoCredentialsByDefault() {
		Recording recording = new Recording();
		CORS.builder().allowedOrigins("https://a.com").build(recording.response());

		assertNull(recording.headers.get("Access-Control-Allow-Credentials"));
		assertNull(recording.headers.get("Vary"));
	}

	/**
	 * 独立评审 S-3: Access-Control-Allow-Origin 协议上只允许单值或 *;
	 * 配了 2 个具体 Origin 时输出的 "a, b" 本就非法(旧有问题, 本次不改变三个基础头的输出),
	 * 但 allowCredentials 分支不能再给一个浏览器必然拒绝的响应写 Credentials:true——
	 * 只允许恰好 1 个非 * Origin 时启用凭证。
	 */
	@Test
	public void testMultipleConcreteOriginsDoNotGetCredentialsHeader() {
		Recording recording = new Recording();
		CORS.builder()
				.allowedOrigins("https://a.com", "https://b.com")
				.allowCredentials(true)
				.build(recording.response());

		assertEquals("https://a.com, https://b.com", recording.headers.get(CORS.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertNull(recording.headers.get("Access-Control-Allow-Credentials"),
				"多 Origin 列表浏览器必然拒绝, 不得宣称凭证可用");
	}
}
