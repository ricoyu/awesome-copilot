package com.awesomecopilot.common.lang.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class UrlUtils {

    private static final String ENCODING = "UTF-8";

    private static final Logger log = LoggerFactory.getLogger(UrlUtils.class);

    /**
     * 对完整URL中的URI部分进行编码，并返回包含编码后URI部分的完整URL。
     * <p>
     * P2-32(CODE_REVIEW_REPORT): 修复前重建 URL 只拼 协议+主机+端口+路径+查询——
     * 实测 "https://ex.com/a?q=hello world#frag" 编码后 "#frag" 整段消失,
     * "https://u:p@ex.com/p" 的用户名密码被丢弃(后续请求以未认证身份发出);
     * 且查询串整体 encode 后把 %3D/%26 无条件换回 =/&——原值里本来就编码好的 %20
     * 被二次编码成 %2520(实测 "q=a%20b" 变 "q=a%2520b")。
     * <p>
     * 现按组件重建(协议://用户信息@主机:端口/路径?查询#片段, 各段独立编码后拼回),
     * 合法的 %XX 序列原样透传不再二次编码。组件拆分不借助 java.net.URL 的 getter,
     * 直接在入参字符串上用分隔符(最后一个 '@'、首个 '/'、'?'、'#')手工切分——
     * 本批修复的中间版本曾用 URL.getUserInfo(), surefire 报告实录其在部分调用路径
     * 返回 "***"(JDK 对 URL 凭证的打码行为, JDK-8248422), 输出被改坏且随 JDK 构建漂移;
     * 手工切分对原文完全忠实, 不受此影响。
     * <p>
     * 行为变化说明:
     * <ul>
     * <li>查询值里的字面 '=' 现在编码为 %3D(修复前 %3D 被无条件换回 '=', 值与
     * 分隔符混在一起, 服务端解码结果不再唯一);</li>
     * <li>path 里的空格等不安全字符现在也会被编码(修复前原样输出, 拼出的仍是坏 URL);</li>
     * <li>分隔结构保留不编码: path 的 '/'、query 的 '=' 与 '&'、userInfo 的 ':'。</li>
     * </ul>
     *
     * @param urlOrUri 需要编码的完整URL, 或单独的查询串
     * @return 包含编码后URI部分的完整URL
     */
    public static String encodeUrl(String urlOrUri) {
        if (!isValidUrl(urlOrUri)) {
            //非完整 URL: 按"单独的查询串"处理(键值对编码, 与完整 URL 的 query 部分同一套规则)
            try {
                return encodeQuery(urlOrUri);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            //① #fragment: 修复前整段被丢弃, 现最后拼回
            int hashPos = urlOrUri.indexOf('#');
            String head = hashPos >= 0 ? urlOrUri.substring(0, hashPos) : urlOrUri;
            String fragment = hashPos >= 0 ? urlOrUri.substring(hashPos + 1) : null;

            //② ?query
            int qPos = head.indexOf('?');
            String query = qPos >= 0 ? head.substring(qPos + 1) : null;
            String loc = qPos >= 0 ? head.substring(0, qPos) : head;

            //③ scheme:// 之后先分 authority 与 path
            int schemeEnd = loc.indexOf("://");
            String scheme = loc.substring(0, schemeEnd + 3); //含 "://"
            String rest = loc.substring(schemeEnd + 3);
            int pathStart = rest.indexOf('/');
            String authority = pathStart >= 0 ? rest.substring(0, pathStart) : rest;
            String path = pathStart >= 0 ? rest.substring(pathStart) : "";

            //④ authority 里用最后一个 @ 分 userinfo 与 host:port(host 不允许 '@', 取最后即可)
            int atPos = authority.lastIndexOf('@');
            String userInfo = atPos >= 0 ? authority.substring(0, atPos) : null;
            String hostPort = atPos >= 0 ? authority.substring(atPos + 1) : authority;

            StringBuilder sb = new StringBuilder();
            sb.append(scheme);
            if (userInfo != null) {
                //':' 是 userinfo 的合法分隔符, 保留(修复前该组件根本不存在于输出里)
                sb.append(encodePreservingPct(userInfo, ":")).append('@');
            }
            sb.append(hostPort); //host:port 字面量按 URL 规则本就合法, 原样保留
            if (!path.isEmpty()) {
                sb.append(encodePath(path));
            }
            if (query != null) {
                sb.append('?').append(encodeQuery(query));
            }
            if (fragment != null) {
                sb.append('#').append(encodePreservingPct(fragment, ""));
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询串编码: 以 & 拆对、每对以第一个 = 拆键值, 分别编码后重新拼接——
     * = 与 & 的分隔结构得以保留, 而值内部额外的字面 = 会被编码成 %3D, 不再和分隔符混淆。
     */
    private static String encodeQuery(String rawQuery) throws UnsupportedEncodingException {
        StringBuilder out = new StringBuilder();
        String[] pairs = rawQuery.split("&");
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                out.append('&');
            }
            String pair = pairs[i];
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                out.append(encodePreservingPct(pair.substring(0, eq), ""))
                        .append('=')
                        .append(encodePreservingPct(pair.substring(eq + 1), ""));
            } else {
                out.append(encodePreservingPct(pair, ""));
            }
        }
        return out.toString();
    }

    /**
     * 路径编码: 按 '/' 分段分别编码再拼回, 保留 '/' 的分隔结构。
     */
    private static String encodePath(String path) throws UnsupportedEncodingException {
        String[] segments = path.split("/", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                out.append('/');
            }
            out.append(encodePreservingPct(segments[i], ""));
        }
        return out.toString();
    }

    /**
     * 组件值编码: 合法的 %XX 转义序列三个字符原样透传(修复前会被二次编码成 %25XX);
     * keep 列出的字符原样保留(如 userInfo 的 ':'); 其余普通字符交给 URLEncoder
     * (输出 %XX 大写十六进制, 空格输出 %20, 与修复前把 + 换回 %20 的结果一致)。
     */
    private static String encodePreservingPct(String s, String keep) throws UnsupportedEncodingException {
        StringBuilder out = new StringBuilder();
        StringBuilder run = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (isPctEscape(s, i)) {
                flushRun(out, run);
                out.append(s, i, i + 3); //合法 %XX 透传
                i += 3;
            } else if (keep.indexOf(c) >= 0) {
                flushRun(out, run);
                out.append(c);
                i++;
            } else {
                run.append(c);
                i++;
            }
        }
        flushRun(out, run);
        return out.toString();
    }

    private static void flushRun(StringBuilder out, StringBuilder run) throws UnsupportedEncodingException {
        if (run.length() > 0) {
            out.append(URLEncoder.encode(run.toString(), ENCODING).replace("+", "%20"));
            run.setLength(0);
        }
    }

    private static boolean isPctEscape(String s, int i) {
        return s.charAt(i) == '%' && i + 2 < s.length()
                && isHexDigit(s.charAt(i + 1)) && isHexDigit(s.charAt(i + 2));
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * 检查字符串是否是有效的URL
     *
     * @param url 要检查的字符串
     * @return 如果是有效的URL则返回true，否则返回false
     */
    public static boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
