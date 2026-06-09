package com.awesomecopilot.search8x.builder.agg;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.AggResultSupport;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
	
	private List<AggregationBuilder> builders = new ArrayList<>();
	
	private ElasticCompositeAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static ElasticCompositeAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticCompositeAggregationBuilder(indices);
	}
	
	void add(AggregationBuilder builder) {
		this.builders.add(builder);
	}
	
	@Override
	public ElasticCompositeAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	public TermAggregationBuilder terms(String name, String field) {
		ElasticTermsAggregationBuilder builder = ElasticTermsAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		return builder;
	}
	
	public ElasticCompositeAggregationBuilder avg(String name, String field) {
		ElasticAggregationBuilder builder = ElasticAvgAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		AggregationBuilder aggregationBuilder = ((ElasticAvgAggregationBuilder) builder).build();
		this.add(aggregationBuilder);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder cardinality(String name, String field) {
		ElasticAggregationBuilder builder = ElasticCardinalityAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		AggregationBuilder aggregationBuilder = ((ElasticCardinalityAggregationBuilder) builder).build();
		this.add(aggregationBuilder);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder max(String name, String field) {
		ElasticAggregationBuilder builder = ElasticMaxAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		AggregationBuilder aggregationBuilder = ((ElasticMaxAggregationBuilder) builder).build();
		this.add(aggregationBuilder);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder min(String name, String field) {
		ElasticAggregationBuilder builder = ElasticMinAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		AggregationBuilder aggregationBuilder = ((ElasticMinAggregationBuilder) builder).build();
		this.add(aggregationBuilder);
		return this;
	}
	
	public ElasticCompositeAggregationBuilder sum(String name, String field) {
		ElasticAggregationBuilder builder = ElasticSumAggregationBuilder.instance(indices).of(name, field);
		ReflectionUtils.setField("compositeAggregationBuilder", builder, this);
		
		AggregationBuilder aggregationBuilder = ((ElasticSumAggregationBuilder) builder).build();
		this.add(aggregationBuilder);
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
		SearchSourceBuilder sourceBuilder = searchSourceBuilder();
		for (AggregationBuilder builder : builders) {
			sourceBuilder.aggregation(builder);
		}
		SearchResponse searchResponse = executeSearch(sourceBuilder);
		addTotalHitsToThreadLocal(searchResponse);
		
		Aggregations aggregations = searchResponse.getAggregations();
		Map<String, Object> resultMap = AggResultSupport.compositeResult(aggregations);
		
		return (Map<String, T>)resultMap;
	}
}