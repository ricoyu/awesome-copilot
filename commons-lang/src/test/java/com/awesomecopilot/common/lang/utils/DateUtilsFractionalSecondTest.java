package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-4 回归测试（CODE_REVIEW_REPORT 2026-09-22）：DateUtils.parse 对小数秒位数不等于 3
 * 的时间串给出错误时间且不报任何错。
 * <p>
 * 根因在 SimpleDateFormatHolder.finalShot：按输入的小数秒位数追加同等个 S。
 * SimpleDateFormat 的 S 是"毫秒数"而不是"小数位"：
 * "45" 会被当成 45 毫秒（正确值 450）；"4567" 被当成 4567 毫秒，lenient 进位后秒数 +4；
 * "456789" 累计进位 456 秒，分钟都变了。
 * <p>
 * 修复后的处理规则与 java.time 毫秒取值一致（toEpochMilli 层面）：小数位多于 3 位取前 3 位（截断到毫秒），
 * 少于 3 位按十进制小数右补 0（0.45 秒 = 450 毫秒）。
 * <p>
 * 两个入口（无时区的 DateUtils.parse(source) 与带时区 parse(source, Asia/Shanghai)）
 * 最终都用 Asia/Shanghai 解析，断言按该时区拆字段，不依赖宿主机默认时区。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class DateUtilsFractionalSecondTest {

	private static final TimeZone SHANGHAI = TimeZone.getTimeZone("Asia/Shanghai");

	private static void assertFields(Date parsed, int wantSecond, int wantMillis, String desc) {
		assertThat(parsed).as("%s 应解析成功", desc).isNotNull();
		Calendar c = Calendar.getInstance(SHANGHAI);
		c.setTime(parsed);
		assertThat(c.get(Calendar.YEAR)).as(desc + " 年").isEqualTo(2026);
		assertThat(c.get(Calendar.MONTH)).as(desc + " 月").isEqualTo(Calendar.SEPTEMBER);
		assertThat(c.get(Calendar.DAY_OF_MONTH)).as(desc + " 日").isEqualTo(22);
		assertThat(c.get(Calendar.HOUR_OF_DAY)).as(desc + " 时").isEqualTo(10);
		assertThat(c.get(Calendar.MINUTE)).as(desc + " 分（修复前 6 位小数会进位成 28）").isEqualTo(20);
		assertThat(c.get(Calendar.SECOND)).as(desc + " 秒（修复前 4 位小数会进位成 34）").isEqualTo(wantSecond);
		assertThat(c.get(Calendar.MILLISECOND)).as(desc + " 毫秒").isEqualTo(wantMillis);
	}

	private static void assertBothEntries(String source, int wantSecond, int wantMillis) {
		assertFields(DateUtils.parse(source), wantSecond, wantMillis, "无时区入口 " + source);
		assertFields(DateUtils.parse(source, SHANGHAI), wantSecond, wantMillis, "带时区入口 " + source);
	}

	@Test
	public void testTwoDigitFraction() {
		//修复前实测: 解析成 10:20:30.045(SDF 把 "45" 当 45 毫秒, 正确值 450)
		assertBothEntries("2026-09-22 10:20:30.45", 30, 450);
	}

	@Test
	public void testFourDigitFraction() {
		//修复前实测: 解析成 10:20:34.567(4 个 S 触发 lenient 进位, 秒数 +4)
		assertBothEntries("2026-09-22 10:20:30.4567", 30, 456);
	}

	@Test
	public void testSixDigitFraction() {
		//修复前实测: 解析成 10:28:06.789(6 个 S 累计进位 456 秒, 分钟都变了)
		assertBothEntries("2026-09-22 10:20:30.456789", 30, 456);
	}

	@Test
	public void testOneDigitFraction() {
		assertBothEntries("2026-09-22 10:20:30.4", 30, 400);
	}

	@Test
	public void testStandardThreeDigitUnaffected() {
		//3 位小数秒这条正常路径不能被改坏
		assertBothEntries("2026-09-22 10:20:30.123", 30, 123);
	}

	@Test
	public void testIsoTFormWithSixDigitFraction() {
		//规整方法用 PT_ALL 重扫输入，'T' 分隔形式的小数位同样要处理正确
		Date parsed = DateUtils.parse("2026-09-22T10:20:30.456789");
		assertThat(parsed).isNotNull();
		Calendar c = Calendar.getInstance(SHANGHAI);
		c.setTime(parsed);
		assertThat(c.get(Calendar.MINUTE)).as("分（若 S 按位数拼接会进位成 28）").isEqualTo(20);
		assertThat(c.get(Calendar.SECOND)).isEqualTo(30);
		assertThat(c.get(Calendar.MILLISECOND)).isEqualTo(456);
	}

	@Test
	public void testTFormWithZoneOffsetAndTwoDigitFraction() {
		//带 +0800 后缀时 group(8) 的起止位置不能把偏移数字卷进规整
		Date parsed = DateUtils.parse("2026-09-22T10:20:30.45+0800");
		assertThat(parsed).isNotNull();
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));
		c.setTime(parsed);
		assertThat(c.get(Calendar.SECOND)).isEqualTo(30);
		assertThat(c.get(Calendar.MILLISECOND)).isEqualTo(450);
	}

	@Test
	public void testNoFractionStillWorks() {
		assertBothEntries("2026-09-22 10:20:30", 30, 0);
	}
}
