package com.awesomecopilot.search8x.builder.agg.v8;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.builder.agg.AbstractAggregationBuilder;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 8.x 原生 Composite Aggregation Builder
 * <p>
 * 使用 co.elastic.clients API 实现组合聚合，将多个聚合作为一个整体返回
 * 
 * <p>
 * Copyright: (C), 2026-06-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class V8CompositeAggregationBuilder extends AbstractAggregationBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(V8CompositeAggregationBuilder.class);
	
	/**
	 * 子聚合 Map (name -> Aggregation)
	 */
	private Map<String, Aggregation> subAggregations = new HashMap<>();
	
	private V8CompositeAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static V8CompositeAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new V8CompositeAggregationBuilder(indices);
	}
	
	/**
	 * 设置查询条件
	 *
	 * @param queryBuilder 查询构建器
	 */
	public V8CompositeAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 添加 Terms 子聚合
	 *
	 * @param name  聚合名称
	 * @param field 字段名
	 */
	public V8CompositeAggregationBuilder addTerms(String name, String field) {
		Aggregation termsAgg = new Aggregation.Builder()
			.terms(t -> t.field(field))
			.build();
		subAggregations.put(name, termsAgg);
		return this;
	}
	
	/**
	 * 添加 Avg 子聚合
	 *
	 * @param name  聚合名称
	 * @param field 字段名
	 */
	public V8CompositeAggregationBuilder addAvg(String name, String field) {
		Aggregation avgAgg = new Aggregation.Builder()
			.avg(a -> a.field(field))
			.build();
		subAggregations.put(name, avgAgg);
		return this;
	}
	
	/**
	 * 添加 Sum 子聚合
	 *
	 * @param name  聚合名称
	 * @param field 字段名
	 */
	public V8CompositeAggregationBuilder addSum(String name, String field) {
		Aggregation sumAgg = new Aggregation.Builder()
			.sum(s -> s.field(field))
			.build();
		subAggregations.put(name, sumAgg);
		return this;
	}
	
	/**
	 * 添加 Min 子聚合
	 *
	 * @param name  聚合名称
	 * @param field 字段名
	 */
	public V8CompositeAggregationBuilder addMin(String name, String field) {
		Aggregation minAgg = new Aggregation.Builder()
			.min(m -> m.field(field))
			.build();
		subAggregations.put(name, minAgg);
		return this;
	}
	
	/**
	 * 添加 Max 子聚合
	 *
	 * @param name  聚合名称
	 * @param field 字段名
	 */
	public V8CompositeAggregationBuilder addMax(String name, String field) {
		Aggregation maxAgg = new Aggregation.Builder()
			.max(m -> m.field(field))
			.build();
		subAggregations.put(name, maxAgg);
		return this;
	}
	
	/**
	 * 添加 Cardinality 子聚合
	 *
	 * @param name  聚合名称
	 * @param field 字段名
	 */
	public V8CompositeAggregationBuilder addCardinality(String name, String field) {
		Aggregation cardinalityAgg = new Aggregation.Builder()
			.cardinality(c -> c.field(field))
			.build();
		subAggregations.put(name, cardinalityAgg);
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits 是否获取总命中数
	 */
	public V8CompositeAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	/**
	 * 执行聚合并返回所有子聚合结果
	 *
	 * @return Map<String, Object> 包含所有子聚合结果的 Map
	 */
	public <T> Map<String, T> get() {
		// 使用 ES 8.x 客户端执行查询
		SearchResponse searchResponse = searchWithV8Client(subAggregations);
		addTotalHitsToThreadLocal(searchResponse);
		
		// 使用 ES 8.x 原生解析器解析结果
		return V8AggResultSupport.compositeResult(searchResponse.aggregations());
	}
}
