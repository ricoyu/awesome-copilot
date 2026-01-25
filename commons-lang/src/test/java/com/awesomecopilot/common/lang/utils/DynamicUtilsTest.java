package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class DynamicUtilsTest {

	@Test
	public void testSubObject() {
		Map<String, Object> properties = new HashMap<>();
		long brandId = 1L;
		properties.put("brandId", brandId);
		PmsCategoryBrandRelationDTO dto = new PmsCategoryBrandRelationDTO();
		dto.setBrandId(666L);
		properties.put("dto", dto);
		Object object = DynamicUtils.createObject(Object.class, properties);
		Object value = ReflectionUtils.invokeMethod("getBrandId", object);
		System.out.println(value);
		ReflectionUtils.invokeMethod("setBrandId", object, 2L);
		value = ReflectionUtils.invokeMethod("getBrandId", object);
		System.out.println(value);
		PmsCategoryBrandRelationDTO obj = ReflectionUtils.invokeMethod("getDto", object);
		System.out.println(value);
	}

	@Test
	public void testSubObject2() {
		Map<String, Object> properties = new HashMap<>();
		Object object = DynamicUtils.createObject(Object.class, properties);
		System.out.println(object);
	}

	@Test
	public void testSubObject3() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("name", null);
		Object object = DynamicUtils.createObject(Object.class, properties);
		System.out.println(object);
	}

}
