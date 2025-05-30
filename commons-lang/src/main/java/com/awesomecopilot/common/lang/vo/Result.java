package com.awesomecopilot.common.lang.vo;

import com.awesomecopilot.common.lang.exception.ServiceException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * REST通用输出结果封装
 * 泛型化是为了微服务之间调用时, data是POJO的情况可以反序列化成POJO, 即达到调用方拿到的result.data是正确的POJO类型而不是LinkedHashMap
 * <p>
 * Copyright: Copyright (c) 2020-08-17 16:54
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class Result<T> {

	/**
	 * 调用成功的状态码
	 */
	private static final String SUCCESS_CODE = "0";

	private static final Logger log = LoggerFactory.getLogger(Result.class);
	
	/**
	 * 请求接口状态码, 0表示成功
	 */
	private String code = "0";
	
	/**
	 * success error
	 */
	private String status;

	/**
	 * message表示在API调用失败的情况下详细的错误信息, 这个信息可以由客户端直接呈现给用户
	 * 调用成功则固定为null；
	 * 数据校验失败时的验证错误也放到这个字段里面
	 * 因为message还会承载详细的验证错误信息, 所以用Object类型, 因为验证错误信息可能是一个List<String[]>类型的
	 * 如果message用String类型的, 那么在服务端就要先对错误信息进行序列化成String, 最后再输出到前端页面前会再序列化一次, 所以字符串的双引号就会多一个转义符, 像这样
	 * "message": "[[\"name\",\"俞雪华 dev\"],[\"age\",\"18\"]]",
	 */
	private Object message;
	
	/**
	 * 输出的调试信息
	 */
	//private Object debugMessage;
	
	/**
	 * 返回的数据
	 */
	private T data;
	
	/**
	 * 分页对象, 如果没有分页, 这个字段不会输出到json串中
	 */
	private Page page;
	
	public <K, V> Result put(K key, V value) {
		if (data instanceof Map) {
			Map<K, V> map = (Map<K, V>)data;
			map.put(key, value);
		} else {
			log.warn("data is not a Map, cannot call put(k, v)");
		}
		return this;
	}

	public T getData() {
		return this.data;
	}
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Object getMessage() {
		return message;
	}


	public void setMessage(Object message) {
		this.message = message;
	}

	public void setData(T data) {
		this.data = data;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	/**
	 * 判断调用是否成功
	 * @return boolean
	 */
	public boolean isSuccess() {
		return SUCCESS_CODE.equals(code);
	}

	/**
	 * 判断是否有异常。如果有，则抛出 {@link ServiceException} 异常
	 */
	public void checkError() throws ServiceException {
		if (isSuccess()) {
			return;
		}
		// 业务异常
		throw new ServiceException(code, (String) message);
	}

	/**
	 * 判断是否有异常。如果有，则抛出 {@link ServiceException} 异常
	 * 如果没有，则返回 {@link #data} 数据
	 */
	@JsonIgnore // 避免 jackson 序列化
	public T getCheckedData() {
		checkError();
		return data;
	}
}
