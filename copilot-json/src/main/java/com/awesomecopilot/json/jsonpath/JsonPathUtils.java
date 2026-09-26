package com.awesomecopilot.json.jsonpath;

import com.awesomecopilot.common.lang.transformer.ValueHandlerFactory;
import com.awesomecopilot.json.JSON;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.json.jsonpath.context.DocumentContext;
import com.awesomecopilot.json.jsonpath.context.JsonContext;
import com.awesomecopilot.json.jsonpath.context.JsonPathCache;
import com.awesomecopilot.json.jsonpath.mapper.JacksonMappingProvider;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 高并发/大报文注意: 所有收 String json 的入口每次调用都会把整篇文档从头解析一遍
 * (实测 40KB 文档单次约 466µs、2MB 文档约 41ms; 路径求值本身只要 0.2-0.5µs)。
 * 同一份报文要读多个字段时, 先 {@link #parse(String)} 拿到 {@link DocumentContext},
 * 再反复用收 ctx 的重载读取, 解析只做一次 (实测 5 字段一组从 2.36ms 降到 0.48ms)。
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
        // provider 必须复用全局装饰过的 mapper: 无参构造会自建一个未带装饰配置的 ObjectMapper,
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
     * 解析 JSON 字符串为 DocumentContext（含内嵌JSON树级展开）, 供"解析一次、读多次"场景复用。
     * <p>
     * 典型用法: 从消息队列/接口收到一份报文要取十几个字段时, 先 parse 再逐个
     * {@code JsonPathUtils.readNode(ctx, path)}, 避免每个字段都把整篇文档重解析一遍
     * (收 String 的旧入口没有文档级缓存, 每次调用都从头解析)。
     * <p>
     * 注意: ctx 内部是已解析的 Map/List 树, 属性/通配/过滤器路径并发读取安全(函数类路径
     * 如 sum()/append() 的编译产物存在 json-path 库级并发缺陷, JsonPathCache 已改为
     * 每次重新编译规避), 但读出的 Map/List 就是树本身
     * (不是副本), 调用方不要修改读出来的容器; 也别跨请求长期持有大文档的 ctx(等于扣住整棵树)。
     *
     * @param json 待解析的JSON串; 空白返回 null; 非法JSON先尝试 cleanup 修复, 仍失败则返回 null
     * @return 可复用的文档上下文
     */
    public static DocumentContext parse(String json) {
        return parse(json, true);
    }

    /**
     * 解析 JSON 字符串为 DocumentContext, 可控制是否做内嵌JSON树级展开。
     * <p>
     * unwrapEmbedded=false 时跳过整树遍历(实测 2MB 文档这步占解析耗时的四成以上),
     * 适合报文确定不含"被序列化成字符串的内嵌JSON"的高频读取场景;
     * 此时 {@code $.billJson.FBillNo} 这类穿透写法读不到, billJson 值保持原始字符串。
     *
     * @param json           待解析的JSON串; 空白返回 null
     * @param unwrapEmbedded 是否把"本身就是合法JSON文档的字符串值"提升为子对象
     * @return 可复用的文档上下文
     */
    public static DocumentContext parse(String json, boolean unwrapEmbedded) {
        if (isBlank(json)) {
            return null;
        }

        try {
            /*
             * 先按原样解析. 以前每次解析前都无条件跑 JSON.cleanup(三次全串replace),
             * 它会把值里合法的转义引号 \" 也解开, 导致含转义双引号的合法JSON被改成
             * 非法串, 所有读取返回 null 且无报错提示(实测复现). 现在改成两步:
             * 1) 原样解析成功 -> 只做"树级展开": 把本身就是合法JSON文档的字符串值提升为子对象,
             *    保住 $.billJson.FBillNo 这类穿透内嵌JSON的老用法, 且不碰其它值;
             * 2) 原样解析失败 -> 才用 cleanup 抢救一把(老代码对截断/双重转义数据的兼容路径)。
             */
            Object obj;
            try {
                obj = CONFIG.jsonProvider().parse(json);
            } catch (Exception directFailed) {
                obj = CONFIG.jsonProvider().parse(JSON.cleanup(json));
                log.warn("JSON直接解析失败, cleanup修复后解析成功, jsonSummary={}", jsonSummary(json));
            }
            if (unwrapEmbedded) {
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
            }
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
     * Bypass JsonContext.read(String), which uses JsonPath's global LRU cache.
     * The default cache can be a shared lock hot spot under high concurrency.
     * 这里改用无锁缓存(JsonPathCache), 避免每次读取都重新 compile 路径表达式。
     */
    private static <T> T readPath(com.jayway.jsonpath.DocumentContext ctx, String path) {
        Object json = ctx.json();
        JsonPath compiledPath = JsonPathCache.compiled(path);
        return compiledPath.read(json, ctx.configuration());
    }

    private static <T> T convertValue(Object value, Class<T> clazz) {
        // 快速路径: 值本来就是目标类型的不可变标量时直接返回, 省掉一次 objectMapper.convertValue
        // 的树往返(实测约 0.9µs/次, readNode(json, path, String.class) 读字符串字段的常见场景里
        // 这步全是白做的拷贝)。
        if (value != null && clazz.isInstance(value) && isImmutableScalar(value)) {
            return clazz.cast(value);
        }
        // mappingProvider 走 objectMapper.convertValue, 产物已是目标类型的新实例
        // (Map/List 是深拷贝副本, 不暴露 ctx 共享树)。标量与容器直接返回:
        // 只有"mapped 还不是目标类型的标量"(如需要字符串二次解析成数字)才交给 ValueHandler。
        // 旧代码无条件 handler.convert(mapped) 有两处既有缺陷(2026-09-26 评审实测):
        // 1) Map/List/POJO 目标无 handler, 直接 NPE, 被上层 catch 后返回 null 且日志无提示,
        //    即 readNode(json, path, Map.class) 永远拿到 null;
        // 2) DoubleValueHandler 不认 Double 入参、StringListValueHandler 不认 List 入参,
        //    JSON 里 5.5 读成 Double.class 也走这条路, 抛异常后被上层捕获返回 null。
        Object mapped = CONFIG.mappingProvider().map(value, clazz, CONFIG);
        if (mapped != null) {
            Class<?> target = wrapPrimitive(clazz);
            if (target.isInstance(mapped) && (isImmutableScalar(mapped) || mapped instanceof Map || mapped instanceof List)) {
                return (T) mapped;
            }
        }
        ValueHandlerFactory.ValueHandler<T> valueHandler = ValueHandlerFactory.determineAppropriateHandler(clazz);
        if (valueHandler == null) {
            return (T) mapped;
        }
        T converted = valueHandler.convert(mapped);
        return converted != null ? converted : (T) mapped;
    }

    /**
     * 基本类型转包装类(double.class -> Double.class 等), 非基本类型原样返回。
     */
    private static Class<?> wrapPrimitive(Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        }
        if (clazz == double.class) return Double.class;
        if (clazz == int.class) return Integer.class;
        if (clazz == long.class) return Long.class;
        if (clazz == float.class) return Float.class;
        if (clazz == short.class) return Short.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == char.class) return Character.class;
        return clazz;
    }

    // 不可变标量白名单按具体类列举(评审E4: AtomicInteger/LongAdder 也 extends Number 但可变,
    // instanceof Number 会把它们放进快速路径)
    private static boolean isImmutableScalar(Object value) {
        return value instanceof String || value instanceof Boolean || value instanceof Character
                || value instanceof Integer || value instanceof Long || value instanceof Double
                || value instanceof Float || value instanceof Short || value instanceof Byte
                || value instanceof java.math.BigDecimal || value instanceof java.math.BigInteger;
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

    // ---------------------------------------------------------------------------------------
    // 收 DocumentContext 的重载: 解析已在上游完成, 这里只做路径求值/类型转换,
    // 标量路径单次成本约 0.2-1µs, 与报文大小无关。高频多字段读取请走这一组。
    // ---------------------------------------------------------------------------------------

    /**
     * 在已解析的文档上判断路径是否存在且有数据。
     */
    public static boolean ifExists(com.jayway.jsonpath.DocumentContext ctx, String path) {
        if (ctx == null) {
            return false;
        }

        try {
            Object result = readPath(ctx, path);
            return !isEmptyResult(result);
        } catch (Exception e) {
            log.error("Read JSON path failed, path={}", path, e);
            return false;
        }
    }

    /**
     * 在已解析的文档上按 JsonPath 读取节点。结果可能是单值或集合。
     */
    public static <T> T readNode(com.jayway.jsonpath.DocumentContext ctx, String path) {
        if (ctx == null) {
            return null;
        }

        try {
            return readPath(ctx, path);
        } catch (Exception e) {
            log.error("读取JSON节点: {} 报错", path, e);
            return null;
        }
    }

    /**
     * 在已解析的文档上读取, 路径存在且有数据才返回, 否则 null。
     */
    @SuppressWarnings("unchecked")
    public static <T> T readNodeIfExists(com.jayway.jsonpath.DocumentContext ctx, String path) {
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
            log.error("Read JSON path failed, path={}", path, e);
            return null;
        }
    }

    /**
     * 在已解析的文档上读取并转换成目标类型。
     */
    public static <T> T readNode(com.jayway.jsonpath.DocumentContext ctx, String path, Class<T> clazz) {
        if (ctx == null) {
            return null;
        }

        try {
            return convertValue(readPath(ctx, path), clazz);
        } catch (Exception e) {
            log.error("Read JSON path failed, path={}, class={}", path, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 在已解析的文档上读取(有数据才返回)并转换成目标类型。
     */
    public static <T> T readNodeIfExists(com.jayway.jsonpath.DocumentContext ctx, String path, Class<T> clazz) {
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
            log.error("Read JSON path failed, path={}, class={}", path, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 在已解析的文档上读取列表并逐项转换成目标类型。
     */
    public static <T> List<T> readListNode(com.jayway.jsonpath.DocumentContext ctx, String path, Class<T> clazz) {
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
            log.error("Read and convert JSON list failed, path={}, class={}", path, clazz.getName(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 在已解析的文档上读取列表, 每个元素序列化成JSON字符串返回。
     */
    public static List<String> readListNode(com.jayway.jsonpath.DocumentContext ctx, String path) {
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
            log.error("Read JSON list failed, path={}", path, e);
            return new ArrayList<>();
        }
    }

    /**
     * 在已解析的文档上按 JsonPath 读取单值; 结果是集合时返回第一个元素。
     */
    @SuppressWarnings({"rawtypes"})
    public static <T> T readNodeSingleValue(com.jayway.jsonpath.DocumentContext ctx, String path) {
        if (ctx == null) {
            return null;
        }

        Object result;
        try {
            result = readPath(ctx, path);
        } catch (Exception e) {
            log.error("Read single JSON value failed, path={}", path, e);
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

    // ---------------------------------------------------------------------------------------
    // 收 String 的旧入口: 语义不变(每次调用完整解析一遍), 内部委托 parse + ctx 重载。
    // 同一份报文读多个字段请改用上面的 ctx 重载, 见类注释。
    // ---------------------------------------------------------------------------------------

    /**
     * @param json
     * @param path
     * @return
     */
    public static boolean ifExists(String json, String path) {
        return ifExists(parse(json), path);
    }

    /**
     * Read a node by JsonPath. The result can be a single value or a collection.
     * 注意: 每次调用都会完整解析一遍 json, 多字段读取请改用 {@link #parse(String)} + ctx 重载。
     *
     * @param json
     * @param path
     * @return
     */
    public static <T> T readNode(String json, String path) {
        return readNode(parse(json), path);
    }

    /**
     * Read the value when the path exists and has data, otherwise return null.
     * This parses the JSON only once.
     */
    public static <T> T readNodeIfExists(String json, String path) {
        return readNodeIfExists(parse(json), path);
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
        return readNode(parse(json), path, clazz);
    }

    public static <T> T readNodeIfExists(String json, String path, Class<T> clazz) {
        return readNodeIfExists(parse(json), path, clazz);
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
        return readListNode(parse(json), path, clazz);
    }

    public static List<String> readListNode(String json, String path) {
        return readListNode(parse(json), path);
    }

    /**
     * Read a value by JsonPath. If the result is a collection, return the first element.
     */
    public static <T> T readNodeSingleValue(String json, String path) {
        return readNodeSingleValue(parse(json), path);
    }
}
