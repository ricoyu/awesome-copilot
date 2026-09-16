package com.awesomecopilot.networking.constants;

/**
 * <p>
 * Copyright: (C), 2021-03-22 11:36
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class MediaType {
	
	public static final String APPLICATION_JSON = "application/json";
	
	/**
	 * 表单提交时默认的Content-Type
	 */
	public static final String APPLICATION_FORM = "application/x-www-form-urlencoded";
	
	/**
	 * 带字符集声明的表单Content-Type(P2-7): 请求体按UTF-8编码, 就把UTF-8写进头里,
	 * 避免服务端按 ISO-8859-1 默认值解析中文表单字段
	 */
	public static final String APPLICATION_FORM_UTF8 = "application/x-www-form-urlencoded;charset=UTF-8";
	
	/**
	 * 表单提交时上传文件的话, Content-Type必须是这个
	 */
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";
}
