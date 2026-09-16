package com.awesomecopilot.networking.enums;

/**
 * URL的scheme部分 
 * <p>
 * Copyright: Copyright (c) 2021-03-22 16:34
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public enum Scheme {
	
	HTTP, 
	
	HTTPS;
	
	@Override
	public String toString() {
		return this.name();
	}
	
	/**
	 * 将字符串转成Scheme枚举对象。
	 * <p>
	 * 评审报告 P2-11 修复：本方法此前是实例方法——语义上是"字符串→枚举"的静态工厂,
	 * 写成实例方法导致必须攥着一个 Scheme 才能解析另一个字符串(Scheme.HTTP.of("https")),
	 * 且返回 null 表示解析失败(不抛异常), 调用链上任何一处忘判空都会把 null 传下去。
	 * 改为静态方法(调用方式 Scheme.of("https")), 解析失败抛
	 * IllegalArgumentException——与仓库里 ContentType/HttpMethod 等枚举的 of 家族行为一致。
	 *
	 * @param schemeStr http / https(忽略大小写)
	 * @return Scheme
	 */
	public static Scheme of(String schemeStr) {
		for (Scheme scheme : Scheme.values()) {
			if (scheme.name().equalsIgnoreCase(schemeStr)) {
				return scheme;
			}
		}
		throw new IllegalArgumentException("不支持的scheme: " + schemeStr);
	}
}
