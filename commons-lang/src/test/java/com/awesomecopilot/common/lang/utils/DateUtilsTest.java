package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.constants.DateConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.awesomecopilot.common.lang.utils.DateUtils.toLocalDateTime;
import static java.text.MessageFormat.format;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateUtilsTest {

	@BeforeEach
	public void setUp() {
		// 初始化操作，如果有需要的话
	}

	@Test
	public void testUTCDate() {
		String utcDateStr = "2022-10-31T02:21:32.493Z";
		Date date = DateUtils.parse(utcDateStr);
		assertNotNull(date);
		assertEquals(1667182892493L, date.getTime());
	}

	@Test
	public void testDate() {
		// 验证日期是否正确
		Date expectedDate = new Date(1629045321942L);
		assertEquals(expectedDate, new Date(1629045321942L));

		// 验证时区是否正确
		ZoneId zoneId = ZoneId.of("+08:00"); // 固定偏移量时区
		ZoneId zoneId1 = ZoneId.of("Asia/Shanghai"); // 地理区域时区
		ZoneId zoneId2 = TimeZone.getTimeZone("Asia/Shanghai").toZoneId(); // 通过 TimeZone 转换的时区

		// 获取当前时间
		ZonedDateTime now = ZonedDateTime.now();

		// 比较当前时间的偏移量是否相同
		assertEquals(now.withZoneSameInstant(zoneId).getOffset(), now.withZoneSameInstant(zoneId1).getOffset());
		assertEquals(now.withZoneSameInstant(zoneId1).getOffset(), now.withZoneSameInstant(zoneId2).getOffset());

		// 打印偏移量以验证
		System.out.println("ZoneId(+08:00) offset: " + now.withZoneSameInstant(zoneId).getOffset());
		System.out.println("ZoneId(Asia/Shanghai) offset: " + now.withZoneSameInstant(zoneId1).getOffset());
		System.out.println("ZoneId(TimeZone Asia/Shanghai) offset: " + now.withZoneSameInstant(zoneId2).getOffset());
	}

	@Test
	public void testStartUpTimes() throws ClassNotFoundException {
		long begin = System.nanoTime();
		//Class.forName("com.awesomecopilot.common.lang.utils.DateUtils");
		long end = System.nanoTime();
		long duration = (end - begin) / 1000000;
		assertTrue(duration >= 0);
	}

	@Test
	public void testParse() {
		String startDateTime = "2021-03-18 13:30:47";
		String endDateTime = "2021-03-19 13:45:47";

		Date date = DateUtils.parse("2021-07-22 10:58:17");
		assertNotNull(date);

		long startTime = DateUtils.toEpochMilis(startDateTime);
		long endTime = DateUtils.toEpochMilis(endDateTime);
		assertEquals(1616045447000L, startTime);
		assertEquals(1616132747000L, endTime);
	}

	@Test
	public void testReverseParse() {
		LocalDateTime startDateTime = DateUtils.toLocalDateTime(1615132800000L);
		LocalDateTime endDateTime = DateUtils.toLocalDateTime(1615737599000L);
		assertEquals(LocalDateTime.of(2021, 3, 8, 0, 0), startDateTime);
		assertEquals(LocalDateTime.of(2021, 3, 14, 23, 59, 59), endDateTime);
	}

	@Test
	public void testDynamicParse() throws ParseException {
		String dateStr1 = "2020-12-23T12:51:18.456019+0200";
		String dateStr2 = "2020-12-23T12:51:18.456+0200";
		String dateStr3 = "2020-12-23T12:51:18.456+0800";
		String dateStr4 = "2020-12-23 12:51:18.456+0800";
		String dateStr5 = "2020-12-23 12:51:18+0800";
		String dateStr6 = "2020-12-23 12:51:18";

		List<String> dates = asList(dateStr1, dateStr2, dateStr3, dateStr4, dateStr5, dateStr6);
		Pattern pattern = Pattern.compile("(\\d{2,4})-(\\d{1,2})-(\\d{1,2})(T?)\\s*(\\d{1,2}):(\\d{1,2}):(\\d{1,2})\\.?(\\d*)(\\+\\d+)?");
		for (String dateStr : dates) {
			Matcher matcher = pattern.matcher(dateStr);
			assertTrue(matcher.matches());
		}
	}

	@Test
	public void testReverse() {
		Date date = new Date(1611715800000L);
		String dateStr = DateUtils.format(date);
		Date date1 = DateUtils.parse(dateStr);
		assertEquals(date.getTime(), date1.getTime());
	}

	@Test
	public void testLocalDateTimeToGMT() {
		LocalDateTime now = LocalDateTime.now();
		ZonedDateTime zonedDateTime = now.atZone(ZoneId.systemDefault());
		ZonedDateTime utc = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
		assertEquals(now, utc.toLocalDateTime().plus(8, HOURS));
	}

	@Test
	public void testDateDiff() {
		LocalDate begin = LocalDate.of(2021, 5, 25);
		LocalDate end = LocalDate.of(2021, 5, 18);
		assertEquals(7, DateUtils.dateDiff(begin, end));
		assertEquals(-7, DateUtils.dateDiff(end, begin));
		assertEquals(0, DateUtils.dateDiff(begin, LocalDate.of(2021, 5, 25)));
	}

	@Test
	public void testUTCParse() {
		String dateStr = "08/03/2019T16:20:17:717 UTC+05:30";
		Date date = DateUtils.parse(dateStr);
		assertNotNull(date);
	}

	@Test
	public void testFMTGMTparse() throws ParseException {
		String dateStr = "Sun, 06 Nov 1994 08:49:37 GMT";
		Date date = DateUtils.parse(dateStr);
		assertNotNull(date);

		dateStr = "Mon, 16 Apr 2018 00:00:00 GMT+08:00";
		Date date2 = DateUtils.parse(dateStr);
		assertEquals(16, date2.getDate());
		assertEquals(3, date2.getMonth());
		assertEquals(0, date2.getHours());
		assertEquals(0, date2.getMinutes());
		assertEquals(0, date2.getSeconds());
	}

	@Test
	public void testGMT() throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date expectedDate1 = format.parse("Mon, 03 Jun 2013 07:01:29 GMT");
		Date expectedDate2 = format.parse("Mon, 16 Apr 2018 00:00:00 GMT+08:00");
		assertEquals(expectedDate1, DateUtils.parse("Mon, 03 Jun 2013 07:01:29 GMT"));
		assertEquals(expectedDate2, DateUtils.parse("Mon, 16 Apr 2018 00:00:00 GMT+08:00"));
	}

	@Test
	public void testParseJenkinsDateFormat() {
		String dateStr = "2021-06-01T14:22:04+08:00";
		LocalDateTime date1 = DateUtils.toLocalDateTime(dateStr);
		Date date2 = DateUtils.parse(dateStr);
		assertEquals(date2.getHours(), date1.getHour());
		assertEquals(date1.getDayOfMonth(), date2.getDate());
	}

	@Test
	public void testCurrentMillis() {
		long currentTime = System.currentTimeMillis();
		assertTrue(currentTime > 0);
	}

	@Test
	public void testParseUTC() {
		String dateStr = "2018-05-30T14:24:48.933+0800";
		LocalDateTime date1 = DateUtils.toLocalDateTime(dateStr);
		Date date2 = DateUtils.parse(dateStr);
		assertEquals(date2.getHours(), date1.getHour());
		assertEquals(date1.getDayOfMonth(), date2.getDate());
	}

	@Test
	public void testToLocaldate() {
		Date date = new Date(121, 7, 20); // 注意：Date的年份是从1900年开始的
		LocalDate localDate = DateUtils.toLocalDate(date);
		assertEquals(LocalDate.of(2021, 8, 20), localDate);
	}

	@Test
	public void testToMillis() {
		LocalDateTime begin = LocalDateTime.of(2021, 7, 28, 10, 41, 0);
		LocalDateTime end = LocalDateTime.of(2021, 7, 28, 10, 43, 2);
		assertEquals(1627440060000L, DateUtils.toEpochMilis(begin).longValue());
		assertEquals(1627440182000L, DateUtils.toEpochMilis(end).longValue());
	}

	@Test
	public void testMillisToDate() {
		LocalDateTime localDateTime = toLocalDateTime(1748753330966l);
		System.out.println(localDateTime);
	}

	@Test
	public void testParseESDateFormat() {
		Date date = DateUtils.parse("2018-07-24T10:29:48.103Z");
		assertNotNull(date);
	}

	@Test
	public void testParse2() {
		String dateStr = "Sun, 31 Dec 2023 20:31:01 +0800";
		Date date = DateUtils.parse(dateStr);
		assertNotNull(date);
	}

	@Test
	public void testFormat() {
		String format = DateUtils.format(new Date(), "yyyyMMddHHmmssS");
		assertNotNull(format);
	}

	@Test
	public void testRfc1123() {
		Date now = new Date();
		String dateStr = DateUtils.format(now, DateConstants.FMT_RFC1123_FORMAT);
		assertNotNull(dateStr);
	}

	@Test
	public void test() {
		String template = """
				{
				"核心线程数(corePoolSize)"        : %d,
				"最大线程数(maximumPoolSize)"     : %d,
				"当前线程数(poolSize)"            : %d,
				"活跃线程数(activeCount)"         : %d,
				"已完成任务数(completedTaskCount)": %d,
				"总任务数(taskCount)"             : %d,
				"当前队列大小(queue.size)"         : %d,
				"队列剩余容量(queue.remaining)"    : %d,
				"保持存活时间 (keepAliveTime)"     : %d秒,
				"允许核心线程超时"                  : %d
				}""";

		String msg2 = String.format(template, 1,
				2,
				3,
				4,
				5,
				6,
				7,
				8,
				9,
				10);
		System.out.print(msg2);
		String msg = format(template, "1",
				2,
				3,
				4,
				5,
				6,
				7,
				8,
				9,
				10);
		System.out.println(msg);
	}
}