package com.awesomecopilot.search8x.support;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.annotation.Field;
import com.awesomecopilot.search8x.annotation.Index;
import com.awesomecopilot.search8x.enums.Analyzer;
import com.awesomecopilot.search8x.enums.FieldType;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static com.awesomecopilot.search8x.enums.FieldType.TEXT;

/**
 * 从Entity上的@Index和@Field注解抽取Mapping信息
 * <p>
 * Copyright: (C), 2021-05-06 14:09
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class MappingSupport {
	
	/**
	 * 从Entity上的@Index和@Field注解抽取Mapping信息
	 *
	 * @param entity 标注了@Index注解的POJO
	 * @return Map<String, Object> 包含dynamic, _source, properties等mapping信息
	 */
	public static Map<String, Object> extractIndexMapping(Class entity) {
		if (entity == null) {
			return null;
		}
		
		Index index = (Index) entity.getAnnotation(Index.class);
		if (index == null) {
			return null;
		}
		
		// 索引级别Mapping设置
		String dynamic = index.dynamic().toString();
		boolean sourceEnabled = index.sourceEnabled();
		
		Map<String, Object> mapping = new HashMap<>();
		mapping.put("dynamic", dynamic);
		
		Map<String, Object> source = new HashMap<>();
		source.put("enabled", sourceEnabled);
		mapping.put("_source", source);
		
		// 获取字段级别的Mapping设置
		Map<String, Object> properties = new HashMap<>();
		Set<java.lang.reflect.Field> fields = ReflectionUtils.filterFieldByAnnotation(entity, Field.class);
		for (java.lang.reflect.Field field : fields) {
			Field annotation = field.getAnnotation(Field.class);
			// ES中的字段名
			String fieldName = annotation.value();
			// 如果没有指定ES字段名, 那么默认取Entity的字段名
			if (isBlank(fieldName)) {
				fieldName = field.getName();
			}
			FieldType fieldType = determineFieldType(annotation.type(), field);
			
			Map<String, Object> fieldMapping = new HashMap<>();
			fieldMapping.put("type", fieldType.toString());
			
			// 只有TEXT类型的字段才需要分词
			if (fieldType == TEXT) {
				Analyzer analyzer = annotation.analyzer();
				Analyzer searchAnalyzer = annotation.searchAnalyzer();
				if (analyzer != null && analyzer != Analyzer.STANDARD) {
					fieldMapping.put("analyzer", analyzer.toString());
				}
				if (searchAnalyzer != null && searchAnalyzer != Analyzer.STANDARD) {
					fieldMapping.put("search_analyzer", searchAnalyzer.toString());
				}
			}
			
			String copyTo = annotation.copyTo();
			if (isNotBlank(copyTo)) {
				fieldMapping.put("copy_to", copyTo);
			}
			
			boolean eagerGlobalOrdinals = annotation.eagerGlobalOrdinals();
			if (eagerGlobalOrdinals) {
				fieldMapping.put("eager_global_ordinals", true);
			}
			
			boolean enabled = annotation.enabled();
			if (!enabled) {
				fieldMapping.put("enabled", false);
			}
			
			boolean enableIndex = annotation.index();
			if (!enableIndex) {
				fieldMapping.put("index", false);
			}
			
			String nullValue = annotation.nullValue();
			if (isNotBlank(nullValue)) {
				fieldMapping.put("null_value", nullValue);
			}
			
			boolean store = annotation.store();
			if (store) {
				fieldMapping.put("store", true);
			}
			
			String format = annotation.format();
			if (isNotBlank(format)) {
				fieldMapping.put("format", format);
			}
			
			properties.put(fieldName, fieldMapping);
		}
		
		mapping.put("properties", properties);
		return mapping;
	}
	
	private static boolean isNotBlank(String str) {
		return str != null && !str.trim().isEmpty();
	}
	
	private static FieldType determineFieldType(FieldType fieldType, java.lang.reflect.Field field) {
		// 因为FieldType.TEXT是默认值, 如果拿到的fieldType不是TEXT类型, 说明显式设置过, 不需要动态判断
		if (fieldType != TEXT) {
			return fieldType;
		}
		
		// 如果fieldType是TEXT类型, 并且字段是String类型, 那么不需要动态判断
		if (field.getType() == String.class) {
			return fieldType;
		}
		
		Class<?> type = field.getType();
		if (type == Integer.class) {
			return FieldType.INTEGER;
		}
		
		if (type == Long.class) {
			return FieldType.LONG;
		}
		
		if (type == LocalDateTime.class || type == Date.class) {
			return FieldType.DATE;
		}
		
		if (type == Double.class) {
			return FieldType.DOUBLE;
		}
		
		return FieldType.KEYWORD;
	}
}
