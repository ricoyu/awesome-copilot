package com.awesomecopilot.networking.utils;

import com.awesomecopilot.networking.builder.FormRequestBuilder;
import com.awesomecopilot.networking.builder.JsonRequestBuilder;
import com.awesomecopilot.networking.constants.MediaType;
import com.awesomecopilot.networking.enums.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

/**
 * 网络相关操作帮助类
 * <p>
 * Copyright: (C), 2019/12/25 10:49
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class HttpUtils {

	private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
	
	private static String EMPTY_STR = "";
	private static String UTF_8 = "UTF-8";

	private HttpUtils() {

	}
	/**
	 * 执行HTTP GET请求并返回结果, Content-Type默认application-json
	 *
	 * @param url
	 * @return JsonRequestBuilder
	 */
	/**
	 * 执行HTTP GET请求并返回结果。
	 * <p>
	 * 评审报告 P2-5 修复：不再预置 Content-Type: application/json——Content-Type 描述的是
	 * "请求体的类型", GET 没有请求体, 带这个头毫无意义, 个别严格校验的网关还会因此拒绝请求。
	 * 需要声明响应类型请用 Accept; 确有携带请求体的非常规用法, 自行调用 addHeader 指定。
	 *
	 * @param url 完整URL
	 * @return JsonRequestBuilder
	 */
	public static JsonRequestBuilder get(String url) {
		JsonRequestBuilder requestBuilder = new JsonRequestBuilder();
		requestBuilder.method(HttpMethod.GET);
		requestBuilder.url(url);
		return requestBuilder;
	}

	/**
	 * 执行HTTP GET请求并返回结果, Content-Type默认application-json
	 * 这个不带url的版本是因为要支持后续单独指定host, port, path来构建请求, 如:
	 * <pre> {@code
	 * String response2 = HttpUtils.get()
	 *         .scheme(Scheme.HTTP)
	 *         .host("192.168.100.101")
	 *         .port(9200)
	 *         .path("/rico/_mapping")
	 *         .request();
	 * }</pre>
	 * @return JsonRequestBuilder
	 */
	public static JsonRequestBuilder get() {
		JsonRequestBuilder requestBuilder = new JsonRequestBuilder();
		requestBuilder.method(HttpMethod.GET);
		return requestBuilder;
	}

	/**
	 * 执行HTTP POST请求并返回结果, Content-Type默认application-json
	 * 这个不带url的版本是因为要支持后续单独指定host, port, path来构建请求, 如:
	 * <pre> {@code
	 * Object result = HttpUtils.post()
	 * 				.method(HttpMethod.PUT)
	 * 				.path("_ingest/pipeline/blog_pipeline")
	 * 				.host("192.168.100.101")
	 * 				.port(9200)
	 * 				.body("")
	 * 				.request();
	 * }</pre>
	 *
	 * @return JsonRequestBuilder
	 */
	public static JsonRequestBuilder post() {
		JsonRequestBuilder requestBuilder = new JsonRequestBuilder();
		requestBuilder.method(HttpMethod.POST);
		requestBuilder.addHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON);
		return requestBuilder;
	}

	/**
	 * 执行HTTP POST请求并返回结果, Content-Type默认application-json
	 *
	 * @param url
	 * @return JsonRequestBuilder
	 */
	public static JsonRequestBuilder post(String url) {
		JsonRequestBuilder requestBuilder = new JsonRequestBuilder();
		requestBuilder.method(HttpMethod.POST);
		requestBuilder.url(url);
		requestBuilder.addHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON);
		return requestBuilder;
	}
	
	/**
	 * 执行HTTP PUT请求并返回结果, Content-Type默认application-json
	 *
	 * @return JsonRequestBuilder
	 */
	public static JsonRequestBuilder put(String url) {
		JsonRequestBuilder requestBuilder = new JsonRequestBuilder();
		requestBuilder.method(HttpMethod.PUT);
		requestBuilder.url(url);
		requestBuilder.addHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON);
		return requestBuilder;
	}
	
	/**
	 * 执行HTTP DELETE请求并返回结果, Content-Type默认application-json
	 *
	 * @return JsonRequestBuilder
	 */
	public static JsonRequestBuilder delete(String url) {
		JsonRequestBuilder requestBuilder = new JsonRequestBuilder();
		requestBuilder.method(HttpMethod.DELETE);
		requestBuilder.url(url);
		// P2-5: DELETE 默认无请求体, 不预置 Content-Type（需要带body时自行 addHeader + 走支持实体的方法）
		return requestBuilder;
	}

	/**
	 * 执行HTTP表单提交, Content-Type默认application/x-www-form-urlencoded
	 *
	 * @param url
	 * @return FormRequestBuilder
	 */
	public static FormRequestBuilder form(String url) {
		FormRequestBuilder builder = new FormRequestBuilder();
		builder.url(url);
		builder.method(HttpMethod.POST);
		/*
		 * 评审报告 P2-7 修复：Content-Type 带上 charset=UTF-8。
		 * 旧值 application/x-www-form-urlencoded 不带字符集, 而 HTTP 表单规范里
		 * 未声明字符集时服务端按 ISO-8859-1 解析是常见行为; 本框架的请求体实际按
		 * UTF-8 编码, 英文数据碰巧一致, 中文数据在严格按规范解析的服务端上会乱码,
		 * 且"英文正常、中文出错"最难排查。声明写进头里, 两边对齐。
		 */
		builder.addHeader(CONTENT_TYPE, MediaType.APPLICATION_FORM_UTF8);
		return builder;
	}
	
}
