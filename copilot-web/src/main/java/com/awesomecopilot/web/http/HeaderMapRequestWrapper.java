package com.awesomecopilot.web.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * 通过Filter修改(增加/覆盖)请求头
 * https://stackoverflow.com/questions/2811769/adding-an-http-header-to-the-request-in-a-servlet-filter
 * http://sandeepmore.com/blog/2010/06/12/modifying-http-headers-using-java/
 * http://bijubnair.blogspot.de/2008/12/adding-header-information-to-existing.html
 * <p>
 * Copyright: Copyright (c) 2021-05-26 11:21
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
	
	public HeaderMapRequestWrapper(HttpServletRequest request) {
		super(request);
	}
	
	/**
	 * 覆盖值表: 同名头只保留最后一次 setHeader 的值(评审报告 P2-9: 旧方法名叫 addHeader
	 * 但行为是覆盖; 现更名为 setHeader 让名字与行为一致, addHeader 保留为转发, 不破坏
	 * 既有子类与外部调用)。
	 * HTTP 头名大小写不敏感, 所以用 CASE_INSENSITIVE_ORDER 的 TreeMap(独立评审 X-1):
	 * 大小写敏感的 HashMap 会让先后两笔不同拼写的 setHeader 留下两个 key,
	 * 第三种拼写查询的结果取决于迭代顺序, 不确定。
	 */
	private final Map<String, String> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	
	/**
	 * 设置(覆盖)请求头。设置后 getHeader / getHeaders / getHeaderNames 三个读取口
	 * 看到的结果保持一致: 该头只剩这一个新值。
	 *
	 * @param name  头名
	 * @param value 头值
	 */
	public void setHeader(String name, String value) {
		headerMap.put(name, value);
	}
	
	/**
	 * @deprecated 名字暗示"追加"但实际是覆盖, 易误导(评审报告 P2-9)。
	 * 行为与 {@link #setHeader(String, String)} 完全一致, 请改用 setHeader。
	 */
	@Deprecated
	public void addHeader(String name, String value) {
		setHeader(name, value);
	}
	
	@Override
	public String getHeader(String name) {
		String headerValue = getOverride(name);
		return headerValue != null ? headerValue : super.getHeader(name);
	}
	
	/**
	 * 返回所有Header的名字(被 setHeader 过的头只出现一次, 不重复)
	 *
	 * @return
	 */
	@Override
	public Enumeration<String> getHeaderNames() {
		List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
		for (String name : headerMap.keySet()) {
			if (!containsIgnoreCase(names, name)) {
				names.add(name);
			}
		}
		return Collections.enumeration(names);
	}
	
	@Override
	public Enumeration<String> getHeaders(String name) {
		// 与 getHeader 保持同一语义: 有覆盖值时只返回覆盖值, 不再"原值+新值"叠加
		// (修复前 getHeader 返回 new 而 getHeaders 返回 [old, new], 同一头两个答案)
		String override = getOverride(name);
		if (override != null) {
			return Collections.enumeration(List.of(override));
		}
		return super.getHeaders(name);
	}
	
	/**
	 * 查覆盖值。headerMap 是 CASE_INSENSITIVE_ORDER 的 TreeMap, get 即大小写不敏感命中,
	 * 结果确定且与 setHeader 的写入拼写无关(独立评审 X-1 修复后不再需要线性扫描)
	 */
	private String getOverride(String name) {
		// CASE_INSENSITIVE_ORDER 比较器不接受 null(旧 HashMap 版本 get(null) 只是 miss), 读路径保持不抛
		return name == null ? null : headerMap.get(name);
	}
	
	private static boolean containsIgnoreCase(List<String> names, String name) {
		for (String existing : names) {
			if (existing != null && existing.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
}
