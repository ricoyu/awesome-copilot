package com.awesomecopilot.web.utils;

import com.awesomecopilot.common.spring.context.ApplicationContextHolder;
import com.awesomecopilot.web.exception.LocalizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * MessageHelper 懒加载与降级行为测试。
 * <p>
 * 评审报告 P1-2：旧实现在 Spring 容器里没有 MessageSource Bean 时,
 * getBean(MessageSource.class) 抛出 NoSuchBeanDefinitionException 直接穿透给调用方
 * （表现为接口返回 500 且错误信息丢失）, "取不到就用默认消息"并没有实现;
 * 双检锁进入 synchronized 后也没有二次判空。修复后要求:
 * 取不到 MessageSource 时 getMessage 按各重载语义返回 null / defaultMessage / code,
 * 并且调用方能从日志里看到为什么拿到 null。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MessageHelperTest {

	@BeforeEach
	@AfterEach
	public void resetLazyState() throws Exception {
		setStaticField("messageSource", null);
		setStaticFieldIfPresent("messageSourceMissing", false);
		new ApplicationContextHolder().setApplicationContext(null);
	}

	private static void setStaticField(String name, Object value) throws ReflectiveOperationException {
		Field f = MessageHelper.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	private static void setStaticFieldIfPresent(String name, Object value) {
		try {
			setStaticField(name, value);
		} catch (ReflectiveOperationException ignored) {
			// 修复前的代码里还没有这个字段
		}
	}

	private static ApplicationContext contextWith(MessageSource messageSource) {
		// GenericApplicationContext 不像 StaticApplicationContext 会自动注册 messageSource,
		// 可以精确模拟"容器里有/没有 MessageSource Bean"两种状态
		GenericApplicationContext ctx = new GenericApplicationContext();
		if (messageSource != null) {
			ctx.getBeanFactory().registerSingleton("messageSource", messageSource);
		}
		ctx.refresh();
		return ctx;
	}

	/**
	 * 模拟"容器里没有 MessageSource Bean"时Spring容器的行为: getBean 抛 NoSuchBeanDefinitionException
	 */
	private static ApplicationContext contextThatThrowsOnMessageSourceLookup() {
		return new GenericApplicationContext() {
			@Override
			public <T> T getBean(Class<T> requiredType) throws org.springframework.beans.BeansException {
				throw new org.springframework.beans.factory.NoSuchBeanDefinitionException(requiredType);
			}
		};
	}

	@Test
	public void testGetMessageWithoutMessageSourceBeanReturnsNullInsteadOfThrowing() {
		// 容器里查不到 MessageSource Bean: 旧代码在这里抛 NoSuchBeanDefinitionException 直接穿透
		new ApplicationContextHolder().setApplicationContext(contextThatThrowsOnMessageSourceLookup());

		assertNull(MessageHelper.getMessage("any.code"));
	}

	@Test
	public void testGetMessageFallsBackToDefaultMessageWhenNoMessageSource() {
		new ApplicationContextHolder().setApplicationContext(contextThatThrowsOnMessageSourceLookup());

		assertEquals("抱歉", MessageHelper.getMessage("any.code", "抱歉"));
		assertEquals("抱歉", MessageHelper.getMessage("any.code", "抱歉", 1, 2));
	}

	@Test
	public void testGetMessageReturnsCodeWhenNoMessageSourceForLocaleOverload() {
		new ApplicationContextHolder().setApplicationContext(contextThatThrowsOnMessageSourceLookup());

		// 该重载实现里 defaultMessage 就是 code 本身
		assertEquals("any.code", MessageHelper.getMessage("any.code", Locale.CHINA, 1));
	}

	@Test
	public void testNonSpringAppReturnsNullWithoutThrowing() {
		// 完全不在 Spring 环境(ApplicationContextHolder 里 context 为 null)
		assertNull(MessageHelper.getMessage("any.code"));
		assertNull(MessageHelper.getParameteredMessage("any.code", 1));
	}

	@Test
	public void testMissingKeyReturnsNull() {
		new ApplicationContextHolder().setApplicationContext(contextWith(new StaticMessageSource()));

		assertNull(MessageHelper.getMessage("missing.key"));
		assertNull(MessageHelper.getMessage("missing.key", List.of("arg")));
	}

	@Test
	public void testResolvedMessageIsCachedAcrossContextRemoval() {
		StaticMessageSource ms = new StaticMessageSource();
		ms.addMessage("hello", Locale.getDefault(), "你好");
		new ApplicationContextHolder().setApplicationContext(contextWith(ms));
		assertEquals("你好", MessageHelper.getMessage("hello"));

		// MessageSource 一旦取到就缓存, 之后容器引用被清掉也不应重新查容器
		new ApplicationContextHolder().setApplicationContext(null);
		assertEquals("你好", MessageHelper.getMessage("hello"));
	}

	@Test
	public void testLocalizedExceptionFallsBackToDefaultMessage() {
		new ApplicationContextHolder().setApplicationContext(contextThatThrowsOnMessageSourceLookup());

		// 无 MessageSource 时异常消息要落到 defaultMessage, 而不是抛异常或返回 null
		LocalizedException e = new LocalizedException("404", "missing.template", "订单不存在");
		assertEquals("订单不存在", e.getLocalizedMessage());
	}
}
