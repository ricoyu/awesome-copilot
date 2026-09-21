package com.awesomecopilot.web.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TraceFilter 测试（评审报告 P2-5）。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TraceFilterTest {

	@AfterEach
	public void cleanMdc() {
		MDC.clear();
	}

	@Test
	public void testTraceIdPropagatedFromHeader() throws IOException, ServletException {
		TraceFilter filter = new TraceFilter();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("TRACE_ID", "upstream-123");
		Set<String> seenDuringChain = new HashSet<>();

		filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> seenDuringChain.add(MDC.get("traceId")));

		assertTrue(seenDuringChain.contains("upstream-123"), "请求头带来的 traceId 必须在链路内可见");
	}

	@Test
	public void testMdcClearedAfterChainCompletes() throws IOException, ServletException {
		TraceFilter filter = new TraceFilter();
		MockHttpServletRequest request = new MockHttpServletRequest();

		filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> assertNotNull(MDC.get("traceId")));

		// 修复前: doFilter 里没有 try/finally, Tomcat 线程复用时上一个请求的 traceId 会残留
		assertNull(MDC.get("traceId"), "请求结束后必须清理 MDC, 否则线程复用会把 traceId 带到别的请求日志里");
	}

	@Test
	public void testMdcClearedEvenWhenChainThrows() throws IOException {
		TraceFilter filter = new TraceFilter();
		MockHttpServletRequest request = new MockHttpServletRequest();

		try {
			filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
				throw new ServletException("boom");
			});
		} catch (ServletException expected) {
			// 异常照常上抛
		}

		assertNull(MDC.get("traceId"), "链路抛异常时也要清理 MDC");
	}

	@Test
	public void testUpstreamHeaderTakesPrecedenceOverGeneratedId() throws IOException, ServletException {
		// 用固定序列的 seam 验证"透传优先于生成"分支的拼接逻辑, 避免随机性
		TraceFilter filter = new TraceFilter() {
			private final AtomicInteger seq = new AtomicInteger(1);

			@Override
			protected int nextTraceId() {
				return seq.getAndIncrement();
			}
		};
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("TRACE_ID", "  ");  // 全空白应视为未提供

		Set<String> generated = new HashSet<>();
		for (int i = 0; i < 3; i++) {
			filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
					(req, res) -> generated.add(MDC.get("traceId")));
		}

		assertEquals(Set.of("100000001", "100000002", "100000003"), generated,
				"traceId = APP_ID(100000000) + 随机段, 固定序列下必须可精确断言");
	}

	@Test
	public void testGeneratedIdsInExpectedRangeAndLowCollision() throws IOException, ServletException {
		TraceFilter filter = new TraceFilter();
		Set<String> ids = new HashSet<>();
		int total = 1000;

		for (int i = 0; i < total; i++) {
			filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
					(req, res) -> ids.add(MDC.get("traceId")));
		}

		for (String id : ids) {
			assertTrue(id.chars().allMatch(Character::isDigit), "traceId 应为纯数字, 实际=" + id);
			long value = Long.parseLong(id);
			assertTrue(value >= 100_000_000L && value < 101_000_000L,
					"traceId 应落在 APP_ID 前缀区间 [100000000, 101000000), 实际=" + id);
		}
		// 1000 次取值范围在百万槽位, 期望重复约 0.5 个; 阈值放宽到 900 仍能有效抓住"每请求 new Random 同种子短窗口重复"
		// 这类系统性问题(那种情况下会大面积撞号), 又不会因为正常随机波动误报
		assertTrue(ids.size() >= 900,
				"1000 次生成去重后不应大面积相同(实际去重 " + ids.size() + " 个)");
	}

	@Test
	public void testAppIdConstantIsFinal() throws Exception {
		Field f = TraceFilter.class.getDeclaredField("APP_ID");
		f.setAccessible(true);
		assertTrue(Modifier.isFinal(f.getModifiers()),
				"APP_ID 是编译期固定的实例编号前缀, 不允许 volatile 可变(旧字段没有任何写入方却声明 volatile)");
		assertEquals(100_000_000, f.getInt(null));
	}
}
