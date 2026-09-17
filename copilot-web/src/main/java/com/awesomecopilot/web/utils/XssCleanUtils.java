package com.awesomecopilot.web.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * XSS 清洗工具类：移除请求内容里的 script 标签、javascript: 伪协议、onXXX 事件属性等
 * HTML 注入点。注意清洗范围只限标签与协议层——纯文本里的函数名不删除（正常业务文案会
 * 被误伤，见 clean() 内注释），文本渲染成 HTML 时应在输出侧做转义。
 */
public class XssCleanUtils {

    // 预编译正则表达式
    // 1. 匹配完整 script 标签及内容
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<\\s*script[^>]*>.*?<\\s*/\\s*script\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // 2. 匹配单独的 script 标签片段
    private static final Pattern SCRIPT_TAG_FRAGMENT_PATTERN = Pattern.compile("<\\s*/?\\s*script\\s*>", Pattern.CASE_INSENSITIVE);
    // 3. 匹配 javascript: 伪协议（包括大小写、空格，仅移除前缀）
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile("java\\s*script\\s*:\\s*", Pattern.CASE_INSENSITIVE);
    // 4. 匹配带引号的 onXXX 事件属性
    private static final Pattern EVENT_ATTR_PATTERN = Pattern.compile("on\\w+\\s*=\\s*([\"']?).*?\\1(?=\\s|>|/)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // 5. 匹配无引号的 onXXX 事件属性
    private static final Pattern EVENT_ATTR_NO_QUOTE_PATTERN = Pattern.compile("on\\w+\\s*=\\s*[^\\s>]+", Pattern.CASE_INSENSITIVE);
    /*
     * 曾经这里有第6条规则: 删除"纯文本中的恶意函数调用" eval(...)/alert(...)/confirm(...)。
     * 已移除(评审报告 P0-2): eval/alert/confirm 也是普通英文词, 出现在业务文案、字段说明、
     * 消息模板里会被连带删除, 属于篡改用户数据; 而真正的 XSS 拦截靠上面几条标签/协议规则,
     * 纯文本不渲染成 HTML 时这些函数名本就无害, 要渲染时应做输出转义(如 StringUtils.escapeHtml4),
     * 不是输入删除。
     */

    /**
     * 兼顾所有测试场景的清洗逻辑
     */
    public static String clean(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String result = value;

        // 1. 移除完整的 script 标签及内容
        result = SCRIPT_TAG_PATTERN.matcher(result).replaceAll("");
        // 2. 移除残留的 script 标签片段
        result = SCRIPT_TAG_FRAGMENT_PATTERN.matcher(result).replaceAll("");
        // 3. 仅移除 javascript: 前缀（保留后续内容）
        result = JAVASCRIPT_PROTOCOL_PATTERN.matcher(result).replaceAll("");
        // 4. 移除带引号的 onXXX 事件属性
        result = EVENT_ATTR_PATTERN.matcher(result).replaceAll("");
        // 5. 移除无引号的 onXXX 事件属性
        result = EVENT_ATTR_NO_QUOTE_PATTERN.matcher(result).replaceAll("");

        return result;
    }

    /**
     * 递归清洗集合/数组/Map 类型的值。
     * 返回清洗后的结果: List 会重建(不可变 List 无法就地 set), Set/Map 同理;
     * String[] 与其他数组就地或重建清洗。调用方必须使用返回值, 不要继续引用原对象。
     */
    public static Object cleanObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return clean((String) value);
        }
        if (value instanceof String[]) {
            String[] arr = (String[]) value;
            for (int i = 0; i < arr.length; i++) {
                arr[i] = clean(arr[i]);
            }
            return arr;
        }
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            for (int i = 0; i < arr.length; i++) {
                arr[i] = cleanObject(arr[i]);
            }
            return arr;
        }
        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<Object, Object> cleaned = new LinkedHashMap<>(Math.max(16, map.size() * 2));
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                cleaned.put(cleanObject(entry.getKey()), cleanObject(entry.getValue()));
            }
            return cleaned;
        }
        if (value instanceof Iterable<?>) {
            List<Object> cleaned = new ArrayList<>();
            for (Object obj : (Iterable<?>) value) {
                cleaned.add(cleanObject(obj));
            }
            return cleaned;
        }
        return value;
    }
}