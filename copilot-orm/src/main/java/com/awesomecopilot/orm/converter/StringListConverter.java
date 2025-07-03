package com.awesomecopilot.orm.converter;

import com.awesomecopilot.common.lang.utils.StringUtils;
import com.awesomecopilot.orm.utils.JsonUtils;

import javax.persistence.AttributeConverter;
import java.util.Arrays;
import java.util.List;

/**
 * 数据库字段值为字符串到Java Bean属性值List类型的转换
 * <p/>
 * Copyright: Copyright (c) 2025-04-04 9:04
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class StringListConverter implements AttributeConverter<List<String>, String> {
	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		if (attribute == null) {
			return null;
		}
		return JsonUtils.toJson(attribute);
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		dbData = dbData.trim();
		if (dbData.startsWith("[") && dbData.endsWith("]")) {
			return (List<String>) JsonUtils.toList(dbData, String.class);
		} else {
			String[] split = StringUtils.split(dbData);
			return Arrays.asList(split);
		}
	}
}
