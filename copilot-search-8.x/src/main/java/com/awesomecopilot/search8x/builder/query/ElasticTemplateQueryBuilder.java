package com.awesomecopilot.search8x.builder.query;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.exception.ElasticQueryException;
import com.awesomecopilot.search8x.support.SearchHitsSupport;
import com.awesomecopilot.search8x.support.SearchResponseBridge;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Search Template 查询
 * <p>
 * Copyright: (C), 2021-06-11 11:37
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticTemplateQueryBuilder {
	
	private String[] indices;
	
	private String templateName;
	
	private Map<String, Object> params = new HashMap<>();
	
	private Class resultType;
	
	public ElasticTemplateQueryBuilder(String... indices) {
		this.indices = indices;
	}
	
	public ElasticTemplateQueryBuilder templateName(String templateName) {
		this.templateName = templateName;
		return this;
	}
	
	public ElasticTemplateQueryBuilder params(Map<String, Object> params) {
		this.params.putAll(params);
		return this;
	}
	
	public ElasticTemplateQueryBuilder param(String paramName, Object paramValue) {
		this.params.put(paramName, paramValue);
		return this;
	}
	
	public ElasticTemplateQueryBuilder resultType(Class resultType) {
		this.resultType = resultType;
		return this;
	}
	
	
	/**
	 * 执行查询
	 *
	 * @param <T>
	 * @return List<T>
	 */
	public <T> List<T> queryForList() {
		SearchHit[] hits = searchHits();
		
		if (hits.length == 0) {
			return Collections.emptyList();
		}
		
		return SearchHitsSupport.toList(hits, resultType);
	}
	
	
	/**
	 * 执行查询, 返回一条记录
	 *
	 * @param <T>
	 * @return T
	 */
	public <T> T queryForOne() {
		SearchHit[] hits = searchHits();
		
		if (hits.length == 0) {
			return null;
		}
		
		SearchHit hit = hits[0];
		String source = hit.getSourceAsString();
		
		if (source == null) {
			return null;
		}
		
		if (ReflectionUtils.isPojo(resultType)) {
			return (T) JacksonUtils.toObject(source, resultType);
		}
		
		return (T) source;
	}
	
	
	/**
	 * 作为查询的公共部分抽取出来
	 *
	 * @return
	 */
	private SearchHit[] searchHits() {
		SearchResponse response = SearchResponseBridge.searchTemplate(
				ElasticUtils.QUERY_CLIENT, indices, templateName, params);
		return response.getHits().getHits();
	}
}
