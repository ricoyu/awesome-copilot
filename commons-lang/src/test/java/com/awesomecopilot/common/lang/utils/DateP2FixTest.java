package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.constants.DateConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-4 / P2-24 / P2-36 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <p>
 * 覆盖点：
 * <ul>
 * <li>P2-4 dateDiff(Date,Date) 是"毫秒差整除 24 小时"不是"相差几天"：
 * 修复前实测相隔 23 小时、跨两个日历日的入参返回 0，
 * 同名重载 dateDiff(LocalDate,LocalDate) 返回 -1——两个重载语义不同但名字相同；</li>
 * <li>P2-24 milisToNextHour 只清分秒没清毫秒：修复前实测 (now+diff)%3600000
 * 残留 675~868 毫秒（等于当前时刻的毫秒位），拿它做整点定时的调用点持续后移；</li>
 * <li>P2-24 toLocalDateTimeCTT(LocalDate,ZoneId) 忽略形参 zoneId：
 * 修复前实测传纽约时区与传上海时区结果相同（方法体两次用 ZONE_ID_SHANG_HAI）；</li>
 * <li>P2-24 :689/:807 的 Objects.nonNull(zoneId) 只返回布尔不抛异常，写了等于没写，
 * null 最终在 JDK 内部抛一条主语含糊的 NPE（实测 message="zone"）；</li>
 * <li>P2-24 toLocalDateTime(LocalDate) 传 null 抛 NPE，同族 toLocalDate(Date)/
 * toLocalDateTime(Date) 传 null 返回 null——同一类三种约定并存，统一为 null→null；</li>
 * <li>P2-36 TIME_ZONE_LOCALE_HASH_MAP 降为 private（不可变化已在 P1-3 批次完成，
 * 这里用反射验证"外部类不可直接访问"）。用反射断言而非直接引用：
 * 改成 private 后直接引用会编译失败，反射断言在修复前后都可编译。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class DateP2FixTest {

	// ---------------- P2-4 dateDiff(Date,Date) 按日历天 ----------------

	@Test
	public void testDateDiffOfDatesCrossingCalendarDay() {
		//2021-05-24 01:00 与 2021-05-25 00:00: 毫秒差 23 小时(整除 24h 得 0), 但跨 1 个日历天
		Calendar c1 = Calendar.getInstance();
		c1.clear();
		c1.set(2021, Calendar.MAY, 24, 1, 0, 0);
		Calendar c2 = Calendar.getInstance();
		c2.clear();
		c2.set(2021, Calendar.MAY, 25, 0, 0, 0);
		Date d1 = c1.getTime(), d2 = c2.getTime();

		//修复前实测: dateDiff(d1,d2)=0 而 LocalDate 版=-1, 同一对日期的两个重载给出相反结果
		assertThat(DateUtils.dateDiff(d1, d2))
				.as("跨 1 个日历天应返回 -1, 与 LocalDate 版算法一致(修复前实测返回 0)")
				.isEqualTo(DateUtils.dateDiff(DateUtils.toLocalDate(d1), DateUtils.toLocalDate(d2)))
				.isEqualTo(-1);
		assertThat(DateUtils.dateDiff(d2, d1)).isEqualTo(1);
	}

	@Test
	public void testDateDiffOfDatesKeepsWholeDaySemantics() {
		//整天入参不回退: 整 7 天的两对日期仍返回 ±7（与既有 DateUtilsTest.testDateDiff 一致）
		Calendar c1 = Calendar.getInstance();
		c1.clear();
		c1.set(2021, Calendar.MAY, 25, 0, 0, 0);
		Calendar c2 = Calendar.getInstance();
		c2.clear();
		c2.set(2021, Calendar.MAY, 18, 0, 0, 0);
		assertThat(DateUtils.dateDiff(c1.getTime(), c2.getTime())).isEqualTo(7);
		assertThat(DateUtils.dateDiff(c2.getTime(), c1.getTime())).isEqualTo(-7);
	}

	// ---------------- P2-24 milisToNextHour 毫秒位 ----------------

	@Test
	public void testMilisToNextHourLandsExactlyOnHourBoundary() throws Exception {
		//修复前实测: (now+diff)%3600000 残留 675~868(=当时的毫秒位), 整点定时持续后移
		for (int i = 0; i < 20; i++) {
			long now = System.currentTimeMillis();
			long diff = DateUtils.milisToNextHour();
			long landing = now + diff;
			long rem = landing % 3_600_000L;
			//now 取在调用之前, landing 会比真边界早 0~几毫秒; 修复前的残差是毫秒位(可达 999),
			//双向距边界 <50ms 即可判"落在整点上"
			assertThat(Math.min(rem, 3_600_000L - rem))
					.as("第 %d 轮: now+diff 应落在整点边界(修复前残留=当时的毫秒位)", i)
					.isLessThan(50L);
			Thread.sleep(3);
		}
	}

	// ---------------- P2-24 toLocalDateTimeCTT 尊重时区参数 ----------------

	@Test
	public void testToDateTimeCTTConvertsFromZoneArgToEast8Literal() {
		//按参数实现: 把 LocalDate 视为 zoneId 时区的零点, 返回其在东八区(CTT)的日期时间字面量。
		//修复前实测: 方法体两次用 ZONE_ID_SHANG_HAI, 形参 zoneId 一次没用——传纽约也拿到东八区零点。
		LocalDate date = LocalDate.of(2021, 5, 24);
		assertThat(DateUtils.toLocalDateTimeCTT(date, ZoneId.of("Asia/Shanghai")))
				.as("传东八区: 行为与修复前一致, 防回退")
				.isEqualTo(LocalDateTime.of(2021, 5, 24, 0, 0));
		assertThat(DateUtils.toLocalDateTimeCTT(date, ZoneId.of("America/New_York")))
				.as("纽约 5-24 零点 = 东八区 5-24 12:00(修复前实测返回 5-24T00:00, 参数被丢弃)")
				.isEqualTo(LocalDateTime.of(2021, 5, 24, 12, 0));
		assertThat(DateUtils.toLocalDateTimeCTT(date, ZoneId.of("UTC")))
				.as("UTC 5-24 零点 = 东八区 5-24 08:00")
				.isEqualTo(LocalDateTime.of(2021, 5, 24, 8, 0));
	}

	// ---------------- P2-24 null 语义统一 ----------------

	@Test
	public void testToLocalDateTimeOfNullLocalDateReturnsNull() {
		//修复前实测抛 NPE(消息 "Cannot invoke LocalDate.atStartOfDay..."),
		//而同族 toLocalDate(Date)/toLocalDateTime(Date) null 都返回 null——统一为 null→null
		assertThat(DateUtils.toLocalDateTime((LocalDate) null)).isNull();
	}

	@Test
	public void testZoneIdNullFailsWithNamedMessage() {
		//修复前 Objects.nonNull(zoneId) 是空操作(只返回布尔不抛异常), null 穿透到 JDK 内部
		//才抛 NPE, 实测 message="zone" 看不出是哪个入参。改 requireNonNull 后主语明确。
		Date now = new Date();
		assertThatThrownBy(() -> DateUtils.toLocalDate(now, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("zoneId");
		assertThatThrownBy(() -> DateUtils.toLocalDateTime(now, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("zoneId");
	}

	// ---------------- P2-36 全局表降为 private ----------------

	@Test
	public void testTimeZoneLocaleMapIsPrivateAndUnmodifiable() throws Exception {
		Field f = DateConstants.class.getDeclaredField("TIME_ZONE_LOCALE_HASH_MAP");
		assertThat(Modifier.isPublic(f.getModifiers()))
				.as("public 全局表任意调用方可读写, 必须降为包内可见并只经 Holder 取值")
				.isFalse();
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<String, Locale> map = (Map<String, Locale>) f.get(null);
		assertThatThrownBy(map::clear).isInstanceOf(UnsupportedOperationException.class);
		assertThat(map.get("GMT")).isEqualTo(Locale.ENGLISH);
	}
}
