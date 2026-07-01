package com.awesomecopilot.search8x.vo;

import com.awesomecopilot.common.lang.vo.Page;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Copyright: (C), 2023-08-10 12:20
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ElasticScroll<T> extends Page {
	
	/**
	 * 这是查询返回的数据部分
	 */
	private List<T> results;
	
	private String scrollId;
	
	public static <T> ElasticScroll<T> emptyResult() {
		ElasticScroll<T> scroll = new ElasticScroll<>();
		// TODO: Lombok should generate these setters
		// scroll.setResults(Collections.emptyList());
		return scroll;
	}
	
	/**
	 * Builder pattern support
	 */
	public static <T> ElasticScrollBuilder<T> builder() {
		return new ElasticScrollBuilder<>();
	}
	
	public static class ElasticScrollBuilder<T> {
		private List<T> results;
		private String scrollId;
		
		public ElasticScrollBuilder<T> results(List<T> results) {
			this.results = results;
			return this;
		}
		
		public ElasticScrollBuilder<T> scrollId(String scrollId) {
			this.scrollId = scrollId;
			return this;
		}
		
		public ElasticScroll<T> build() {
			ElasticScroll<T> scroll = new ElasticScroll<>();
			scroll.setResults(results);
			scroll.setScrollId(scrollId);
			return scroll;
		}
	}
}
