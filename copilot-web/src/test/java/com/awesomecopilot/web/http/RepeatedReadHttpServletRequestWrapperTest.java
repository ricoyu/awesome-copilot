package com.awesomecopilot.web.http;

import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可重复读请求包装器的回归测试。
 * <p>
 * 评审报告 P1-1：旧实现用 WebUtils.bodyString 缓存请求体，该方法按行读取后删除
 * 回车/换行/制表符，导致字段值含真实换行的 JSON 在二次读取时和客户端原始字节不一致。
 * 修复后 body 必须按原始字节缓存，读取两次结果与原文完全相同。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RepeatedReadHttpServletRequestWrapperTest {

	private static final String MULTILINE_JSON =
			"{\"log\":\"first line\nsecond line\r\nthird\tend\"}";

	@Test
	public void testMultilineBodyIsByteIdenticalAfterReread() throws IOException {
		byte[] original = MULTILINE_JSON.getBytes(StandardCharsets.UTF_8);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/json");
		request.setContent(original);

		RepeatedReadHttpServletRequestWarpper wrapper = new RepeatedReadHttpServletRequestWarpper(request);

		// 第一次读
		byte[] first = wrapper.getInputStream().readAllBytes();
		// 第二次读（模拟过滤器读过之后 Controller 再读）
		byte[] second = wrapper.getInputStream().readAllBytes();

		assertArrayEquals(original, first, "换行/制表符不允许在缓存请求体时丢失");
		assertArrayEquals(original, second, "重复读取必须返回同样的字节");
	}

	@Test
	public void testGetReaderPreservesLineBreaks() throws IOException {
		byte[] original = MULTILINE_JSON.getBytes(StandardCharsets.UTF_8);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/json");
		request.setContent(original);

		RepeatedReadHttpServletRequestWarpper wrapper = new RepeatedReadHttpServletRequestWarpper(request);

		// getReader 与 getInputStream 共用同一份字节缓存, 二次读取内容必须与原文一致
		try (var reader = wrapper.getReader()) {
			StringBuilder sb = new StringBuilder();
			char[] buf = new char[64];
			int n;
			while ((n = reader.read(buf)) != -1) {
				sb.append(buf, 0, n);
			}
			assertEquals(MULTILINE_JSON, sb.toString());
		}
	}

	@Test
	public void testIsFinishedReflectsStreamExhaustion() throws IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/json");
		request.setContent("{\"a\":1}".getBytes(StandardCharsets.UTF_8));

		RepeatedReadHttpServletRequestWarpper wrapper = new RepeatedReadHttpServletRequestWarpper(request);

		ServletInputStream in = wrapper.getInputStream();
		assertTrue(in.isReady(), "内存流随时可读, isReady 必须为 true");
		assertTrue(!in.isFinished(), "读取前应未结束");
		in.readAllBytes();
		assertTrue(in.isFinished(), "读完之后 isFinished 必须为 true");
	}

	@Test
	public void testMultipartBodyIsNotBuffered() throws IOException {
		// multipart/form-data 是文件上传, 全量缓存进内存既无意义又危险, wrapper 应放行原始流
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("multipart/form-data; boundary=----abc");
		request.setContent("file-bytes".getBytes(StandardCharsets.UTF_8));

		RepeatedReadHttpServletRequestWarpper wrapper = new RepeatedReadHttpServletRequestWarpper(request);

		// body 未缓存时 getInputStream 委托原始 request, 内容仍可读
		byte[] read = wrapper.getInputStream().readAllBytes();
		assertArrayEquals("file-bytes".getBytes(StandardCharsets.UTF_8), read);
	}
}
