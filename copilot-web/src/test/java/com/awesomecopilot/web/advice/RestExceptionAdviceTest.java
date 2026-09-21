package com.awesomecopilot.web.advice;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.awesomecopilot.common.lang.exception.BusinessException;
import com.awesomecopilot.common.lang.exception.EntityNotFoundException;
import com.awesomecopilot.common.lang.exception.ServiceException;
import com.awesomecopilot.common.lang.vo.Result;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RestExceptionAdvice 行为测试（评审报告 P2-1 / P2-2 / P2-3 / P2-4）。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class RestExceptionAdviceTest {

	// ---------- P2-4: 请求体格式错误是客户端问题, 必须返回 400 ----------

	@Test
	public void testHttpMessageNotReadableReturns400() throws Exception {
		RestExceptionAdvice advice = new RestExceptionAdvice();

		// 走父类 public final handleException 的真实分发路径(与 Spring MVC 运行时一致)
		var entity = advice.handleException(
				new HttpMessageNotReadableException("Unexpected character 'x'",
						new org.springframework.http.HttpInputMessage() {
							@Override
							public java.io.InputStream getBody() {
								return java.io.InputStream.nullInputStream();
							}

							@Override
							public org.springframework.http.HttpHeaders getHeaders() {
								return new org.springframework.http.HttpHeaders();
							}
						}),
				new ServletWebRequest(new MockHttpServletRequest()));

		assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode(),
				"客户端提交的 JSON 解析失败不应返回 500, 否则网关/熔断会把它计入服务端失败率");
	}

	// ---------- P2-3: 上传超限必须进自定义 handler, 且能解析出实际/限制大小 ----------

	@Test
	public void testMaxUploadSizeExceededEntersCustomHandlerViaParentDispatch() throws Exception {
		RestExceptionAdvice advice = new RestExceptionAdvice();
		// Spring 6.1 的父类用 public final handleException 按 instanceof 分发到各 handleXxx;
		// 子类方法签名若不匹配父类, 这里会走父类默认实现返回 413 ProblemDetail 而不是 200+Result
		MaxUploadSizeExceededException e = new MaxUploadSizeExceededException(104857600L,
				new IllegalStateException("org.apache.tomcat.util.http.fileupload.impl"
						+ ".SizeLimitExceededException: the request was rejected because its size (405491652)"
						+ " exceeds the configured maximum (104857600)"));

		var entity = advice.handleException(e, new ServletWebRequest(new MockHttpServletRequest()));

		assertEquals(HttpStatus.OK, entity.getStatusCode(), "自定义分支返回 200+Result(父类默认是 413+ProblemDetail)");
		assertTrue(entity.getBody() instanceof Result);
		assertEquals("4005", ((Result) entity.getBody()).getCode());
		String message = String.valueOf(((Result) entity.getBody()).getMessage());
		// 修复前: ACTUAL_SIZE_PATTERN 的 .*...$ 结构永远匹配不上任何大小文本(见处置表),
		// 且 Spring 6.1.5 的 getMessage() 只剩 "Maximum upload size of 104857600 bytes exceeded",
		// 实际大小在 cause 文本里——修复后限制大小来自 e.getMaxUploadSize(), 不再依赖正则碰运气
		assertTrue(message.contains("104857600"), "消息里必须带限制大小, 实际=" + message);
		assertFalse(message.isBlank(), "返回给客户端的消息不能是空串, 实际=" + message);
	}

	@Test
	public void testMaxUploadSizeExceededWithoutCauseSizeStillReturnsCode4005() throws Exception {
		RestExceptionAdvice advice = new RestExceptionAdvice();

		var entity = advice.handleException(new MaxUploadSizeExceededException(1024L), new ServletWebRequest(new MockHttpServletRequest()));

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("4005", ((Result) entity.getBody()).getCode());
	}

	/**
	 * 独立评审 S-1: cause 文本里出现超过 Long 范围的数字时, 提取逻辑不能抛
	 * NumberFormatException——异常解析器自己失败会落到 Spring 默认错误页, 客户端连 4005 都拿不到
	 */
	@Test
	public void testAbsurdHugeNumberInCauseDoesNotBreakTheHandler() throws Exception {
		RestExceptionAdvice advice = new RestExceptionAdvice();
		MaxUploadSizeExceededException e = new MaxUploadSizeExceededException(104857600L,
				new IllegalStateException("the request was rejected because its size (99999999999999999999999)"
						+ " exceeds the configured maximum (104857600)"));

		var entity = advice.handleException(e, new ServletWebRequest(new MockHttpServletRequest()));

		assertEquals(HttpStatus.OK, entity.getStatusCode(), "解析器自身不能抛异常, 至少降级返回 4005");
		assertEquals("4005", ((Result) entity.getBody()).getCode());
	}

	/**
	 * 独立评审 S-2: 遍历 cause 链必须有深度上限——Throwable.initCause 允许构造 a→b→a 双节点环,
	 * 无上限循环会永久占死处理该请求的线程。超时断言: 修复前 RED 表现为超时, 修复后毫秒级返回。
	 */
	@Test
	public void testCauseCycleDoesNotHangTheHandler() throws Exception {
		RestExceptionAdvice advice = new RestExceptionAdvice();
		Throwable a = new RuntimeException("a");
		Throwable b = new RuntimeException("b");
		a.initCause(b);
		b.initCause(a); // 允许: initCause 只拒绝 cause==this, 不检测跨对象环
		MaxUploadSizeExceededException e = new MaxUploadSizeExceededException(104857600L, a);

		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			var entity = advice.handleException(e, new ServletWebRequest(new MockHttpServletRequest()));
			assertEquals(HttpStatus.OK, entity.getStatusCode());
		}, "cause 环不能导致无限循环");
	}

	// ---------- P2-1: 异常路径上不允许每次都查容器 ----------

	@Test
	public void testSentinelBeanLookupIsDoneAtMostOnce() {
		AtomicInteger lookups = new AtomicInteger();
		ApplicationContext ctx = (ApplicationContext) Proxy.newProxyInstance(
				RestExceptionAdviceTest.class.getClassLoader(),
				new Class<?>[]{ApplicationContext.class},
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
						if ("getBean".equals(method.getName()) && args != null && args.length == 1
								&& args[0] instanceof String) {
							lookups.incrementAndGet();
							throw new org.springframework.beans.factory.NoSuchBeanDefinitionException((String) args[0]);
						}
						Class<?> rt = method.getReturnType();
						if (rt == boolean.class) return false;
						if (rt == int.class) return 0;
						return null;
					}
				});
		RestExceptionAdvice advice = new RestExceptionAdvice();
		advice.setApplicationContext(ctx);

		for (int i = 0; i < 5; i++) {
			advice.handleThrowable(new RuntimeException("boom-" + i));
		}

		assertEquals(1, lookups.get(),
				"没有 Sentinel 时也只允许第一次异常查一次容器, 之后必须缓存结果(修复前每个错误请求查一次)");
	}

	// ---------- P2-2: 日志消息不允许是空串 ----------

	@Test
	public void testExceptionHandlersLogNonEmptyMessages() {
		RestExceptionAdvice advice = new RestExceptionAdvice();
		ch.qos.logback.classic.Logger adviceLogger =
				(ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RestExceptionAdvice.class);
		ch.qos.logback.classic.Level originalLevel = adviceLogger.getLevel();
		adviceLogger.setLevel(ch.qos.logback.classic.Level.DEBUG); // 显式放宽, 不依赖 root 级别恰好允许 info
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		adviceLogger.addAppender(appender);
		try {
			advice.handleBusinessException(new BusinessException("5001", "库存不足"));
			advice.handleServiceException(new ServiceException("5002", "服务开小差"));
			advice.handleEntityNotFoundException(new EntityNotFoundException("订单不存在"));
			advice.handleUniqueConstraintViolationException(
					new com.awesomecopilot.validation.exception.UniqueConstraintViolationException("uk_name"));
			advice.handleLocalizedException(
					new com.awesomecopilot.web.exception.LocalizedException("5001", "tpl.missing", "服务繁忙"));
			advice.handleHttpRequestMethodNotSupported(
					new HttpRequestMethodNotSupportedException("DELETE"),
					new org.springframework.http.HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED, new ServletWebRequest(new MockHttpServletRequest()));
		} finally {
			adviceLogger.detachAppender(appender);
			adviceLogger.setLevel(originalLevel);
		}

		assertEquals(6, appender.list.size(), "六个 handler 各产生一条日志");
		for (ILoggingEvent event : appender.list) {
			assertTrue(event.getFormattedMessage() != null && !event.getFormattedMessage().isBlank(),
					"日志消息不能是空串(修复前六处 log.error(\"\", e) 在日志检索时没有关键词)");
		}
	}

	// ---------- 覆写关系锚点(签名漂移防护) ----------

	@Test
	public void testUploadHookOverrideAnchor() throws Exception {
		// 独立评审 S-6: 旧名 testTypeMismatchStillDelegatesToParent 名不副实, 改为如实命名。
		// 本用例只锚定"父类 protected 钩子签名存在"——签名一旦变化, @Override 会让编译直接失败,
		// 双保险防止子类方法无声失去覆写(评审报告 P2-3 的担忧点)。真实分发行为由
		// testMaxUploadSizeExceededEntersCustomHandlerViaParentDispatch 覆盖。
		assertTrue(ResponseEntityExceptionHandler.class
				.getDeclaredMethod("handleMaxUploadSizeExceededException",
						MaxUploadSizeExceededException.class, org.springframework.http.HttpHeaders.class,
						HttpStatusCode.class, org.springframework.web.context.request.WebRequest.class)
				.getParameterCount() == 4);
	}
}
