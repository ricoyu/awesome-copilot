package com.awesomecopilot.json.jsonpath.context;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jayway.jsonpath.JsonPath;

/**
 * JsonPath 编译结果的全局无锁热缓存。
 * <p>
 * JsonPath 编译产物不可变且线程安全, 可安全跨线程复用。带容量上限(20000条),
 * 防止业务把变量拼进 path(如 {@code $.x[?(@.id==' + id + ')])} 时无界堆积造成内存泄漏;
 * 超出后按 W-TinyLFU 驱逐, 代价只是重新 compile 一次(实测动态 path compile 约 6.5µs/个)。
 * <p>
 * 之所以放在 context 包而不是 JsonPathUtils 私有字段: {@link JsonContext#read(String, com.jayway.jsonpath.Predicate...)}
 * 也要走这份缓存。它原来走 json-path 库自带的全局 LRUCache(内部 ReentrantLock),
 * 高并发下多个线程在同一把锁上排队; 换到这里后与 JsonPathUtils 主入口共用同一份无锁缓存。
 * <p>
 * Copyright: Copyright (c) 2026-09-26
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public final class JsonPathCache {

    private static final Cache<String, JsonPath> PATH_CACHE = Caffeine.newBuilder()
            .maximumSize(20_000)
            .build();

    /**
     * 取(必要时编译)路径表达式的编译产物。path 先 trim, 与 JsonContext.read 的键统一,
     * 避免同一路径因首尾空白产生两个缓存条目、两个入口行为不一致
     * (2026-09-26 评审实测: 不 trim 时 readNode(ctx,"  $.a  ") 返回 null 而 ctx.read 同串能读到)。
     * <p>
     * 含 '(' 的函数表达式(如 $.nums.sum($.nums)、$.x.max()、append)不进缓存:
     * json-path 3.0.0 的 FunctionPathToken 在求值期会把函数参数写进共享编译产物的
     * Parameter 字段(lateBinding/evaluated 非 volatile), 同一条缓存实例被多线程跨报文复用时
     * 参数互相覆盖——实测 8 线程读两份报文 48 万次串值 10.4 万次且无任何异常。
     * 函数路径每次重新 compile, 单条短路径 compile 实测约 2.6-6.5µs, 远比数据串台错误的代价低。
     */
    public static JsonPath compiled(String path) {
        String key = path.trim();
        if (key.indexOf('(') >= 0) {
            return JsonPath.compile(key);
        }
        return PATH_CACHE.get(key, JsonPath::compile);
    }

    /**
     * 当前缓存条数, 仅用于测试与排查。
     */
    public static long size() {
        return PATH_CACHE.estimatedSize();
    }

    /**
     * 清空缓存。这是进程级共享状态, 仅供测试隔离与线上排查使用。
     */
    public static void clear() {
        PATH_CACHE.invalidateAll();
    }

    private JsonPathCache() {
    }
}
