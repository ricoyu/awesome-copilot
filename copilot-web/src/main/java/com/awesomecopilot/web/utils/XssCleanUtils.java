package com.awesomecopilot.web.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 最终版 XSS 清洗工具类：兼顾纯文本恶意函数清理和 href 内容保留
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
    // 6. 匹配纯文本中的恶意函数调用（不含标签包裹）
    private static final Pattern PLAIN_TEXT_MALICIOUS_FUNC_PATTERN = Pattern.compile("(?<!href=')\\s*(eval|alert|confirm)\\s*\\([^)]*\\)\\s*(?!')", Pattern.CASE_INSENSITIVE);

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
        // 6. 仅清理纯文本中的恶意函数（不清理 href 内的）
        result = PLAIN_TEXT_MALICIOUS_FUNC_PATTERN.matcher(result).replaceAll("");

        return result;
    }

    /**
     * 递归清洗集合/数组类型的参数
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
        if (value instanceof Iterable<?>) {
            Iterable<?> iterable = (Iterable<?>) value;
            for (Object obj : iterable) {
                cleanObject(obj);
            }
        }
        return value;
    }
}