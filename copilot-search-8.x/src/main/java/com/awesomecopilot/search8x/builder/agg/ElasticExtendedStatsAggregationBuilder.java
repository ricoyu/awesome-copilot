package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.ExtendedStatsAggResult;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * extended_stats聚合, 比stats聚合多了平方和、方差、标准差、标准差界限
 * <p>
 * Copyright: (C), 2021-05-10 16:55
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticExtendedStatsAggregationBuilder extends AbstractAggregationBuilder implements ElasticAggregationBuilder {

	private static final Logger log = LoggerFactory.getLogger(ElasticExtendedStatsAggregationBuilder.class);

	private ElasticExtendedStatsAggregationBuilder(String[] indices) {
		this.indices = indices;
	}

	public static ElasticExtendedStatsAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticExtendedStatsAggregationBuilder(indices);
	}

	/**
	 * 给聚合起个名字, 后续获取聚合数据时需要
	 *
	 * @param name  聚合名称
	 * @param field 聚合字段
	 * @return ElasticExtendedStatsAggregationBuilder
	 */
	@Override
	public ElasticExtendedStatsAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}

	/**
	 * 聚合返回的结果中是否要包含总命中数
	 * @param fetchTotalHits 是否获取总命中数
	 * @return ElasticExtendedStatsAggregationBuilder
	 */
	public ElasticExtendedStatsAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}

	@Override
	public ElasticExtendedStatsAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}

	@Override
	public Aggregation build() {
		return Aggregation.of(a -> a.extendedStats(s -> s.field(field)));
	}

	public ExtendedStatsAggResult get() {
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
			log.error("Extended stats aggregation failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		Map<String, Aggregate> aggregations = searchResponse.aggregations();

		return V8AggResultSupport.extendedStatsResult(aggregations, name);
	}
}
