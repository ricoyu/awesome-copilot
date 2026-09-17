package com.awesomecopilot.web.context.support;

import com.awesomecopilot.web.converter.GenericEnumConverter;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CustomConversionService 枚举转换缓存测试。
 * <p>
 * 评审报告 P1-3：旧实现覆写 getConverter 后, 凡目标类型是枚举就 new 一个
 * GenericEnumConverter 返回, 每次枚举绑定都白白新建对象。修复后同一个
 * (source,target) 查找必须复用同一个 converter 实例。
 * <p>
 * Copyright: Copyright (c) 2026-09-17
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CustomConversionServiceTest {

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

	/**
	 * 记录 getConverter 枚举分支每次实际返回的 converter 实例(按对象身份比较)
	 */
	private static class RecordingConversionService extends CustomConversionService {
		final List<GenericConverter> returned = new ArrayList<>();

		@Override
		protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
			GenericConverter converter = super.getConverter(sourceType, targetType);
			if (Enum.class.isAssignableFrom(targetType.getObjectType())
					&& converter instanceof GenericEnumConverter) {
				returned.add(converter);
			}
			return converter;
		}
	}

	@Test
	public void testSameConverterInstanceReusedAcrossLookups() {
		RecordingConversionService cs = new RecordingConversionService();
		cs.setProperties(Set.of("code"));
		cs.afterPropertiesSet();

		TypeDescriptor stringType = TypeDescriptor.valueOf(String.class);
		for (int i = 0; i < 5; i++) {
			cs.getConverter(stringType, TypeDescriptor.valueOf(Grade.class));
		}

		assertEquals(5, cs.returned.size(), "5 次查找都应命中枚举分支");
		assertEquals(1, distinctByIdentity(cs.returned).size(),
				"5 次查找必须复用同一个实例(按对象身份), 不允许每次新建");
	}

	private static Set<GenericConverter> distinctByIdentity(List<GenericConverter> list) {
		Set<GenericConverter> set = Collections.newSetFromMap(new IdentityHashMap<>());
		set.addAll(list);
		return set;
	}

	@Test
	public void testPropertiesChangeInvalidatesCachedConverter() {
		RecordingConversionService cs = new RecordingConversionService();
		cs.setProperties(Set.of("code"));
		cs.afterPropertiesSet();

		TypeDescriptor stringType = TypeDescriptor.valueOf(String.class);
		GenericConverter before = cs.getConverter(stringType, TypeDescriptor.valueOf(Grade.class));
		cs.setProperties(Set.of("desc"));
		GenericConverter after = cs.getConverter(stringType, TypeDescriptor.valueOf(Grade.class));

		assertTrue(before != after, "setProperties 之后必须重建枚举 converter, 否则新配置不生效");
	}

	@Test
	public void testEnumConversionStillWorksAfterFix() {
		CustomConversionService cs = new CustomConversionService();
		cs.setProperties(Set.of("code"));
		cs.afterPropertiesSet();

		TypeDescriptor stringType = TypeDescriptor.valueOf(String.class);
		// 按配置的 code 属性匹配
		assertEquals(Grade.B, cs.convert("102", stringType, TypeDescriptor.valueOf(Grade.class)));
		// 属性匹配不到时回退 name 匹配
		assertEquals(Grade.A, cs.convert("A", stringType, TypeDescriptor.valueOf(Grade.class)));
	}

	@Test
	public void testFactoryBeanRegistersStringToArrayConverter() {
		CustomConversionServiceFactoryBean factoryBean = new CustomConversionServiceFactoryBean();
		factoryBean.setProperties(Set.of("code"));
		factoryBean.afterPropertiesSet();

		Object converted = factoryBean.getObject().convert("a,b,c",
				TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(String[].class));
		assertTrue(converted instanceof String[]);
		assertEquals(3, ((String[]) converted).length);
		assertEquals(List.of("a", "b", "c"), Arrays.asList((String[]) converted));
	}
}
