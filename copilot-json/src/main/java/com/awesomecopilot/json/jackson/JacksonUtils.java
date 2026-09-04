package com.awesomecopilot.json.jackson;

import com.awesomecopilot.json.ObjectMapperDecorator;
import com.awesomecopilot.json.exception.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Jackson工具类
 * <p>
 * Copyright: Copyright (c) 2017-10-30 13:13
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public final class JacksonUtils {
	
	private static final Logger log = LoggerFactory.getLogger(JacksonUtils.class);
	
	private static ObjectMapper objectMapper = null;
	
	static {
		ObjectMapperDecorator decorator = new ObjectMapperDecorator();
		objectMapper = ObjectMapperFactory.createOrFromBeanFactory();
		decorator.decorate(objectMapper);
	}
	
	/**
	 * 将json字符串转成指定对象
	 *
	 * @param json
	 * @param clazz
	 * @return T
	 */
	public static <T> T toObject(String json, Class<T> clazz) {
		if (isBlank(json)) {
			return null;
		}
		
		try {
			return objectMapper.readValue(json, clazz);
		} catch (IOException e) {
			log.error("将JSON串\n{}\n转成{}失败", json, clazz.getName());
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}
	
	public static <T> T toObject(byte[] src, Class<T> clazz) {
		try {
			return objectMapper.readValue(src, clazz);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}
	
	/**
	 * Map转POJO
	 *
	 * @param map
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public static <T> T mapToPojo(Map map, Class<T> clazz) {
		return objectMapper.convertValue(map, clazz);
	}
	
	/**
	 * JSON字符串转MAP
	 *
	 * @param json
	 * @return
	 */
	public static <T> Map<String, T> toMap(String json) {
		if (isBlank(json)) {
			return null;
		}
		Map<String, T> map = new HashMap<String, T>();
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, T>>() {
			});
		} catch (IOException e) {
			log.error("将JSON串\n{}\n转成Map失败", json);
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}
	
	/**
	 * POJO转Map
	 *
	 * @param pojo
	 * @param <T>
	 * @return
	 */
	public static <T> Map<String, T> pojoToMap(Object pojo) {
		return objectMapper.convertValue(pojo, new TypeReference<Map<String, T>>() {
		});
	}
	
	/**
	 * JSON字符串转MAP
	 *
	 * @param json
	 * @return
	 */
	public static <K, V> Map<K, V> toGenericMap(String json) {
		if (isBlank(json)) {
			return emptyMap();
		}
		Map<K, V> map = new HashMap<>();
		try {
			return objectMapper.readValue(json, new TypeReference<Map<K, V>>() {
			});
		} catch (IOException e) {
			log.error("将JSON串\n{}\n转成Map失败", json);
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}
	
	public static <T> List<T> toList(String jsonArray, Class<T> clazz) {
		if (isBlank(jsonArray)) {
			return emptyList();
		}
		CollectionType javaType = objectMapper.getTypeFactory()
				.constructCollectionType(List.class, clazz);
		try {
			return objectMapper.readValue(jsonArray, javaType);
		} catch (IOException e) {
			log.error("Parse json array \n{} \n to List of type {} failed", jsonArray, clazz, e);
			throw new JacksonException(e);
		}
	}
	
	/**
	 * 将对象转成json串
	 *
	 * @param object
	 * @param <T>
	 * @return
	 */
	public static <T> String toJson(T object) {
		if (object == null) {
			return null;
		}
		if (object instanceof String) {
			return (String) object;
		}
		try {
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}
	
	public static byte[] toBytes(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof String) {
			return ((String) object).getBytes(UTF_8);
		}
		try {
			return objectMapper.writeValueAsBytes(object);
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}
	
	public static <T> String toPrettyJson(T object) {
		if (object == null) {
			return null;
		}
		
		if (object instanceof String) {
			return new JSONObject((String) object).toString(2);
		}
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}
	
	public static void writeValue(Writer writer, Object value) {
		try {
			objectMapper.writeValue(writer, value);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}
	
	/**
	 * 构建 JSON 对象（对标 Fastjson JSONObject）, 用法示例:
	 * <pre>
	 * ObjectNode json = JacksonUtils.createJsonObject();
	 * json.put("name", "三少爷");
	 * json.put("age", 18);
	 * System.out.println(JacksonUtils.toJson(json));
	 * </pre>
	 * @return ObjectNode
	 */
	public static ObjectNode createJsonObject() {
		return objectMapper.createObjectNode();
	}
	
	/**
	 * 根据JSON字符串生成ObjectNode
	 * @param jsonStr
	 * @return
	 * @throws Exception
	 */
	public static ObjectNode toJsonNode(String jsonStr) throws Exception {
		JsonNode jsonNode = objectMapper.readTree(jsonStr);
		// 校验顶层是否为对象，防止数组/基础类型报错
		if (!jsonNode.isObject()) {
			throw new IllegalArgumentException("传入JSON顶层不是对象，无法转为ObjectNode");
		}
		return (ObjectNode) jsonNode;
	}
	
	/**
	 * 将JSON字符串解析成 ObjectNode JSON对象
	 * @param json
	 * @return ObjectNode
	 */
	public static ObjectNode parseObject(String json) {
		// 3. Jackson 转 ObjectNode（和 fastjson JSON.parseObject 完全对应）
		try {
			return objectMapper.readValue(json, ObjectNode.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("解析json字符串报错: ", e);
		}
	}
	
	/**
	 * 将JSON对象读取成一个JsonNode对象
	 * @param json
	 * @return JsonNode
	 */
	public static JsonNode readTree(String json) {
		try {
			return objectMapper.readTree(json);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new JacksonException(e);
		}
	}

	/**
	 * 判断一个字符串是否是合法的JSON字符串
	 * @param json
	 * @return
	 */
	public static boolean isValidJson(String json) {
		if (isBlank(json)) {
			return false;
		}
        try {
            objectMapper.readTree(json);
			return true;
        } catch (JsonProcessingException e) {
			log.error("", e);
			return false;
		}
    }
	
	public static ObjectMapper objectMapper() {
		return objectMapper;
	}
	
	/**
	 * 运行时动态添加 MixIn。
	 * <p>
	 * 注意：该方法直接修改共享的 {@code objectMapper}，且 {@link ObjectMapper#addMixIn(Class, Class)}
	 * 内部通过复制 AnnotationIntrospector 实现，<b>不保证并发安全</b>。请勿在高并发场景下频繁调用，
	 * 建议在应用启动阶段一次性完成所有 MixIn 注册。
	 *
	 * @param target      目标类型
	 * @param mixinSource MixIn 源类型
	 */
	public static void addMixIn(Class target, Class mixinSource) {
		objectMapper.addMixIn(target, mixinSource);
	}

	/**
	 * 将给定的 JSON 字符串处理为最外层加上双引号，并且字符串内部的每个双引号前都加上一个反斜杠转义符。
	 *
	 * @param jsonStr 原始 JSON 字符串
	 * @return 转换后的字符串
	 */
	public static String formatJsonString(String jsonStr) {
		// 去除换行符和回车符
		String noNewLines = jsonStr.replace("\n", "").replace("\r", "");
		// 转义内部的双引号
		String escapedJson = noNewLines.replace("\"", "\\\"");
		// 最外层加上双引号
		return "\"" + escapedJson + "\"";
	}
}
