package com.awesomecopilot.orm.converter;

import com.awesomecopilot.common.lang.utils.StringUtils;
import com.awesomecopilot.orm.utils.JsonUtils;
import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.*;

public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {
	@Override
	public String convertToDatabaseColumn(List<Integer> attribute) {
		return JsonUtils.toJson(attribute);
	}

	@Override
	public List<Integer> convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		dbData = dbData.trim();
		if (dbData.startsWith("[") && dbData.endsWith("]")) {
			dbData = dbData.substring(1, dbData.length() - 1);
		}
		String[] split = StringUtils.split(dbData);
		return Arrays.asList(split).stream().map(Integer::parseInt).collect(toList());
	}
}