package com.awesomecopilot.common.spring.utils;

import com.awesomecopilot.common.lang.utils.DynamicUtils;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpElTest {

	@Test
	public void test1() {
		String spel ="category_brands_#{pmsCategoryBrandRelationDTO.brandId}";

		PmsCategoryBrandRelationDTO dto = new PmsCategoryBrandRelationDTO();
		dto.setBrandId(1L);
		dto.setBrandName("测试");
		dto.setCatelogName("测试");
		Map<String, Object> properties = new HashMap<>();
		properties.put("pmsCategoryBrandRelationDTO", dto);
		Object rootObject = DynamicUtils.createObject(Object.class, properties);

		String value = SpElUtils.parse(spel, rootObject);
		assertEquals("category_brands_1", value);
	}

	@Test
	public void test2() {
		String spel ="category_brands_#{brandId}";

		PmsCategoryBrandRelationDTO dto = new PmsCategoryBrandRelationDTO();
		dto.setBrandId(1L);
		dto.setBrandName("测试");
		dto.setCatelogName("测试");
		Map<String, Object> properties = new HashMap<>();
		properties.put("brandId", 666);
		Object rootObject = DynamicUtils.createObject(Object.class, properties);

		String value = SpElUtils.parse(spel, rootObject);
		assertEquals("category_brands_666", value);
	}

	@Data
	public static class PmsCategoryBrandRelationDTO {

		private Long id;

		private Long brandId;

		private Long catelogId;

		private String brandName;

		private String catelogName;
	}
}
