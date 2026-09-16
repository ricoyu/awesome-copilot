package com.awesomecopilot.networking.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 域名处理
 * <p>
 * 2026-09-16 按《copilot-networking/评审报告.md》P1-9 重写：
 * ① 旧实现用嵌套量词正则 (\w*\.?){N}——匹配失败时正则引擎要尝试所有切分组合,
 *   耗时随输入长度指数增长(实测 401 字符 6.3 秒), 接收用户可控输入就是一个请求
 *   拖死一个线程(ReDoS)。现在改为纯字符串切分, 处理时间与输入长度成线性。
 * ② \w 不包含连字符, "my-site.com" 的一级域名旧实现返回 "site.com"。
 *   现在按主机名合法字符集(字母数字、连字符、点)处理, 返回 "my-site.com"。
 * <p>
 * Copyright: (C), 2021-09-13 14:22
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class DomainUtils {

	/**
	 * 常见的"公共后缀"组合。这些后缀算一级域名时要多吃一段:
	 * www.a.com.cn 的一级域名是 a.com.cn 而不是 com.cn。
	 */
	private static final Set<String> TWO_LEVEL_SUFFIXES = new HashSet<>(Arrays.asList(
			"com.cn", "net.cn", "gov.cn", "org.cn", "edu.cn", "org.nz", "com.au", "co.uk", "org.uk"));

	/**
	 * 获取一级、二级、三级域名(从右往左数 N+后缀段 拼回)。
	 * <p>
	 * 纯字符串处理, 无正则回溯; 输入多长都线性完成。
	 *
	 * @param url   主机名或含主机的URL(如 http://www.my-site.com:8080/x)
	 * @param level 1=一级, 2=二级, 3=三级
	 * @return String 命中的域名部分; 无法识别返回空串
	 */
	public static String getDomain(String url, int level) {
		if (isBlank(url) || level < 1 || level > 3) {
			return level < 1 || level > 3 ? "" : null;
		}
		String host = extractHost(url.trim());
		// 纯 IPv4 不是域名
		if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
			return "";
		}
		String[] parts = host.split("\\.");
		if (parts.length < 2) {
			return "";
		}

		/*
		 * 先定后缀占几段: 末尾两段若恰是已知组合后缀(com.cn 等)占2段, 否则顶级域占1段。
		 * 再从后缀左侧取 level 段。旧正则把后缀写成固定枚举列表, 这里保持一致的优先识别。
		 */
		int suffixLen = 1;
		if (parts.length >= 3) {
			String lastTwo = parts[parts.length - 2] + "." + parts[parts.length - 1];
			if (TWO_LEVEL_SUFFIXES.contains(lastTwo.toLowerCase())) {
				suffixLen = 2;
			}
		}

		int take = level + suffixLen;
		if (parts.length <= take) {
			return host; // 段数不够, 整个主机就是答案(如 baidu.com 取一级/二级都得 baidu.com)
		}
		return String.join(".", Arrays.copyOfRange(parts, parts.length - take, parts.length));
	}

	/**
	 * 从"可能是完整URL"的串里取主机名: 去掉 scheme、路径、query、端口, 保留连字符。
	 */
	private static String extractHost(String url) {
		int i = url.indexOf("://");
		String rest = i >= 0 ? url.substring(i + 3) : url;
		// 砍掉 path / query / fragment
		for (char c : new char[]{'/', '?', '#'}) {
			int idx = rest.indexOf(c);
			if (idx >= 0) {
				rest = rest.substring(0, idx);
			}
		}
		// 砍掉端口(处理 IPv6 的 [::1]:8080 形态: 先找 ']', 冒号只在其后才算端口分隔)
		if (rest.startsWith("[")) {
			int close = rest.indexOf(']');
			return close > 0 ? rest.substring(0, close + 1) : rest;
		}
		int colon = rest.indexOf(':');
		if (colon >= 0) {
			rest = rest.substring(0, colon);
		}
		return rest;
	}
}
