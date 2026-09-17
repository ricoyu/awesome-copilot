package com.awesomecopilot.web.converter;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ObjectToEnumConverterFactory 行为测试。
 * <p>
 * 评审报告 P1-6：旧实现只遍历配置的 properties 做属性匹配, 匹配不到直接返回 null,
 * 不像 GenericEnumConverter 那样回退到按 name / ordinal 匹配——不配 properties 时
 * 这个工厂"注册了却什么都不转"。修复后属性匹配不到必须回退 name/ordinal。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ObjectToEnumConverterFactoryTest {

	public enum Grade {
		A(101, "优"),
		B(102, "良");

		private final int code;
		private final String desc;

		Grade(int code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public int getCode() {
			return code;
		}

		public String getDesc() {
			return desc;
		}
	}

	@Test
	public void testPropertyMatchStillWorks() {
		ObjectToEnumConverterFactory factory = new ObjectToEnumConverterFactory();
		factory.setProperties(Set.of("code"));

		assertEquals(Grade.B, factory.getConverter(Grade.class).convert("102"));
	}

	@Test
	public void testFallsBackToNameWhenNoPropertiesConfigured() {
		ObjectToEnumConverterFactory factory = new ObjectToEnumConverterFactory();

		// 修复前: properties 为空集, 任何输入都返回 null
		assertEquals(Grade.A, factory.getConverter(Grade.class).convert("A"));
	}

	@Test
	public void testFallsBackToOrdinalForNumber() {
		ObjectToEnumConverterFactory factory = new ObjectToEnumConverterFactory();

		assertEquals(Grade.B, factory.getConverter(Grade.class).convert(1));
	}

	@Test
	public void testUnmatchedValueReturnsNull() {
		ObjectToEnumConverterFactory factory = new ObjectToEnumConverterFactory();
		factory.setProperties(Set.of("code"));

		assertNull(factory.getConverter(Grade.class).convert("不存在"));
	}

	@Test
	public void testEmptyStringThrowsInsteadOfSilentNull() {
		ObjectToEnumConverterFactory factory = new ObjectToEnumConverterFactory();

		// 探针实测: EnumUtils 内部 lookup(String) 对空白值抛 IllegalArgumentException,
		// 与 GenericEnumConverter 路径(被Spring包成ConversionFailedException)行为一致。
		// 固化该语义: 空串不是"匹配不到的普通值", 而是显式报错, 防止将来有人改成悄悄返回null。
		assertThrows(IllegalArgumentException.class,
				() -> factory.getConverter(Grade.class).convert(""));
	}
}
