package com.copilot.search.builder.agg.sub;

import com.copilot.common.lang.utils.ReflectionUtils;
import com.copilot.search.builder.agg.ElasticDateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;

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
		AggregationBuilder avgAggregationBuilder = build();
		ReflectionUtils.invokeMethod("subAggregation", aggregationBuilder, avgAggregationBuilder);
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
