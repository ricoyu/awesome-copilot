package com.awesomecopilot.json.jsonpath;

import com.awesomecopilot.common.lang.transformer.ValueHandlerFactory;
import com.awesomecopilot.json.JSON;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.json.jsonpath.context.DocumentContext;
import com.awesomecopilot.json.jsonpath.context.JsonContext;
import com.awesomecopilot.json.jsonpath.mapper.JacksonMappingProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * https://github.com/json-path/JsonPath
 * <p>
 * Operator		Description
 * $			The root element to query. This starts all path expressions.
 * json串的根元素,不管json是数组还是对象形式
 *
 * @			The current node being processed by a filter predicate.
 * 代表当前正在处理的item
 * *			Wildcard. Available anywhere a name or numeric are required.
 * ..			Deep scan. Available anywhere a name is required.
 * .<name>		Dot-notated child
 * [start:end]	Array slice operator
 * [?(<expression>)]		Filter expression. Expression must evaluate to a boolean value.
 * 过滤器很有用
 * ['<name>' (, '<name>')]	Bracket-notated child or children
 * [<number> (, <number>)]	Array index or indexes
 * <p>
 * 示例：
 * $.store.book[0].title
 * 或者
 * $['store']['book'][0]['title']
 * @.error 当前节点有没有error子节点
 * <p>
 * Copyright: Copyright (c) 2018-03-16 14:20
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 */
@Slf4j
public final class JsonPathUtils {
    
    private static final Configuration CONFIG;
    
    static {
        JsonProvider jsonProvider = new JacksonJsonProvider();
        MappingProvider mappingProvider = new JacksonMappingProvider(JacksonUtils.objectMapper());
        
        CONFIG = Configuration.builder()
                .jsonProvider(jsonProvider)
                .mappingProvider(mappingProvider)
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
    }
    
    /**
     * 解析 JSON 字符串为 DocumentContext（无缓存）
     */
    private static DocumentContext parseJson(String json) {
        if (isBlank(json)) {
            return null;
        }
        
        try {
            String processedJson = json;
            if (isNotBlank(processedJson)) {
                /*
                 * 特殊处理像
                 */
                processedJson = JSON.cleanup(json);
                if (log.isDebugEnabled()) {
                    log.debug("处理后的JSON: {}", processedJson);
                }
            }
            
            
            Object obj = CONFIG.jsonProvider().parse(processedJson);
            return new JsonContext(obj, CONFIG);
        } catch (Throwable e) {
            log.error("JSON格式有问题, 输入的JSON串为: {}", json, e);
            return null;
        }
    }
    
    /**
     * @param json
     * @param path
     * @return
     */
    public static boolean ifExists(String json, String path) {
        com.jayway.jsonpath.DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return false;
        }
        
