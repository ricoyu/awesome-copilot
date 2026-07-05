package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 这个聚合是对字段去重后统计数量
 * <p>
 * Copyright: (C), 2021-06-18 9:32
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticCardinalityAggregationBuilder extends AbstractAggregationBuilder implements ElasticAggregationBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticCardinalityAggregationBuilder.class);
	
	private ElasticCardinalityAggregationBuilder(String... indices) {
		this.indices = indices;
	}
	
	public static ElasticCardinalityAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticCardinalityAggregationBuilder(indices);
	}
	
	/**
	 * 给聚合起个名字, 后续获取聚合数据时需要
	 *
	 * @param name
	 * @return ElasticCardinalityAggregationBuilder
	 */
	@Override
	public ElasticCardinalityAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}
	
	@Override
	public ElasticCardinalityAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数 
	 * @param fetchTotalHits
	 * @return ElasticCardinalityAggregationBuilder
	 */
	public ElasticCardinalityAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	@Override
	public Aggregation build() {
		return Aggregation.of(a -> a.cardinality(c -> c.field(field)));
	}
	
	public Long get() {
		Aggregation aggregation = build();
		
		SearchRequest.Builder builder = searchRequestBuilder();
		builder.aggregations(name, aggregation).size(0);
		SearchRequest request = builder.build();
		logDsl(request);
		
		SearchResponse<Map> searchResponse = null;
		try {
			searchResponse = ElasticUtils.QUERY_CLIENT.search(request, Map.class);
		} catch (Exception e) {
			String errorDetail = extractEsErrorDetails(e);
			log.error("Cardinality aggregation failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations = searchResponse.aggregations();
		
		return V8AggResultSupport.cardinalityResult(aggregations, name);
	}
}
