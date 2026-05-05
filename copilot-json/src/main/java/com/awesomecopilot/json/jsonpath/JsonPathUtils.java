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
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
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
public final class JsonPathUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonPathUtils.class);

    private static final int JSON_LOG_PREVIEW_LENGTH = 128;

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
                 * Normalize escaped embedded JSON objects.
                 */
                processedJson = JSON.cleanup(json);
                if (log.isDebugEnabled()) {
                    log.debug("处理后的JSON: {}", jsonSummary(processedJson));
                }
            }

            Object obj = CONFIG.jsonProvider().parse(processedJson);
            return new JsonContext(obj, CONFIG);
        } catch (Exception e) {
            log.error("JSON格式有问题, 输入的JSON串为: {}", jsonSummary(json), e);
            return null;
        }
    }

    /**
     * Bypass JsonContext.read(String), which uses JsonPath's global LRU cache.
     * The default cache can be a shared lock hot spot under high concurrency.
     */
    private static <T> T readPath(DocumentContext ctx, String path) {
        Object json = ctx.json();
        return JsonPath.compile(path).read(json, ctx.configuration());
    }

    private static <T> T convertValue(Object value, Class<T> clazz) {
        Object mapped = CONFIG.mappingProvider().map(value, clazz, CONFIG);
        ValueHandlerFactory.ValueHandler<T> valueHandler = ValueHandlerFactory.determineAppropriateHandler(clazz);
        return valueHandler.convert(mapped);
    }

    private static boolean isEmptyResult(Object result) {
        if (result == null) {
            return true;
        }
        if (result instanceof Collection) {
            return ((Collection<?>) result).isEmpty();
        }
        if (result instanceof JSONArray) {
            return ((JSONArray) result).length() == 0;
        }
        return false;
    }

    private static String jsonSummary(String json) {
        if (json == null) {
            return "null";
        }
        String preview = json.length() <= JSON_LOG_PREVIEW_LENGTH
                ? json
                : json.substring(0, JSON_LOG_PREVIEW_LENGTH);
        return "length=" + json.length()
                + ", hash=" + Integer.toHexString(json.hashCode())
                + ", preview=" + preview;
    }

    /**
     * @param json
     * @param path
     * @return
     */
    public static boolean ifExists(String json, String path) {
        DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return false;
        }

        try {
            Object result = readPath(ctx, path);
            return !isEmptyResult(result);
        } catch (Exception e) {
            log.error("Read JSON path failed, path={}, jsonSummary={}", path, jsonSummary(json), e);
            return false;
        }
    }

    /**
     * Read a node by JsonPath. The result can be a single value or a collection.
     *
     * @param json
     * @param path
     * @return
     */
    public static <T> T readNode(String json, String path) {
        DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }

        try {
            return readPath(ctx, path);
        } catch (Exception e) {
            log.error("读取JSON节点: {} 报错, jsonSummary={}", path, jsonSummary(json), e);
            return null;
        }
    }

    /**
     * Read the value when the path exists and has data, otherwise return null.
     * This parses the JSON only once.
     */
    public static <T> T readNodeIfExists(String json, String path) {
        DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }

        try {
            Object result = readPath(ctx, path);
            if (isEmptyResult(result)) {
                return null;
            }
            return (T) result;
        } catch (Exception e) {
            log.error("Read JSON path failed, path={}, jsonSummary={}", path, jsonSummary(json), e);
            return null;
        }
    }

    /**
     * Read a node by JsonPath and convert it to the target type.
     *
     * @param json
     * @param path
     * @param clazz
     * @return
     */
    public static <T> T readNode(String json, String path, Class<T> clazz) {
        DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }

        try {
            return convertValue(readPath(ctx, path), clazz);
        } catch (Exception e) {
            log.error("Read JSON path failed, path={}, class={}, jsonSummary={}", path, clazz.getName(), jsonSummary(json), e);
            return null;
        }
    }

    public static <T> T readNodeIfExists(String json, String path, Class<T> clazz) {
        DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }

        try {
            Object result = readPath(ctx, path);
            if (isEmptyResult(result)) {
                return null;
            }
            return convertValue(result, clazz);
        } catch (Exception e) {
            log.error("Read JSON path failed, path={}, class={}, jsonSummary={}", path, clazz.getName(), jsonSummary(json), e);
            return null;
        }
    }

    /**
     * Example 1: JSON array
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
     *     "nickname": "san-shao-ye",
     *     "birthday": "1982-11-09",
     *     "gender": 1,
     *     "avatar": "qiniu/image/j/8D57A18AD6926D6D879DFA36B4ED9CC4.jpg"
     *   }
     * ]
     * }</pre>
     * Read elements with or without error property.<p>
     * <pre>{@code
     * JsonPathUtils.readListNode(result, "[?(@.error)].username", String.class)
     * JsonPathUtils.readListNode(result, "[?(!@.error)].username", String.class)
     * }</pre>
     * <p>
     * Example 2: JSON object
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
     * Read all usernames: $.users[*].username
     *
     * Example 2: read all usernames: $.users[*].username
     */
    public static <T> List<T> readListNode(String json, String path, Class<T> clazz) {
        DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return new ArrayList<>();
        }

        try {
            Object rawResult = readPath(ctx, path);

            if (rawResult == null) {
                return new ArrayList<>();
            }

            if (!(rawResult instanceof List)) {
                log.warn("Path {} did not return List, actualType={}", path, rawResult.getClass());
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) rawResult;

            if (rawList.isEmpty()) {
                return new ArrayList<>();
            }

            ObjectMapper mapper = JacksonUtils.objectMapper();
            List<T> result = new ArrayList<>(rawList.size());

            for (Object item : rawList) {
                if (item == null) {
                    result.add(null);
                    continue;
                }
                if (clazz.isInstance(item)) {
                    result.add(clazz.cast(item));
                    continue;
                }
                JsonNode node = mapper.valueToTree(item);
                T converted = mapper.treeToValue(node, clazz);
                result.add(converted);
            }

            return result;
        } catch (Exception e) {
            log.error("Read and convert JSON list failed, path={}, class={}, jsonSummary={}", path, clazz.getName(), jsonSummary(json), e);
            return new ArrayList<>();
        }
    }

    @SuppressWarnings({"unchecked"})
    public static List<String> readListNode(String json, String path) {
        DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return new ArrayList<>();
        }

        try {
            List<Object> results = readPath(ctx, path);
            if (results == null) {
                return new ArrayList<>();
            }
            return results.stream().map(JacksonUtils::toJson).collect(toList());
        } catch (Exception e) {
            log.error("Read JSON list failed, path={}, jsonSummary={}", path, jsonSummary(json), e);
            return new ArrayList<>();
        }
    }

    /**
     * Read a value by JsonPath. If the result is a collection, return the first element.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T readNodeSingleValue(String json, String path) {
        DocumentContext ctx = parseJson(json);
        if (ctx == null) {
            return null;
        }

        Object result;
        try {
            result = readPath(ctx, path);
        } catch (Exception e) {
            log.error("Read single JSON value failed, path={}, jsonSummary={}", path, jsonSummary(json), e);
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