        try {
            Object result = ctx.read(path);
            if (result == null) {
                return false;
            }
            if (result instanceof JSONArray) {
                return ((JSONArray) result).length() != 0;
            }
            if (result instanceof Collection) {
                return !((Collection<?>) result).isEmpty();
            }
            return true;
        } catch (Exception e) {
            log.error("读取JSON节点: {} 报错, JSON为: {}", path, json, e);
            return false;
        }
    }
    
    /**
     * 通过Jsonpath读取某个节点，可能返回的是单个对象，也可能是集合对象
     *
     * @param json
     * @param path
     * @return
     */
    public static <T> T readNode(String json, String path) {
        com.jayway.jsonpath.DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }
        
        try {
            return ctx.read(path);
        } catch (Exception e) {
            log.error("读取JSON节点: {} 报错, JSON为: {}", path, json, e);
            return null;
        }
    }
    
    /**
     * 如果路径存在且有值则读取，否则返回null
     * （比原版更高效，因为只 parse 一次）
     */
    public static <T> T readNodeIfExists(String json, String path) {
        com.jayway.jsonpath.DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }
        
        try {
            Object result = ctx.read(path);
            if (result == null) {
                return null;
            }
            if (result instanceof Collection && ((Collection<?>) result).isEmpty()) {
                return null;
            }
            if (result instanceof JSONArray && ((JSONArray) result).length() == 0) {
                return null;
            }
            return (T) result;
        } catch (Exception e) {
            log.error("读取JSON节点: {} 报错, JSON为: {}", path, json, e);
            return null;
        }
    }
    
    /**
     * 通过Jsonpath读取某个节点，并转换为指定类型
     *
     * @param json
     * @param path
     * @param clazz
     * @return
     */
    public static <T> T readNode(String json, String path, Class<T> clazz) {
        com.jayway.jsonpath.DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }
        
        try {
            Object value = ctx.read(path, clazz);
            ValueHandlerFactory.ValueHandler<T> valueHandler = ValueHandlerFactory.determineAppropriateHandler(clazz);
            return valueHandler.convert(value);
        } catch (Exception e) {
            log.error("读取JSON节点: {} 报错, JSON为: {}", path, json, e);
            return null;
        }
    }
    
    public static <T> T readNodeIfExists(String json, String path, Class<T> clazz) {
        com.jayway.jsonpath.DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }
        
        try {
            Object result = ctx.read(path);
            if (result == null || (result instanceof Collection && ((Collection<?>) result).isEmpty())) {
                return null;
            }
            Object value = ctx.read(path, clazz);
            ValueHandlerFactory.ValueHandler<T> valueHandler = ValueHandlerFactory.determineAppropriateHandler(clazz);
            return valueHandler.convert(value);
        } catch (Exception e) {
            log.error("读取JSON节点: {} 报错, JSON为: {}", path, json, e);
            return null;
        }
    }
    
    /**
     * 示例1：给定json数组
     * <pre>{@code
     * [
     *   {
     *     "username": "hawk",
     *     "error": {
     *       "code": 899001,
     *       "message": "user exist"
     *     }
     *   },
     *   {
     *     "username": "ricoyucsd",
     *     "error": {
     *       "code": 899001,
     *       "message": "user exist"
     *     }
     *   },
     *   {
     *     "username": "ricoyussss",
     *     "nickname": "三少爷",
     *     "birthday": "1982-11-09",
     *     "gender": 1,
     *     "avatar": "qiniu/image/j/8D57A18AD6926D6D879DFA36B4ED9CC4.jpg"
     *   }
     * ]
     * }</pre>
     * 分别读取包含/不包含 error属性的元素<p>
     * <pre>{@code
     * JsonPathUtils.readListNode(result, "[?(@.error)].username", String.class)
     * JsonPathUtils.readListNode(result, "[?(!@.error)].username", String.class)
     * }</pre>
     * <p>
     * 示例2：给定json对象
     * <pre>{@code
     * {
     *   "count": 2,
     *   "total": 714,
     *   "start": 0,
     *   "users": [
     *     {
     *       "mtime": "2018-03-16 18:12:41",
     *       "gender": 0,
     *       "username": "96289794xcuqzz",
     *       "ctime": "2018-03-16 18:12:41"
     *     },
     *     {
     *       "mtime": "2018-03-16 18:12:41",
     *       "gender": 0,
     *       "username": "96269528oydxvk",
     *       "ctime": "2018-03-16 18:12:41"
     *     }
     *   ]
     * }
     * }</pre>
     * 取所有的username：$.users[*].username
     *
     * 示例2：取所有的username：$.users[*].username
     */
    public static <T> List<T> readListNode(String json, String path, Class<T> clazz) {
        com.jayway.jsonpath.DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return new ArrayList<>();
        }
        
        try {
            // 先读取为 raw List（通常 List<LinkedHashMap> 或 List<Map>）
            Object rawResult = ctx.read(path);
            
            if (rawResult == null) {
                return new ArrayList<>();
            }
            
            if (!(rawResult instanceof List)) {
                log.warn("路径 {} 返回的不是 List，而是 {}", path, rawResult.getClass());
                return new ArrayList<>();
            }
            
            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) rawResult;
            
            if (rawList.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 用 Jackson 批量转换
            ObjectMapper mapper = JacksonUtils.objectMapper();
            List<T> result = new ArrayList<>(rawList.size());
            
            for (Object item : rawList) {
                if (item == null) {
                    result.add(null);
                    continue;
                }
                // 如果已经是 clazz 实例，直接加（极少见）
                if (clazz.isInstance(item)) {
                    result.add(clazz.cast(item));
                    continue;
                }
                // 否则转 JSON 节点再反序列化
                JsonNode node = mapper.valueToTree(item);
                T converted = mapper.treeToValue(node, clazz);
                result.add(converted);
            }
            
            return result;
        } catch (Exception e) {
            log.error("读取并转换 List 节点失败: path={}, class={}, json={}", path, clazz.getName(), json, e);
            return new ArrayList<>();
        }
    }
    
    @SuppressWarnings({"unchecked"})
    public static List<String> readListNode(String json, String path) {
        com.jayway.jsonpath.DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return new ArrayList<>();
        }
        
        try {
            List<Object> results = ctx.read(path);
            if (results == null) {
                return new ArrayList<>();
            }
            return results.stream().map(result -> JacksonUtils.toJson(result)).collect(toList());
        } catch (Exception e) {
            log.error("path为: {}, json为:{}", path, json, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 通过Jsonpath读取某个节点，返回单个对象，如果是集合则取第一个
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T readNodeSingleValue(String json, String path) {
        com.jayway.jsonpath.DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }
        
        Object result;
        try {
            result = ctx.read(path);
        } catch (Exception e) {
            log.error("path为: {}, json为:{}", path, json, e);
            return null;
        }
        
        if (result == null) {
            return null;
        }
        
        if (result instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) result;
            if (jsonArray.length() == 0) {
                return null;
            }
            return (T) jsonArray.get(0);
        }
        
        if (result instanceof List) {
            List list = (List) result;
            if (list.size() == 0) {
                return null;
            }
            return (T) list.get(0);
        }
        
        return (T) result;
    }
}