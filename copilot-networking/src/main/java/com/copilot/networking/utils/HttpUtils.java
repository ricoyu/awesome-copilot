package com.copilot.networking.utils;

import com.copilot.networking.enums.HttpMethod;
import com.copilot.networking.builder.FormRequestBuilder;
import com.copilot.networking.builder.JsonRequestBuilder;
import com.copilot.networking.constants.MediaType;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public final class HttpUtils {
	
	private static String EMPTY_STR = "";
	private static String UTF_8 = "UTF-8";
	
	/**
	 * 执行HTTP GET请求并返回结果, Content-Type默认application-json
	 *
	 * @param url
	 * @return JsonRequestBuilder
	 */
	public static JsonRequestBuilder get(String url) {
		JsonRequestBuilder requestBuilder = new JsonRequestBuilder();
		requestBuilder.method(HttpMethod.GET);
		requestBuilder.url(url);
		requestBuilder.addHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON);
		return requestBuilder;
	}
	
	/**
	 * 执行HTTP GET请求并返回结果, Content-Type默认application-json
	 *
	 * @return JsonRequestBuilder
	 */
	public static JsonRequestBuilder get() {
		JsonRequestBuilder requestBuilder = new JsonRequestBuilder();
		requestBuilder.method(HttpMethod.GET);
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
		requestBuilder.addHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON);
		return requestBuilder;
	}
	
	/**
	 * 执行HTTP POST请求并返回结果, Content-Type默认application-json
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
	 * 执行HTTP表单提交, Content-Type默认application/x-www-form-urlencoded
	 *
	 * @param url
	 * @return FormRequestBuilder
	 */
	public static FormRequestBuilder form(String url) {
		FormRequestBuilder builder = new FormRequestBuilder();
		builder.url(url);
		builder.method(HttpMethod.POST);
		builder.addHeader(CONTENT_TYPE, MediaType.APPLICATION_FORM);
		return builder;
	}
	
}
