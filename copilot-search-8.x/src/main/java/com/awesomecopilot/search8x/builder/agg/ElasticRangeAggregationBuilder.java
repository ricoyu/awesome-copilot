package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregation;
import com.awesomecopilot.search8x.builder.agg.support.FromToRange;
import com.awesomecopilot.search8x.builder.agg.support.KeyFromToRange;
import com.awesomecopilot.search8x.builder.agg.support.KeyUnboundFromRange;
import com.awesomecopilot.search8x.builder.agg.support.KeyUnboundToRange;
import com.awesomecopilot.search8x.builder.agg.support.Range;
import com.awesomecopilot.search8x.builder.agg.support.UnboundFromRange;
import com.awesomecopilot.search8x.builder.agg.support.UnboundToRange;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.AggResultSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * https://www.elastic.co/guide/en/elasticsearch/reference/7.6/search-aggregations-bucket-datehistogram-aggregation.html
 * <p>
 * Copyright: (C), 2021-07-12 18:03
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticRangeAggregationBuilder extends AbstractAggregationBuilder implements ElasticAggregationBuilder, SubAggregatable, Compositable {
	
	private List<Range> ranges = new ArrayList<>();
	
	/**
	 * Add a new range to this aggregation.
	 *
	 * @param key  the key to use for this range in the response
	 * @param from the lower bound on the distances, inclusive
	 * @param to   the upper bound on the distances, exclusive
	 */
	public ElasticRangeAggregationBuilder addRange(String key, double from, double to) {
		if (isBlank(key)) {
			ranges.add(new FromToRange(from, to));
		} else {
			ranges.add(new KeyFromToRange(key, from, to));
		}
		return this;
	}
	
	/**
	 * Same as {@link #addRange(String, double, double)} but the key will be
	 * automatically generated based on <code>from</code> and
	 * <code>to</code>.
	 */
	public ElasticRangeAggregationBuilder addRange(double from, double to) {
		return addRange(null, from, to);
	}
	
	/**
	 * Add a new range with no lower bound.
	 *
	 * @param key the key to use for this range in the response
	 * @param to  the upper bound on the distances, exclusive
	 */
	public ElasticRangeAggregationBuilder addUnboundedTo(String key, double to) {
		if (isNotBlank(key)) {
			ranges.add(new KeyUnboundToRange(key, to));
		} else {
			ranges.add(new UnboundToRange(to));
		}
		return this;
	}
	
	/**
	 * Same as {@link #addUnboundedTo(String, double)} but the key will be
	 * computed automatically.
	 */
	public ElasticRangeAggregationBuilder addUnboundedTo(double to) {
		return addUnboundedTo(null, to);
	}
	
	/**
	 * Add a new range with no upper bound.
	 *
	 * @param key  the key to use for this range in the response
	 * @param from the lower bound on the distances, inclusive
	 */
	public ElasticRangeAggregationBuilder addUnboundedFrom(String key, double from) {
		if (isNotBlank(key)) {
			ranges.add(new KeyUnboundFromRange(key, from));
		} else {
			ranges.add(new UnboundFromRange(from));
		}
		return this;
	}
	
	/**
	 * Same as {@link #addUnboundedFrom(String, double)} but the key will be
	 * computed automatically.
	 */
	public ElasticRangeAggregationBuilder addUnboundedFrom(double from) {
		return addUnboundedFrom(null, from);
	}
	
	
	private ElasticRangeAggregationBuilder(String[] indices) {
		this.indices = indices;
	}

	public static ElasticRangeAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticRangeAggregationBuilder(indices);
	}
	
	/**
	 * 给聚合起个名字, 后续获取聚合数据时需要
	 *
	 * @param name
	 * @param field
	 * @return ElasticHistogramAggregationBuilder
	 */
	@Override
	public ElasticRangeAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}
	
	@Override
	public ElasticRangeAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits
	 * @return ElasticDateHistogramAggregationBuilder
	 */
	public ElasticRangeAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	@Override
	public ElasticRangeAggregationBuilder subAggregation(SubAggregation subAggregation) {
		subAggregations.add(subAggregation);
		return this;
	}
	
	@Override
	public Aggregation build() {
		List<AggregationRange> aggRanges = ranges.stream()
				.map(Range::toAggregationRange)
				.collect(Collectors.toList());
		
		Map<String, Aggregation> subAggsMap = buildSubAggregationsMap(subAggregations);
		return Aggregation.of(a -> {
			var cb = a.range(r -> {
				r.field(field).ranges(aggRanges);
				return r;
			});
			if (subAggsMap != null) {
				cb.aggregations(subAggsMap);
			}
			return a;
		});
	}
	
	@Override
	public ElasticCompositeAggregationBuilder and() {
		// 使用 this.name 作为聚合名称
		compositeAggregationBuilder.add(this.name, build());
		return compositeAggregationBuilder;
	}
	
	@SuppressWarnings({"unchecked"})
	public <T> Map<String, T> get() {
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
			log.error("Range aggregation failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations = searchResponse.aggregations();
		
		return (Map<String, T>) AggResultSupport.rangeResult(aggregations, name);
	}
}
