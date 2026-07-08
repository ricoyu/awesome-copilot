package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 组合多个聚合
 * https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/_structuring_aggregations.html
 * <p>
 * Copyright: (C), 2021-06-18 21:21
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticCompositeAggregationBuilder extends AbstractAggregationBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticCompositeAggregationBuilder.class);
	
	/**
	 * 保存聚合名称和 Aggregation 对象的映射关系
	 */
	private Map<String, Aggregation> namedAggregations = new HashMap<>();
	
	private ElasticCompositeAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static ElasticCompositeAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticCompositeAggregationBuilder(indices);
	}
	
	void add(String name, Aggregation builder) {
		this.namedAggregations.put(name, builder);
	}
	
	@Override
	public ElasticCompositeAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	public TermAggregationBuilder terms(String name, String field) {
		ElasticTermsAggregationBuilder builder = ElasticTermsAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		// 将 terms 聚合添加到 namedAggregations 中
		add(name, builder.build());
		return builder;
	}
	
	public ElasticCompositeAggregationBuilder avg(String name, String field) {
		ElasticAggregationBuilder builder = ElasticAvgAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		Aggregation aggregation = ((ElasticAvgAggregationBuilder) builder).build();
		this.add(name, aggregation);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder cardinality(String name, String field) {
		ElasticAggregationBuilder builder = ElasticCardinalityAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		Aggregation aggregation = ((ElasticCardinalityAggregationBuilder) builder).build();
		this.add(name, aggregation);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder max(String name, String field) {
		ElasticAggregationBuilder builder = ElasticMaxAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		Aggregation aggregation = ((ElasticMaxAggregationBuilder) builder).build();
		this.add(name, aggregation);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder min(String name, String field) {
		ElasticAggregationBuilder builder = ElasticMinAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		Aggregation aggregation = ((ElasticMinAggregationBuilder) builder).build();
		this.add(name, aggregation);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder sum(String name, String field) {
		ElasticAggregationBuilder builder = ElasticSumAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		Aggregation aggregation = ((ElasticSumAggregationBuilder) builder).build();
		this.add(name, aggregation);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder valueCount(String name, String field) {
		ElasticAggregationBuilder builder = ElasticValueCountAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		Aggregation aggregation = ((ElasticValueCountAggregationBuilder) builder).build();
		this.add(name, aggregation);
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数 
	 * @param fetchTotalHits
	 * @return ElasticCompositeAggregationBuilder
	 */
	public ElasticCompositeAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	public <T> Map<String, T> get() {
		SearchRequest.Builder builder = searchRequestBuilder();
		// 使用用户设置的聚合名称而不是 UUID
		for (Map.Entry<String, Aggregation> entry : namedAggregations.entrySet()) {
			builder.aggregations(entry.getKey(), entry.getValue());
		}
		builder.size(0);
		SearchRequest request = builder.build();
		
		logDsl(request);
		
		SearchResponse<Map> searchResponse = null;
		try {
			searchResponse = ElasticUtils.QUERY_CLIENT.search(request, Map.class);
		} catch (Exception e) {
			String errorDetail = extractEsErrorDetails(e);
			log.error("Composite aggregation failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		
		Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations = searchResponse.aggregations();
		Map<String, Object> resultMap = V8AggResultSupport.compositeResult(aggregations);
		
		return (Map<String, T>) resultMap;
	}
}
