package com.awesomecopilot.json.jsonpath;

import com.awesomecopilot.common.lang.transformer.ValueHandlerFactory;
import com.awesomecopilot.json.JSON;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.json.jsonpath.context.DocumentContext;
import com.awesomecopilot.json.jsonpath.context.JsonContext;
import com.awesomecopilot.json.jsonpath.mapper.JacksonMappingProvider;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

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
        // provider 必须复用全局装饰过的 mapper: 无参构造会自建裸 ObjectMapper,
        // 导致 ALLOW_SINGLE_QUOTES 等解析端配置只在一半场景生效(与 JacksonUtils.toObject 行为不一致)
        JsonProvider jsonProvider = new JacksonJsonProvider(JacksonUtils.objectMapper());
        MappingProvider mappingProvider = new JacksonMappingProvider(JacksonUtils.objectMapper());

        CONFIG = Configuration.builder()
                .jsonProvider(jsonProvider)
                .mappingProvider(mappingProvider)
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
    }

    /**
     * 树级展开内嵌JSON时用的严格解析器: 不忽略未知字段配置无所谓, 关键是整串必须是
     * 一个完整JSON文档(readTree会校验尾部多余内容). 独立实例, 不共享全局装饰mapper,
     * 避免把 Date 装饰等无关配置带进来.
     */
    private static final ObjectMapper EMBEDDED_JSON_READER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    /**
     * 内嵌JSON字符串最大展开层数, 防御异常深嵌套
     */
    private static final int MAX_UNWRAP_DEPTH = 5;

    /**
     * 解析 JSON 字符串为 DocumentContext（无缓存）
     */
    private static DocumentContext parseJson(String json) {
        if (isBlank(json)) {
            return null;
        }

        try {
            /*
             * 先按原样解析. 以前每次解析前都无条件跑 JSON.cleanup(三次全串replace),
             * 它会把值里合法的转义引号 \" 也解开, 导致含转义双引号的合法JSON被改成
             * 非法串, 所有读取静默返回null(实测复现). 现在改成两步:
             * 1) 原样解析成功 -> 只做"树级展开": 把本身就是合法JSON文档的字符串值提升为子对象,
             *    保住 $.billJson.FBillNo 这类穿透内嵌JSON的老用法, 且不碰其它值;
             * 2) 原样解析失败 -> 才用 cleanup 抢救一把(老代码对截断/双重转义数据的兼容路径).
             */
            Object obj;
            try {
                obj = CONFIG.jsonProvider().parse(json);
            } catch (Exception directFailed) {
                obj = CONFIG.jsonProvider().parse(JSON.cleanup(json));
                log.warn("JSON直接解析失败, cleanup修复后解析成功, jsonSummary={}", jsonSummary(json));
            }
            // 整篇文档被字符串化(根节点本身就是个字符串 "{...}")的脏数据: 先对根尝试解一层,
            // 否则下游只遍历 Map/List 成员, 根级字符串永远不会被展开.
            if (obj instanceof String) {
                Object unwrappedRoot = tryUnwrapText(obj);
                if (unwrappedRoot != null) {
                    obj = unwrappedRoot;
                }
            }
            // JacksonJsonProvider.parse 产出的是 Map/List 树(非JsonNode), 在这棵树上展开内嵌JSON
            unwrapEmbeddedJson(obj, 0);
            return new JsonContext(obj, CONFIG);
        } catch (Exception e) {
            log.error("JSON格式有问题, 输入的JSON串为: {}", jsonSummary(json), e);
            return null;
        }
    }

    /**
     * 树级展开内嵌JSON: 递归遍历 Map/List(provider解析产物是纯Map/List树),
     * 字符串值若能整体严格解析为JSON文档, 就用解析出的 Map/List子树原地替换它,
     * 让 $.a.b 能直接穿透 a 原本是JSON字符串的场景.
     * <p>
     * 与旧的字符串级 cleanup 的区别: 只提升"严格合法的JSON文档", 值里出现的普通转义引号
     * (如 he said \"hi\")、非JSON模板串(如 {name})一律原样保留.
     * <p>
     * depth 计的是"已展开的字符串化链层数", 只有一次成功 unwrap 才 +1; 顺着原生 Map/List
     * 子节点下钻不加计数. 这样深业务嵌套(如 l1.l2.l3.l4.l5.l6.payload 各层都是原生对象)不会
     * 被误当成字符串化链超预算——预算只用于防"自我多层字符串化"的病态嵌套.
     *
     * @param node        当前节点(Map/List/String/Number等)
     * @param unwrapDepth 已展开的字符串化层数
     */
    @SuppressWarnings("unchecked")
    private static void unwrapEmbeddedJson(Object node, int unwrapDepth) {
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                entry.setValue(unwrapChild(entry.getValue(), unwrapDepth));
            }
        } else if (node instanceof List) {
            List<Object> list = (List<Object>) node;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, unwrapChild(list.get(i), unwrapDepth));
            }
        }
    }

    /**
     * 展开一个子节点并返回要写回的值: 能解成 JSON 文档就返回子树(并继续下钻, 计数 +1);
     * 否则返回原值(若是原生 Map/List 则继续下钻但不计数).
     */
    private static Object unwrapChild(Object value, int unwrapDepth) {
        Object unwrapped = tryUnwrapText(value);
        if (unwrapped != null) {
            // 字符串化链过深(自我多层嵌套)则停止, 保留原字符串; 原生树遍历不受此预算限制
            if (unwrapDepth + 1 >= MAX_UNWRAP_DEPTH) {
                return value;
            }
            unwrapEmbeddedJson(unwrapped, unwrapDepth + 1);
            return unwrapped;
        }
        // 原生子结构: 继续遍历其成员, 但不增加字符串化计数
        unwrapEmbeddedJson(value, unwrapDepth);
        return value;
    }

    /**
     * 值是字符串且(剥掉若干层引号字符串化后)以 '{' 或 '[' 开头、能整体严格解析成
     * Map/List 时返回解析结果; 其余一律返回 null(原样保留).
     * 引号层是循环剥的, 支持任意层数的"整段JSON被反复字符串化"存储, 层数受 MAX_UNWRAP_DEPTH 约束.
     */
    private static Object tryUnwrapText(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String text = (String) value;
        // 以引号开头说明整段又被当成字符串序列化过, 循环剥到首个非引号字符(受层数上限保护)
        for (int guard = 0; text.length() >= 1 && text.charAt(0) == '"' && guard < MAX_UNWRAP_DEPTH; guard++) {
            try {
                Object stripped = EMBEDDED_JSON_READER.readValue(text, Object.class);
                if (!(stripped instanceof String)) {
                    return null;
                }
                text = (String) stripped;
            } catch (Exception ignored) {
                return null;
            }
        }
        if (text.length() < 2) {
            return null;
        }
        char first = text.charAt(0);
        if (first != '{' && first != '[') {
            return null;
        }
        try {
            Object inner = EMBEDDED_JSON_READER.readValue(text, Object.class);
            if (inner instanceof Map || inner instanceof List) {
                return inner;
            }
        } catch (Exception notJson) {
            // 不是合法JSON文档, 原样保留
        }
        return null;
    }

    /**
     * 无锁路径表达式热缓存。JsonPath 编译结果不可变且线程安全，可安全复用。
     * 用 Caffeine 带上限(20000条), 防止业务把变量拼进 path(如 $.x[?(@.id==' + id + ')])
     * 时无界堆积造成内存泄漏; 超出后按 LRU 驱逐, 代价只是重新 compile 一次.
     */
    private static final Cache<String, JsonPath> PATH_CACHE = Caffeine.newBuilder()
            .maximumSize(20_000)
            .build();

    /**
     * Bypass JsonContext.read(String), which uses JsonPath's global LRU cache.
     * The default cache can be a shared lock hot spot under high concurrency.
     * 这里改用无锁缓存, 避免每次读取都重新 compile 路径表达式。
     */
    private static <T> T readPath(DocumentContext ctx, String path) {
        Object json = ctx.json();
        JsonPath compiledPath = PATH_CACHE.get(path, JsonPath::compile);
        return compiledPath.read(json, ctx.configuration());
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
        // 旧版这里还有一个 org.json.JSONArray 分支, 但 CONFIG 用的是 JacksonJsonProvider,
        // 路径结果只会是 Map/List/标量, JSONArray 分支永远走不到, 已删
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

        // JacksonJsonProvider 的路径结果只会是 List/Map/标量, 旧版的 org.json.JSONArray 分支永远走不到, 已删
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
