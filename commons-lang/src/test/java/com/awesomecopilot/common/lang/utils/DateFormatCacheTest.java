package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.constants.DateConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.awesomecopilot.common.lang.constants.DateConstants.PT_DATE_EN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 日期格式器缓存组回归测试（CODE_REVIEW_REPORT P1-1 / P1-2 / P1-3 / P1-16 / P2-36）。
 * <p>
 * P1-1：DateTimeFormatter 是不可变对象，三处 format.withZone(...) 返回值被丢弃，时区从未生效。
 * P1-2：缓存键漏维度——formatFor(pattern) 与 formatFor(pattern, locale) 共用键 pattern；
 * timezone 版键只用 timezone；SimpleDateFormatHolder 侧 ENGLISH/FRENCH 的 getCountry() 都是
 * 空串，键都是 pattern+""，互相顶掉。
 * P1-3：TIME_ZONE_LOCALE_HASH_MAP 键类型是 TimeZone，取值传 timezone.getID()（String），
 * 恒为 null，"按时区挑默认语言"从未生效。
 * P1-16：PT_DATE_EN 家族正则把 \\\\d 多转义了一层（字面反斜杠），MM-dd-yyyy 永远匹配不上，
 * DateUtils.parse("12-25-2020") 返回 null；且正则分隔符是连字符而 FMT 格式串是斜杠，两边不一致。
 * P2-36：TIME_ZONE_LOCALE_HASH_MAP 是 public 可变 HashMap，任意调用方可清空全局表。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class DateFormatCacheTest {

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static final TimeZone SHANGHAI = TimeZone.getTimeZone("Asia/Shanghai");

	@BeforeEach
	public void clearCaches() {
		//两个 Holder 的缓存都是 ThreadLocal，清掉避免用例间互相干扰
		DateFormatterHolder.clearThreadLocal();
		SimpleDateFormatHolder.clearThreadLocal();
	}

	// ---------- P1-1 withZone 返回值被丢弃 ----------

	@Test
	public void testDateTimeFormatterZoneActuallyApplied() {
		DateTimeFormatter f = DateFormatterHolder.formatFor("yyyy-MM-dd HH:mm:ss", SHANGHAI);
		//修复前实测: getZone() 恒 null——withZone 返回新实例但没人接收
		assertThat(f.getZone())
				.as("formatFor(pattern, timezone) 应返回带该时区的格式器")
				.isEqualTo(ZoneId.of("Asia/Shanghai"));

		DateTimeFormatter g = DateFormatterHolder.formatFor("yyyy-MM-dd HH:mm:ss", GMT);
		assertThat(g.getZone()).isEqualTo(ZoneId.of("GMT"));

		//同 pattern 不同 timezone 的格式时刻结果必须不同（时区真正参与计算）
		LocalDateTime ldt = LocalDateTime.of(2026, 9, 23, 12, 0, 0);
		assertThat(f.format(ldt.atZone(ZoneId.of("Asia/Shanghai")).toInstant()
				.atZone(ZoneId.of("Asia/Shanghai")))).isEqualTo("2026-09-23 12:00:00");
		assertThat(g.format(ldt.atZone(GMT.toZoneId()).toInstant().atZone(GMT.toZoneId())))
				.isEqualTo("2026-09-23 12:00:00");
	}

	@Test
	public void testDateTimeFormatterDefaultZoneIsShanghai() {
		DateTimeFormatter f = DateFormatterHolder.formatFor("yyyy-MM-dd HH:mm:ss");
		assertThat(f.getZone())
				.as("默认入口 javadoc 承诺 timezone 为 Asia/Shanghai")
				.isEqualTo(ZoneId.of("Asia/Shanghai"));
	}

	// ---------- P1-2 缓存键碰撞 ----------

	@Test
	public void testDtfPatternOnlyAndPatternLocaleKeysDoNotCollide() {
		//修复前实测: 先建 (pattern, ENGLISH) 后, 无 locale 入口拿到同一 ENGLISH 实例
		DateTimeFormatter en = DateFormatterHolder.formatFor("yyyy-MM-dd EEE", Locale.ENGLISH);
		DateTimeFormatter def = DateFormatterHolder.formatFor("yyyy-MM-dd EEE");

		//DateTimeFormatter 不重写 equals, 只能比输出。评审修复后默认入口固定 Locale.CHINA(与 JVM locale 无关),
		//断言可以精确到中文输出, en_US 机器上同样成立(评审1-P2: 上一版"不等英文"的弱断言在 en_US 必挂)
		java.time.LocalDate date = java.time.LocalDate.of(2026, 9, 22);
		assertThat(en.format(date)).isEqualTo("2026-09-22 Tue");
		assertThat(def.format(date))
				.as("默认入口固定 CHINA, 不得命中 locale 入口的英文实例")
				.isEqualTo("2026-09-22 周二");
	}

	@Test
	public void testDtfTimezoneKeyMustIncludePattern() {
		//修复前实测: formatFor(pattern, timezone) 先建的 pattern 会顶掉后建的——键碰撞时
		//第二个 pattern 拿到第一个的格式器, 格式化 "2026-09-23" 得到 "2026" 而不是 "2026-09"
		DateFormatterHolder.formatFor("yyyy", SHANGHAI);
		DateTimeFormatter second = DateFormatterHolder.formatFor("yyyy-MM", SHANGHAI);
		String out = second.format(java.time.LocalDate.of(2026, 9, 23));
		assertThat(out)
				.as("不同 pattern 同 timezone 应各自建格式器")
				.isEqualTo("2026-09");
	}

	@Test
	public void testSdfEnglishAndFrenchLocalesDoNotCollide() {
		//修复前实测: ENGLISH/FRENCH 的 getCountry() 都是空串, 键相同, 法语入口拿到英文实例
		SimpleDateFormat en = SimpleDateFormatHolder.formatFor("MMM d", Locale.ENGLISH);
		SimpleDateFormat fr = SimpleDateFormatHolder.formatFor("MMM d", Locale.FRENCH);
		Date date = new Date(126, 8, 1); //2026-09-01（Date 旧构造器, 测试专用）
		assertThat(fr.format(date)).as("法语月份名应与英文不同").isNotEqualTo(en.format(date));
		assertThat(fr.format(date)).contains("sept");
	}

	// ---------- P1-3 键类型错用 ----------

	@Test
	public void testTimeZoneLocaleLookupActuallyHits() {
		//修复前实测: TIME_ZONE_LOCALE_HASH_MAP.get(getID()) 恒 null, GMT 也拿不到 ENGLISH
		//GMT→ENGLISH, Asia/Shanghai→CHINA: 同一个 MMM 模式在两种 locale 下月份名不同
		//基准时间取正午, 避免本地/GMT 跨日导致月份不一致
		java.util.Calendar cal = java.util.Calendar.getInstance(SHANGHAI);
		cal.set(2026, java.util.Calendar.SEPTEMBER, 1, 12, 0, 0);
		Date sep2026 = cal.getTime();
		String gmtOut = SimpleDateFormatHolder.formatFor("MMM", GMT).format(sep2026);
		String cnOut = SimpleDateFormatHolder.formatFor("MMM", TimeZone.getTimeZone("Asia/Shanghai")).format(sep2026);
		assertThat(gmtOut).as("GMT 应命中 ENGLISH（修复前恒落到 CHINA, 输出与中文入口相同）")
				.isEqualTo("Sep");
		assertThat(cnOut).isEqualTo("9月");
	}

	// ---------- P1-16 MM-dd-yyyy 正则多转义 ----------

	@Test
	public void testAmericanDateRegexMatches() {
		assertThat(PT_DATE_EN.matcher("12-25-2020").matches())
				.as("PT_DATE_EN 应匹配注释里的示例 12-25-2020（修复前为 false）")
				.isTrue();
		assertThat(DateConstants.PT_DATE_EN_1.matcher("12-5-2020").matches()).isTrue();
		assertThat(DateConstants.PT_DATE_EN_2.matcher("1-25-2020").matches()).isTrue();
		assertThat(DateConstants.PT_DATE_EN_3.matcher("1-5-2020").matches()).isTrue();
	}

	@Test
	public void testParseAmericanDate() {
		Date parsed = DateUtils.parse("12-25-2020");
		//修复前实测: 返回 null（只有一条 warn 日志）
		assertThat(parsed).as("DateUtils.parse(\"12-25-2020\") 应解析成功").isNotNull();
		SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
		iso.setTimeZone(SHANGHAI);
		assertThat(iso.format(parsed)).isEqualTo("2020-12-25");

		assertThat(DateUtils.toLocalDate("12-25-2020"))
				.as("LocalDate 入口同样要能解析（正则分隔符与格式串必须统一）")
				.isEqualTo(java.time.LocalDate.of(2020, 12, 25));
	}

	// ---------- P2-36 全局表可变(表已降 private, 不可变断言见 DateP2FixTest) ----------

	@Test
	public void testTimeZoneLocaleLookupStillServesGmtEnglish() {
		assertThat(DateConstants.localeOf("GMT")).isEqualTo(Locale.ENGLISH);
	}
}
