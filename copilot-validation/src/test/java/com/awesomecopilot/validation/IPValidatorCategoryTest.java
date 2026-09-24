package com.awesomecopilot.validation;

import com.awesomecopilot.common.spring.i18n.LocaleContextHolder;
import com.awesomecopilot.validation.enums.IPCategory;
import com.awesomecopilot.validation.validation.IPValidator;
import com.awesomecopilot.validation.validation.annotation.IP;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @IP 注解 category 维度的回归测试。
 * <p>
 * IPValidator 原实现第二个 if 未写成 else if: category=IP_V4 时第一个 if 算出的 IPv4 校验结果
 * 会被 else 分支的 "IPv4 || IPv6" 覆盖, 导致标了 @IP(IP_V4) 的字段放行合法 IPv6 地址。
 * <p>
 * Copyright: (C), 2026/9/24
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class IPValidatorCategoryTest {

	private static final String IPV4 = "192.168.1.1";
	private static final String IPV6 = "2001:db8::ff00:42:8329";
	private static final String NOT_AN_IP = "deepdata$";

	private IPValidator validator;
	private ConstraintValidatorContext context;

	@BeforeEach
	public void setUp() {
		validator = new IPValidator();
		context = Mockito.mock(ConstraintValidatorContext.class);
		ConstraintValidatorContext.ConstraintViolationBuilder builder =
				Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
		when(context.buildConstraintViolationWithTemplate(Mockito.anyString())).thenReturn(builder);
	}

	private void initializeWith(IPCategory category) {
		IP ip = Mockito.mock(IP.class);
		when(ip.category()).thenReturn(category);
		when(ip.message()).thenReturn("IP 地址不合法");
		validator.initialize(ip);
	}

	@Test
	public void testV4CategoryRejectsIpv6Address() {
		initializeWith(IPCategory.IP_V4);
		// bug 复现断言: 现状(修复前)这里返回 true, 本用例应失败(RED), 修复后通过
		assertThat(validator.isValid(IPV6, context)).isFalse();
		// 失败分支的对外行为: 默认违规被禁用, 且给出错误消息(无 MessageSource 时走默认值)
		Mockito.verify(context).disableDefaultConstraintViolation();
		Mockito.verify(context).buildConstraintViolationWithTemplate("IP 地址不合法");
	}

	@Test
	public void testFailurePathDoesNotThrowWhenMessageSourceActive() {
		// 运行期 LocaleConfigurerFilter 会给每个请求装好 MessageSource;
		// 消息源里查不到 "IP 地址不合法" 这个 code 时, 校验失败应正常给出默认消息, 而不是抛 NoSuchMessageException
		LocaleContextHolder.setMessageSource(new StaticMessageSource());
		try {
			initializeWith(IPCategory.IP_V4);
			assertThat(validator.isValid(IPV6, context)).isFalse();
			Mockito.verify(context).buildConstraintViolationWithTemplate("IP 地址不合法");
		} finally {
			LocaleContextHolder.setMessageSource(null);
		}
	}

	@Test
	public void testV4CategoryAcceptsIpv4Address() {
		initializeWith(IPCategory.IP_V4);
		assertThat(validator.isValid(IPV4, context)).isTrue();
	}

	@Test
	public void testV6CategoryRejectsIpv4Address() {
		initializeWith(IPCategory.IP_V6);
		assertThat(validator.isValid(IPV4, context)).isFalse();
	}

	@Test
	public void testV6CategoryAcceptsIpv6Address() {
		initializeWith(IPCategory.IP_V6);
		assertThat(validator.isValid(IPV6, context)).isTrue();
	}

	@Test
	public void testUnrestrictedAcceptsBothFamilies() {
		initializeWith(IPCategory.UN_RESTRICTED);
		assertThat(validator.isValid(IPV4, context)).isTrue();
		assertThat(validator.isValid(IPV6, context)).isTrue();
	}

	@Test
	public void testAnyCategoryRejectsNonIpString() {
		initializeWith(IPCategory.UN_RESTRICTED);
		assertThat(validator.isValid(NOT_AN_IP, context)).isFalse();
	}

	@Test
	public void testV4AndV6CategoriesRejectNonIpString() {
		initializeWith(IPCategory.IP_V4);
		assertThat(validator.isValid(NOT_AN_IP, context)).isFalse();
		initializeWith(IPCategory.IP_V6);
		assertThat(validator.isValid(NOT_AN_IP, context)).isFalse();
	}

	@Test
	public void testBlankValueSkippedByAllCategories() {
		for (IPCategory category : IPCategory.values()) {
			initializeWith(category);
			assertThat(validator.isValid(null, context)).as("category=%s null", category).isTrue();
			assertThat(validator.isValid("  ", context)).as("category=%s blank", category).isTrue();
		}
	}
}
