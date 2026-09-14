package com.awesomecopilot.json;

import com.awesomecopilot.json.jackson.JacksonUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归: java.util.Date 序列化/反序列化往返必须保留毫秒.
 * <p>
 * 旧配置 setDateFormat("yyyy-MM-dd HH:mm:ss") 是秒级, toJson->toObject 一圈毫秒被吃掉,
 * 时间静默偏移最多999ms(实测 1726286400123 -> 1726286400000). 见 copilot-json 审计报告 P1-2.
 * 同时校验旧的秒级字符串仍能解析(向后兼容).
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class JacksonUtilsDateTest {

	public static class Box {
		private Date date;

		public Box() {}

		public Box(Date date) {
			this.date = date;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}

	@Test
	public void dateMillisSurviveRoundTrip() {
		long millis = 1726286400123L; // 末尾 123 毫秒
		String json = JacksonUtils.toJson(new Box(new Date(millis)));
		Box back = JacksonUtils.toObject(json, Box.class);
		assertEquals(millis, back.getDate().getTime(), "Date 毫秒在往返中丢失: " + json);
	}

	@Test
	public void legacySecondPrecisionStringStillParses() {
		// 历史数据是不带毫秒的秒级串, 必须仍能正确解析(识别链兼容)
		Box legacy = JacksonUtils.toObject("{\"date\":\"2024-09-14 12:00:00\"}", Box.class);
		assertEquals(1726286400000L, legacy.getDate().getTime());
	}

	@Test
	public void dateInsideMapRoundTrips() {
		Date d = new Date(1726286400456L);
		String json = JacksonUtils.toJson(Map.of("t", d));
		Map<String, Object> back = JacksonUtils.toMap(json);
		// toMap 拿到的是字符串, 校验毫秒确实写进了串里
		assertInstanceOf(String.class, back.get("t"));
		assertTrue(((String) back.get("t")).endsWith(".456"),
				"Map 内 Date 序列化应带毫秒, 实际=" + back.get("t"));
	}
}
