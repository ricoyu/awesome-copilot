package com.awesomecopilot.search8x.vo;

import com.awesomecopilot.common.lang.vo.Page;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Copyright: (C), 2021-02-23 21:26
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ElasticPage<T> extends Page implements Serializable {
	
	private static final long serialVersionUID = 4426577582458914214L;
	
	/**
	 * 这是查询返回的数据部分
	 */
	private List<T> results;
	
	/**
	 * 这是给下一次Search After查询用的sort
	 */
	private Object[] sort;
	
	/**
	 * 返回一个空的PageResult, results是一个空的不可变List, sort是一个长度为0的数组
	 * @return PageResult
	 */
	public static <T> ElasticPage<T> emptyResult() {
		ElasticPage<T> page = new ElasticPage<>();
		// TODO: Lombok should generate these setters
		// page.setResults(Collections.emptyList());
		// page.setSort(new Object[0]);
		return page;
	}
	
	/**
	 * Builder pattern support
	 */
	public static <T> ElasticPageBuilder<T> builder() {
		return new ElasticPageBuilder<>();
	}
	
	public static class ElasticPageBuilder<T> {
		private List<T> results;
		private Object[] sort;
		
		public ElasticPageBuilder<T> results(List<T> results) {
			this.results = results;
			return this;
		}
		
		public ElasticPageBuilder<T> sort(Object[] sort) {
			this.sort = sort;
			return this;
		}
		
		public ElasticPage<T> build() {
			ElasticPage<T> page = new ElasticPage<>();
			// TODO: Lombok should generate these setters
			// page.setResults(results);
			// page.setSort(sort);
			return page;
		}
	}
}
