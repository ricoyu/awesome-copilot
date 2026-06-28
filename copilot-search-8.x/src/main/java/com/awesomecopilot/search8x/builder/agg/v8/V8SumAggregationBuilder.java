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
 * Elasticsearch 8.x 原生 Sum Aggregation Builder
 * <p>
 * 使用 co.elastic.clients API 实现求和聚合，计算数值字段的总和
 * 
 * <p>
 * Copyright: (C), 2026-06-23
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class V8SumAggregationBuilder extends AbstractAggregationBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(V8SumAggregationBuilder.class);
	
	private V8SumAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static V8SumAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new V8SumAggregationBuilder(indices);
	}
	
	/**
	 * 设置聚合名称和字段
	 *
	 * @param name  聚合名称
	 * @param field 要计算总和的数值字段
	 */
	public V8SumAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}
	
	/**
	 * 设置查询条件
	 *
	 * @param queryBuilder 查询构建器
	 */
	public V8SumAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits 是否获取总命中数
	 */
	public V8SumAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	/**
	 * 添加子聚合
	 *
	 * @param subAggregation 子聚合
	 * @return 当前聚合构建器实例
	 */
	@Override
	public V8SumAggregationBuilder subAggregation(com.awesomecopilot.search8x.builder.agg.sub.SubAggregation subAggregation) {
		super.subAggregation(subAggregation);
		return this;
	}
	
	/**
	 * 构建 ES 8.x 原生 Sum Aggregation
	 */
	private Aggregation buildV8Aggregation() {
		// 使用 function-style API 创建 sum 聚合
		return new Aggregation.Builder()
			.sum(s -> s.field(field))
			.build();
	}
	
	/**
	 * 执行聚合并返回总和值
	 *
	 * @return Double 总和值，如果没有数据则返回 null
	 */
	public Double get() {
		// 构建 ES 8.x 聚合
		Map<String, Aggregation> aggregations = new HashMap<>();
		aggregations.put(name, buildV8Aggregation());
		
		// 使用 ES 8.x 客户端执行查询
		SearchResponse searchResponse = searchWithV8Client(aggregations);
		addTotalHitsToThreadLocal(searchResponse);
		
		// 使用 ES 8.x 原生解析器解析结果
		return V8AggResultSupport.sumResult(searchResponse.aggregations(), name);
	}
}
