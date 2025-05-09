package com.awesomecopilot.common.lang.vo;

import com.awesomecopilot.common.lang.errors.ErrorType;
import com.awesomecopilot.common.lang.errors.ErrorTypes;

/**
 * <blockquote><pre>
 * status    请求接口状态码
 * code      数据请求状态码
 * message   数据请求返回消息
 * data      单个数据类
 * results   数组类型
 *
 * 情况1  status==200&&code==0 的时候请求接口数据成功, 解析数据
 * 情况2  status!=200          直接捕获异常 exception
 * 情况3  status＝200 code!=0  弹框message给用户
 * </pre></blockquote>
 * <p>
 * 泛型用法
 * <pre> {@code
 * Results.<StorageDTO>success().data(storageDTO)
 * Results.<List<StorageDTO>>success().data(storageDTOs)
 * }</pre>
 * Copyright: Copyright (c) 2019-10-14 15:54
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class Results {

	private Results() {
	}

	/**
	 * 泛型化是为了微服务之间调用时, data是POJO的情况可以反序列化成POJO, 即达到调用方拿到的result.data是正确的POJO类型而不是LinkedHashMap
	 * @param <T>
	 */
	public static class Builder<T> {
		private final Result<T> result = new Result<>();

		/**
		 * 请求接口状态码, 0代表成功, 非0代表失败
		 */
		private String code = "0";

		/**
		 * message表示在API调用失败的情况下详细的错误信息, 这个信息可以由客户端直接呈现给用户
		 * 调用成功则固定为null；
		 * 因为message还会承载详细的验证错误信息, 所以用Object类型, 因为验证错误信息可能是一个List<String[]>类型的
		 * 如果message用String类型的, 那么在服务端就要先对错误信息进行序列化成String, 最后再输出到前端页面前会再序列化一次, 所以字符串的双引号就会多一个转义符, 像这样
		 * "message": "[[\"name\",\"俞雪华 dev\"],[\"age\",\"18\"]]",
		 */
		private Object message = null;

		/**
		 * success error
		 */
		private String status;

		/**
		 * 单个数据或集合类型对象
		 */
		private T data;

		private Page page;

		/**
		 * 设置status code和message
		 *
		 * @param code    设置请求状态代码
		 * @param message 设置返回消息描述
		 * @return
		 */
		public Builder<T> status(String code, Object message) {
			this.code = code;
			if (!ErrorTypes.SUCCESS.code().equals(code)) {
				this.status = "fail";
			} else {
				this.status = "success";
			}
			this.message = message;
			return this;
		}

		public Builder<T> status(ErrorType errorType) {
			if (errorType == ErrorTypes.SUCCESS) {
				this.status = "success";
				return this;
			} else {
				return status(errorType.code(), errorType.message());
			}
		}

		public Builder<T> message(Object message) {
			this.message = message;
			return this;
		}

		/**
		 * 设置返回数据
		 * 泛型化是为了微服务之间调用时, data是POJO的情况可以反序列化成POJO, 即达到调用方拿到的result.data是正确的POJO类型而不是LinkedHashMap
		 *
		 * @param data 返回的实际数据
		 * @return Result
		 */
		public Builder<T> data(T data) {
			this.data = data;
			return this;
		}

		/**
		 * 分页支持
		 *
		 * @param page
		 * @return Builder
		 */
		public Builder<T> page(Page page) {
			this.page = page;
			return this;
		}

		public Result<T> build() {
			result.setMessage(message);
			result.setCode(code);
			result.setData(data);
			result.setPage(page);
			result.setStatus(status);
			return result;
		}
	}

	/**
	 * 设置status 200 OK
	 *
	 * @return Builder
	 */
	public static <T> Builder<T> success() {
		Builder<T> builder = new Builder<>();
		builder.status(ErrorTypes.SUCCESS);
		return builder;
	}

	/**
	 * 设置status 200 OK
	 *
	 * @return Builder
	 */
	public static <T> Builder<T> fail() {
		Builder<T> builder = new Builder<>();
		builder.status(ErrorTypes.FAIL);
		return builder;
	}

	public static <T> Builder<T> status(String code, Object message) {
		Builder<T> builder = new Builder<>();
		return builder.status(code, message);
	}

	public static <T> Builder<T> status(ErrorType errorType) {
		Builder<T> builder = new Builder<>();
		return builder.status(errorType.code(), errorType.message());
	}

}
