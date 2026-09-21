package com.awesomecopilot.web.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WebUtils.bodyString 测试（评审报告 P2-10 关联 P1-1）。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class WebUtilsTest {

	@Test
	public void testBodyStringPreservesTabs() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContent("a\tb\tc".getBytes(StandardCharsets.UTF_8));

		String body = WebUtils.bodyString(request);

		// 修复前: 返回值末尾的 replaceAll("\r|\n|\t","") 会把制表符也删掉, 得到 "abc";
		// readLine 本来就不会产出 \r\n, 这个 replaceAll 唯一的实际作用就是破坏制表符
		assertEquals("a\tb\tc", body);
	}

	@Test
	public void testBodyStringJoinsLinesWithoutInventedSeparators() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContent("line1\nline2".getBytes(StandardCharsets.UTF_8));

		// 保持既有行为: 逐行拼接不加分隔符(有下游按这个约定做签名比对), 这里固化现状防顺手改动
		assertEquals("line1line2", WebUtils.bodyString(request));
	}
}
