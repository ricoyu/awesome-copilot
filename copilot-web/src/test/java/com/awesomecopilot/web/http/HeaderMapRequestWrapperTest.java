package com.awesomecopilot.web.http;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HeaderMapRequestWrapper 测试（评审报告 P2-9）。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@SuppressWarnings("deprecation")
public class HeaderMapRequestWrapperTest {

	@Test
	public void testAddHeaderOverridesAndGetHeaderSeesNewValue() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("tenant", "old");
		HeaderMapRequestWrapper wrapper = new HeaderMapRequestWrapper(request);

		wrapper.addHeader("tenant", "new");

		assertEquals("new", wrapper.getHeader("tenant"));
	}

	@Test
	public void testGetHeadersReflectsOverrideNotOldPlusNew() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("tenant", "old");
		HeaderMapRequestWrapper wrapper = new HeaderMapRequestWrapper(request);
		wrapper.addHeader("tenant", "new");

		// 修复前: getHeaders 把原始值和覆盖值拼在一起返回 [old, new],
		// 而 getHeader 只返回 new——同一个头两个读取口给出两个答案
		var values = Collections.list(wrapper.getHeaders("tenant"));
		assertEquals(Collections.singletonList("new"), values,
				"覆盖后所有读取路径都应只看到新值");
	}

	@Test
	public void testGetHeaderNamesHasNoDuplicateOnOverride() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("tenant", "old");
		HeaderMapRequestWrapper wrapper = new HeaderMapRequestWrapper(request);
		wrapper.addHeader("tenant", "new");
		wrapper.addHeader("extra", "e");

		var names = Collections.list(wrapper.getHeaderNames());
		assertEquals(1, Collections.frequency(names, "tenant"), "头名不允许重复出现: " + names);
		assertTrue(names.contains("extra"));
	}

	@Test
	public void testRepeatedAddHeaderLastWins() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		HeaderMapRequestWrapper wrapper = new HeaderMapRequestWrapper(request);

		wrapper.addHeader("k", "1");
		wrapper.addHeader("k", "2");

		assertEquals("2", wrapper.getHeader("k"), "同名多次调用为覆盖语义(javadoc已注明)");
	}

	/**
	 * 独立评审 X-1(评审报告 P2-9 的未闭环点): HTTP 头名大小写不敏感,
	 * 先后用两种拼写写同一个头必须视为一次覆盖, 三个读取口(含第三种拼写)结果确定。
	 * 修复前 headerMap 是大小写敏感的 HashMap, 双拼写会留下两个 key,
	 * getHeader("Tenant")=旧值、getHeader("tenant")=新值, 第三种拼写取决于 HashMap 迭代顺序
	 */
	@Test
	public void testMixedCaseWritesKeepLastValueDeterministicAcrossAllReads() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		HeaderMapRequestWrapper wrapper = new HeaderMapRequestWrapper(request);

		wrapper.setHeader("Tenant", "a");
		wrapper.setHeader("tenant", "b");

		assertEquals("b", wrapper.getHeader("Tenant"), "覆盖后原拼写也必须读到最后一笔值");
		assertEquals("b", wrapper.getHeader("TENANT"), "第三种大小写拼写必须得到确定结果, 不依赖迭代顺序");
		assertEquals(Collections.singletonList("b"), Collections.list(wrapper.getHeaders("tenant")));
		assertEquals(1, Collections.list(wrapper.getHeaderNames()).stream()
				.filter(n -> n.equalsIgnoreCase("tenant")).count(), "头名列表里该头只允许出现一次");
	}
}
