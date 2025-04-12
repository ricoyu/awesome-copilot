package com.awesomecopilot.common.lang.reflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class GenericTypeInspector {

	private static Logger log = LoggerFactory.getLogger(GenericTypeInspector.class);

	/**
	 * 找到这个字段的泛型类型
	 *
	 * @param field
	 * @return String
	 */
	public static String inspectGenericTypes(Field field) {
		if (List.class.isAssignableFrom(field.getType())) {
			Type genericType = field.getGenericType();

			if (genericType instanceof ParameterizedType) {
				ParameterizedType paramType = (ParameterizedType) genericType;
				Type[] typeArguments = paramType.getActualTypeArguments();

				if (typeArguments.length > 0) {
					log.info("泛型类型: " + typeArguments[0].getTypeName());
					return typeArguments[0].getTypeName();
				}
			}
		}
		return "";
	}
}
