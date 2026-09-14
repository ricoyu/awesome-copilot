package com.awesomecopilot.json.jackson;

import com.awesomecopilot.json.ObjectMapperDecorator;
import com.awesomecopilot.json.exception.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
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
	
	/**
	 * 美化输出用的 ObjectWriter. ObjectWriter 是不可变的线程安全对象, 复用避免每次 writeValue 都新建;
	 * 惰性初始化: 全局 mapper 在 static 块中才装配完, 类加载期直接 writerWithDefaultPrettyPrinter() 会拿到未装饰的实例.
	 */
	private static volatile ObjectWriter prettyWriter;
	
	static {
		ObjectMapperDecorator decorator = new ObjectMapperDecorator();
		objectMapper = ObjectMapperFactory.createOrFromBeanFactory();
		decorator.decorate(objectMapper);
	}
	
	private static ObjectWriter prettyWriter() {
		ObjectWriter w = prettyWriter;
		if (w == null) {
			synchronized (JacksonUtils.class) {
				if (prettyWriter == null) {
					prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();
				}
				w = prettyWriter;
			}
		}
		return w;
	}
	
	/**
	 * 日志里引用JSON报文用的摘要: 长度 + hashCode + 前128字符.
	 * 失败路径禁止把原始JSON整串打进日志——2MB的坏报文单次 log.error 实测425ms(同步appender),
	 * 坏数据风暴时会把业务线程全拖进日志IO.
	 */
	private static String jsonSummary(String json) {
		if (json == null) {
			return "null";
		}
		int previewLength = 128;
		String preview = json.length() <= previewLength ? json : json.substring(0, previewLength);
		return "length=" + json.length()
				+ ", hash=" + Integer.toHexString(json.hashCode())
				+ ", preview=" + preview;
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
			log.error("将JSON串[{}]转成{}失败", jsonSummary(json), clazz.getName(), e);
			throw new JacksonException(e);
		}
	}
	
	public static <T> T toObject(byte[] src, Class<T> clazz) {
		try {
			return objectMapper.readValue(src, clazz);
		} catch (IOException e) {
			log.error("将JSON字节数组转成{}失败", clazz.getName(), e);
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
	 * <p>
	 * 注意: 泛型参数只是摆设——Jackson拿不到运行期的 T, value 实际类型由JSON内容决定
	 * (嵌套对象是 LinkedHashMap, 数组是 ArrayList). {@code Map<String, User> users = toMap(json)}
	 * 编译能过, 取出来强转 User 会 ClassCastException. 需要具体类型请用
	 * {@code toObject(json, objectMapper.getTypeFactory().constructMapType(...))} 或扩展本方法收 TypeReference.
	 *
	 * @param json
	 * @return
	 */
	public static <T> Map<String, T> toMap(String json) {
		if (isBlank(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, T>>() {
			});
		} catch (IOException e) {
			log.error("将JSON串[{}]转成Map失败", jsonSummary(json), e);
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
	 * <p>
	 * 注意: 同 {@link #toMap(String)}, K/V 泛型参数是摆设, 实际返回的 Map 键永远是 String(JSON规范),
	 * 值是 Jackson 按内容推断的 LinkedHashMap/ArrayList/String/Number/Boolean/null.
	 *
	 * @param json
	 * @return
	 */
	public static <K, V> Map<K, V> toGenericMap(String json) {
		if (isBlank(json)) {
			return emptyMap();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<K, V>>() {
			});
		} catch (IOException e) {
			log.error("将JSON串[{}]转成Map失败", jsonSummary(json), e);
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
			log.error("Parse json array [{}] to List of type {} failed", jsonSummary(jsonArray), clazz, e);
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
			log.error("对象{}序列化成JSON失败", object.getClass().getName(), e);
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
			log.error("对象{}序列化成字节数组失败", object.getClass().getName(), e);
			throw new JacksonException(e);
		}
	}
	
	/**
	 * 美化输出. 大对象慎用: 结果整串驻留内存, 内部还有树转换, 实测10万元素列表堆放大约11倍;
	 * 要输出大对象请走流式 {@link #writeValue(Writer, Object)}.
	 */
	public static <T> String toPrettyJson(T object) {
		if (object == null) {
			return null;
		}
		
		try {
			if (object instanceof String) {
				// 原来用 org.json 全量 parse 再 toString(2): 同一份数据树+字符串多份驻留,
				// 且坏串抛 org.json.JSONException, 与本类其余方法的 JacksonException 不一致.
				// 改走全局 mapper: readTree -> 缓存的 prettyWriter, 异常类型统一.
				JsonNode node = objectMapper.readTree((String) object);
				return prettyWriter().writeValueAsString(node);
			}
			return prettyWriter().writeValueAsString(object);
		} catch (JsonProcessingException e) {
			log.error("美化输出JSON失败", e);
			throw new JacksonException(e);
		}
	}
	
	public static void writeValue(Writer writer, Object value) {
		try {
			objectMapper.writeValue(writer, value);
		} catch (IOException e) {
			log.error("流式写出JSON失败", e);
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
			log.error("JSON解析成树失败, json=[{}]", jsonSummary(json), e);
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
			// 调用方要的就是布尔结果, 非法JSON是业务常态(校验失败), 打整串堆栈是纯噪音;
			// 排查需要时开DEBUG看摘要.
			if (log.isDebugEnabled()) {
				log.debug("非法JSON, json=[{}], 原因: {}", jsonSummary(json), e.getOriginalMessage());
			}
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
	 * <p>
	 * 实现改为 {@code objectMapper.writeValueAsString}, 转义交给Jackson一次完成:
	 * 旧版手工 replace 有两个洞——反斜杠不转义(输入 {@code a\b} 产出非法JSON),
	 * 换行符是直接删掉的(内容丢失), 现在会转义成 \n 保留.
	 * <p>
	 * null 入参返回字符串 "null"(旧版是 NPE), 与 JSON 规范的 null 字面量一致.
	 *
	 * @param jsonStr 原始 JSON 字符串
	 * @return 转换后的字符串
	 */
	public static String formatJsonString(String jsonStr) {
		try {
			return objectMapper.writeValueAsString(jsonStr);
		} catch (JsonProcessingException e) {
			// String 序列化实测不会失败, 这个分支只是接口契约要求, 真进了就抛出不吞
			log.error("转义JSON字符串失败, json=[{}]", jsonSummary(jsonStr), e);
			throw new JacksonException(e);
		}
	}
}
