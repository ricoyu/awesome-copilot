package com.awesomecopilot.web.resolver;

import org.junit.jupiter.api.Test;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;

import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 日期参数解析器测试。
 * <p>
 * 评审报告 P1-4：旧实现里 DateArgumentResolver 直接走 DateUtils.parse 自动格式匹配,
 * 完全忽略参数上的 @DateTimeFormat(pattern=...) —— 开发者写了格式化注解却不生效且没有任何提示。
 * 修复后 Resolver 必须优先按注解解析, 没有注解时才走自动匹配。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class DateArgumentResolversTest {

	// ---- 模拟 Controller 方法：参数名 + @DateTimeFormat 注解 ----

	static class Ctl {
		public void dateWithPattern(@RequestParam("v") @DateTimeFormat(pattern = "dd-MM-yyyy") Date v) {
		}

		public void dateAuto(@RequestParam("v") Date v) {
		}

		public void dateWithIso(@RequestParam("v") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date v) {
		}

		public void localDateWithIso(@RequestParam("v") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate v) {
		}

		public void localDateTimeWithIso(@RequestParam("v") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime v) {
		}

		public void localTimeWithIso(@RequestParam("v") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime v) {
		}

		public void localDateWithPattern(@RequestParam("v") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate v) {
		}

		public void localDateTimeWithPattern(@RequestParam("v") @DateTimeFormat(pattern = "yyyy年MM月dd日 HH时mm分") LocalDateTime v) {
		}

		public void localTimeWithPattern(@RequestParam("v") @DateTimeFormat(pattern = "HH时mm分") LocalTime v) {
		}
	}

	private static MethodParameter param(String methodName, Class<?> paramType) throws Exception {
		MethodParameter mp = new MethodParameter(Ctl.class.getDeclaredMethod(methodName, paramType), 0);
		mp.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
		return mp;
	}

	private static ServletWebRequest webRequestFor(MethodParameter mp, String value) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		if (value != null) {
			// resolver 通过 parameter.getParameterName() 取参, mock 请求必须用同名参数
			request.setParameter(mp.getParameterName(), value);
		}
		return new ServletWebRequest(request);
	}

	private static Calendar cal(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return c;
	}

	@Test
	public void testDateResolverHonorsDateTimeFormatPattern() throws Exception {
		DateArgumentResolver resolver = new DateArgumentResolver();

		MethodParameter mp = param("dateWithPattern", Date.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "17-09-2026"), null);

		// 修复前: 自动解析不认 dd-MM-yyyy, 返回 null(注解被无声忽略)
		assertTrue(result instanceof Date, "@DateTimeFormat(pattern)必须生效");
		Calendar c = cal((Date) result);
		assertEquals(2026, c.get(Calendar.YEAR));
		assertEquals(Calendar.SEPTEMBER, c.get(Calendar.MONTH));
		assertEquals(17, c.get(Calendar.DAY_OF_MONTH));
	}

	@Test
	public void testDateResolverStillAutoParsesWithoutAnnotation() throws Exception {
		DateArgumentResolver resolver = new DateArgumentResolver();

		MethodParameter mp = param("dateAuto", Date.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026-09-17 08:30:00"), null);

		assertTrue(result instanceof Date, "无注解时仍走自动格式匹配");
		Calendar c = cal((Date) result);
		assertEquals(2026, c.get(Calendar.YEAR));
		assertEquals(8, c.get(Calendar.HOUR_OF_DAY));
		assertEquals(30, c.get(Calendar.MINUTE));
	}

	@Test
	public void testDateResolverReturnsNullForBlankParameter() throws Exception {
		DateArgumentResolver resolver = new DateArgumentResolver();

		MethodParameter mp = param("dateAuto", Date.class);
		assertNull(resolver.resolveArgument(mp, null,
				webRequestFor(mp, null), null));
	}

	@Test
	public void testLocalDateResolverHonorsPattern() throws Exception {
		LocalDateArgumentResolver resolver = new LocalDateArgumentResolver();

		MethodParameter mp = param("localDateWithPattern", LocalDate.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "17/09/2026"), null);

		assertEquals(LocalDate.of(2026, 9, 17), result);
	}

	@Test
	public void testLocalDateTimeResolverHonorsPattern() throws Exception {
		LocalDateTimeArgumentResolver resolver = new LocalDateTimeArgumentResolver();

		MethodParameter mp = param("localDateTimeWithPattern", LocalDateTime.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026年09月17日 08时30分"), null);

		assertEquals(LocalDateTime.of(2026, 9, 17, 8, 30), result);
	}

	@Test
	public void testLocalTimeResolverHonorsPattern() throws Exception {
		LocalTimeArgumentResolver resolver = new LocalTimeArgumentResolver();

		MethodParameter mp = param("localTimeWithPattern", LocalTime.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "08时30分"), null);

		assertEquals(LocalTime.of(8, 30), result);
	}

	// ---- @DateTimeFormat(iso=...) 分支：必须与 Spring 官方 iso 语义一致(接受可选的时区偏移) ----

	@Test
	public void testDateResolverIsoAcceptsZuluValue() throws Exception {
		DateArgumentResolver resolver = new DateArgumentResolver();

		// 修复前(第一版实现)用 ISO_LOCAL_DATE_TIME, 对带 Z 的值抛 DateTimeParseException,
		// 而旧代码(DateUtils自动解析)这类值是能解的——等于修复本身造成了倒退
		MethodParameter mp = param("dateWithIso", Date.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026-09-17T08:30:00.003Z"), null);

		assertTrue(result instanceof Date);
		// Z 表示 UTC 08:30, 东八区本地时间应为 16:30
		assertEquals(16, cal((Date) result).get(Calendar.HOUR_OF_DAY));
	}

	@Test
	public void testDateResolverIsoAcceptsOffsetValue() throws Exception {
		DateArgumentResolver resolver = new DateArgumentResolver();

		MethodParameter mp = param("dateWithIso", Date.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026-09-17T08:30:00+08:00"), null);

		assertTrue(result instanceof Date);
		assertEquals(8, cal((Date) result).get(Calendar.HOUR_OF_DAY));
	}

	@Test
	public void testDateResolverIsoPlainValueStillWorks() throws Exception {
		DateArgumentResolver resolver = new DateArgumentResolver();

		MethodParameter mp = param("dateWithIso", Date.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026-09-17T08:30:00"), null);

		assertTrue(result instanceof Date);
		assertEquals(8, cal((Date) result).get(Calendar.HOUR_OF_DAY));
	}

	@Test
	public void testLocalDateResolverIsoAcceptsZoneSuffix() throws Exception {
		LocalDateArgumentResolver resolver = new LocalDateArgumentResolver();

		MethodParameter mp = param("localDateWithIso", LocalDate.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026-09-17+08:00"), null);

		assertEquals(LocalDate.of(2026, 9, 17), result);
	}

	@Test
	public void testLocalDateTimeResolverIsoAcceptsZuluValue() throws Exception {
		LocalDateTimeArgumentResolver resolver = new LocalDateTimeArgumentResolver();

		MethodParameter mp = param("localDateTimeWithIso", LocalDateTime.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026-09-17T08:30:00.003Z"), null);

		assertEquals(LocalDateTime.of(2026, 9, 17, 8, 30, 0, 3_000_000), result);
	}

	@Test
	public void testLocalTimeResolverIsoAcceptsZuluValue() throws Exception {
		LocalTimeArgumentResolver resolver = new LocalTimeArgumentResolver();

		MethodParameter mp = param("localTimeWithIso", LocalTime.class);
		Object result = resolver.resolveArgument(mp, null,
				webRequestFor(mp, "10:15:30Z"), null);

		assertEquals(LocalTime.of(10, 15, 30), result);
	}

	// ---- 失败路径: 输入与 pattern 不符必须报错, 不允许悄悄解析出错值 ----

	@Test
	public void testDateResolverPatternMismatchThrowsNotSilentWrongDate() throws Exception {
		DateArgumentResolver resolver = new DateArgumentResolver();

		// 第一版实现走 DateUtils.parse(source, format), 其底层 SimpleDateFormat 是 lenient 模式:
		// "2026-09-17" 按 "dd-MM-yyyy" 解析出"公元23年"的错值还不报错——这种不报错的错值比抛异常危险得多
		MethodParameter mp = param("dateWithPattern", Date.class);
		assertThrows(RuntimeException.class, () -> resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026-09-17"), null));
	}

	@Test
	public void testLocalResolversPatternMismatchThrows() throws Exception {
		LocalDateArgumentResolver resolver = new LocalDateArgumentResolver();

		MethodParameter mp = param("localDateWithPattern", LocalDate.class);
		assertThrows(DateTimeParseException.class, () -> resolver.resolveArgument(mp, null,
				webRequestFor(mp, "2026-09-17"), null));
	}

	@Test
	public void testSupportsParameterCoversDateSubclassesOnly() throws Exception {
		DateArgumentResolver resolver = new DateArgumentResolver();

		assertTrue(resolver.supportsParameter(param("dateAuto", Date.class)));
		// java.sql.Timestamp 是 Date 子类, 也应支持
		MethodParameter sqlParam = new MethodParameter(
				SubCtl.class.getDeclaredMethod("sql", Timestamp.class), 0);
		sqlParam.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
		assertTrue(resolver.supportsParameter(sqlParam));
		assertFalse(resolver.supportsParameter(param("localDateWithPattern", LocalDate.class)),
				"LocalDate 不归 Date resolver 管");
	}

	static class SubCtl {
		public void sql(Timestamp t) {
		}
	}
}
