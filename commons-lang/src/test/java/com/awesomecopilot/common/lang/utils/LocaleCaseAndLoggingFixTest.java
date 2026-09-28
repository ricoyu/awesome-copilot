package com.awesomecopilot.common.lang.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.awesomecopilot.common.lang.enums.SizeUnit;
import com.awesomecopilot.common.lang.vo.OrderBean;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Month;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 2026-09-27 批次回归测试（对应评审报告的用词/健壮性修复）:
 * <ol>
 * <li>无 Locale 的大小写转换: String.toLowerCase()/toUpperCase() 用 JVM 默认 Locale,
 *     土耳其语(Locale "tr")下 'I'.toLowerCase() 得到无点小写 'ı'、'i'.toUpperCase()
 *     得到带点的 'İ'——与英文协议值(I/1、info/INFO)对不上。程序内部的大小写不敏感比较
 *     必须固定 Locale.ROOT。测试在 tr 默认 Locale 下断言行为仍按英文规则。</li>
 * <li>StringUtils.decodeUrl 不支持字符集分支: 修复前 printStackTrace 直打标准错误,
 *     库代码应走 slf4j(与同类 encodeUrl 一致), 用 ListAppender 捕获日志事件断言。</li>
 * </ol>
 * <p>
 * Copyright: Copyright (c) 2026-09-27
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class LocaleCaseAndLoggingFixTest {

	/**
	 * 在土耳其默认 Locale 下执行 body, 结束后恢复。
	 * tr 是经典陷阱语言: 'i'/'I' 各有带点/无点两个变体, 大小写映射与英文不同。
	 */
	private static void withTurkishDefaultLocale(Runnable body) {
		Locale original = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr"));
			body.run();
		} finally {
			Locale.setDefault(original);
		}
	}

	@Test
	public void testContainsIgCaseUnaffectedByTurkishLocale() {
		withTurkishDefaultLocale(() -> {
			// 修复前: "TITLE".toLowerCase(tr)="tıtle"(无点ı), 找不到 'i' → false
			assertThat(StringUtils.containsIgCase("TITLE", "i")).isTrue();
			assertThat(StringUtils.containsAnyIgCase("TITLE", "i")).isTrue();
		});
	}

	@Test
	public void testToLowerCaseUpperCaseUnaffectedByTurkishLocale() {
		withTurkishDefaultLocale(() -> {
			// 修复前: "title".toUpperCase(tr)="TİTLE"(带点İ), 与 "TITLE" 不等
			assertThat(StringUtils.toUpperCase("title")).isEqualTo("TITLE");
			assertThat(StringUtils.toLowerCase("TITLE")).isEqualTo("title");
		});
	}

	@Test
	public void testEnumLookupUnaffectedByTurkishLocale() {
		withTurkishDefaultLocale(() -> {
			// 含字母 i 的枚举名回落匹配: "April" 先按原样 valueOf 失败, 回落 toUpperCase;
			// 修复前 toUpperCase(tr) 得到 "APRİL"(带点İ) → 查不到, 返回 null
			assertThat((Month) EnumUtils.lookupEnum(Month.class, "April")).isEqualTo(Month.APRIL);
			assertThat(OrderBean.DIRECTION.of("asc")).isEqualTo(OrderBean.DIRECTION.ASC);
			assertThat(OrderBean.DIRECTION.of("DESC")).isEqualTo(OrderBean.DIRECTION.DESC);
			assertThat(SizeUnit.parse("kb")).isEqualTo(SizeUnit.KB);
		});
	}

	@Test
	public void testDecodeUrlUnsupportedCharsetLogsViaSlf4j() {
		Logger logger = (Logger) LoggerFactory.getLogger(StringUtils.class);
		Level originalLevel = logger.getLevel();
		logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			// 非法字符集名 → URLDecoder.decode 抛 UnsupportedEncodingException
			String result = StringUtils.decodeUrl("abc", "NO-SUCH-CHARSET");
			assertThat(result).isNull();
			// 修复前: 该分支 printStackTrace(标准错误, ListAppender 收不到), 无任何日志事件
			// getMessage() 是未插值的模板("...charset: {}"), 不含参数值; getFormattedMessage() 才是拼好的整行
			boolean hasErrorLog = appender.list.stream()
					.anyMatch(e -> e.getLevel() == Level.ERROR
							&& String.valueOf(e.getFormattedMessage()).contains("NO-SUCH-CHARSET"));
			assertThat(hasErrorLog)
					.as("不支持字符集的解码失败必须打一条含字符集名的 ERROR 日志")
					.isTrue();
		} finally {
			logger.detachAppender(appender);
			logger.setLevel(originalLevel);
		}
	}
}
