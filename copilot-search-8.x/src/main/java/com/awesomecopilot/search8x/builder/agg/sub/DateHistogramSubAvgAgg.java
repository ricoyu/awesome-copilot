package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.builder.agg.ElasticDateHistogramAggregationBuilder;

import java.util.Map;

/**
 * <p>
 * Copyright: (C), 2021-08-18 14:46
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class DateHistogramSubAvgAgg extends SubAvgAgg {
	
	private ElasticDateHistogramAggregationBuilder aggregationBuilder;
	
	public DateHistogramSubAvgAgg(ElasticDateHistogramAggregationBuilder aggregationBuilder, String name, String field) {
		this.aggregationBuilder = aggregationBuilder;
		this.name = name;
		this.field = field;
	}
	
	@Override
	public ElasticDateHistogramAggregationBuilder and() {
		Aggregation avgAggregation = build();
		ReflectionUtils.invokeMethod("subAggregation", aggregationBuilder, new SubAggregation() {
			@Override
			public Aggregation build() {
				return avgAggregation;
			}
			@Override
			public SubAggregation and() {
				return this;
			}
		});
		return aggregationBuilder;
	}
	
	/**
	 * 代理主聚合的GET操作
	 *
	 * @param <T>
	 * @return Map<String, T>
	 */
	public <T> Map<String, T> thenGet() {
		return and().get();
	}
}
