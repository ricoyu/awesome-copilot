package com.awesomecopilot.orm.utils;

import com.awesomecopilot.orm.exception.JacksonException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Jackson工具类
 * <p>
 * Copyright: Copyright (c) 2017-10-30 13:13
 * <p>
 * Company: DataSense
 * <p>
 * @author Rico Yu	ricoyu520@gmail.com
 * @version 1.0
 * @on
 */
public final class JsonUtils {

	private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(ofPattern("yyyy-MM-dd HH:mm:ss")));
		javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(ofPattern("yyyy-MM-dd")));
		javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(ofPattern("HH:mm:ss")));
		javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(ofPattern("yyyy-MM-dd HH:mm:ss")));
		javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(ofPattern("yyyy-MM-dd")));
		javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(ofPattern("HH:mm:ss")));
		mapper.registerModule(javaTimeModule);
		mapper.registerModule(new Hibernate6Module());

		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
				.withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	}

	/**
	 * 将对象转成json串
	 * 
	 * @param object
	 * @return String
	 */
	public static <T> String toJson(T object) {
		if (object == null) {
			return null;
		}
		String json = null;
		try {
			json = mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
		return json;
	}

	public static <T> String toPrettyJson(T object) {
		if (object == null) {
			return null;
		}
		String json = null;
		try {
			json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
		return json;
	}


	public static <T> List<T> toList(String jsonArray, Class<T> clazz) {
		if (isBlank(jsonArray)) {
			return emptyList();
		}
		CollectionType javaType = mapper.getTypeFactory()
				.constructCollectionType(List.class, clazz);
		try {
			return mapper.readValue(jsonArray, javaType);
		} catch (IOException e) {
			log.error("Parse json array \n{} \n to List of type {} failed", jsonArray, clazz, e);
			throw new JacksonException(e);
		}
	}
}