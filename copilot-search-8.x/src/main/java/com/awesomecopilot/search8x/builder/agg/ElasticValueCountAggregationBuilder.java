package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.AggResultSupport;
import com.awesomecopilot.search8x.support.ValueCountAggResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Value Count 聚合, 统计某个字段的非空值数量(不做去重)
 * <p>
 * 区别于 cardinality 聚合, value_count 不做去重, 只统计非空值数量
 * <p>
 * Copyright: (C), 2021-06-18 9:32
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticValueCountAggregationBuilder extends AbstractAggregationBuilder implements ElasticAggregationBuilder {

	private static final Logger log = LoggerFactory.getLogger(ElasticValueCountAggregationBuilder.class);

	private ElasticValueCountAggregationBuilder(String... indices) {
		this.indices = indices;
	}

	public static ElasticValueCountAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticValueCountAggregationBuilder(indices);
	}

	/**
	 * 给聚合起个名字, 后续获取聚合数据时需要
	 *
	 * @param name  聚合名称
	 * @param field 聚合字段
	 * @return ElasticValueCountAggregationBuilder
	 */
	@Override
	public ElasticValueCountAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}

	@Override
	public ElasticValueCountAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}

	/**
	 * 聚合返回的结果中是否要包含总命中数
	 * @param fetchTotalHits 是否获取总命中数
	 * @return ElasticValueCountAggregationBuilder
	 */
	public ElasticValueCountAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}

	@Override
	public Aggregation build() {
		return Aggregation.of(a -> a.valueCount(v -> v.field(field)));
	}

	public ValueCountAggResult get() {
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
			log.error("Value count aggregation failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		Map<String, Aggregate> aggregations = searchResponse.aggregations();

		return AggResultSupport.valueCountResult(aggregations, name);
	}
}
