package com.awesomecopilot.networking.constants;

/**
 * <p>
 * Copyright: (C), 2021-03-16 17:49
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class HttpMethods {
	
	/*
	 * 评审报告 P2-10 修复：POST/PUT/DELETE/OPTIONS/TRACE 五个常量的值此前全部误写成
	 * "GET"(复制粘贴未改)。该类修复前零引用, 改值无行为影响, 只消除"哪天有人拿
	 * HttpMethods.POST 判 POST 永远为假"的隐患。与 enums.HttpMethod 职责重复,
	 * 长期建议合并删除。
	 */
	public static final String GET = "GET";
	
	public static final String POST = "POST";
	
	public static final String PUT = "PUT";
	
	public static final String DELETE = "DELETE";
	
	public static final String OPTIONS = "OPTIONS";
	
	public static final String TRACE = "TRACE";
	
	public static final String HEAD = "HEAD";
	
	public static final String PATCH = "PATCH";
	
}
