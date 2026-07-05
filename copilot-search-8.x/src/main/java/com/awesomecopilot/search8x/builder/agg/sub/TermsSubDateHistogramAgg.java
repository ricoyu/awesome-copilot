package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.builder.agg.ElasticTermsAggregationBuilder;
import com.awesomecopilot.search8x.enums.CalendarInterval;
import com.awesomecopilot.search8x.enums.FixedInterval;

import java.util.List;
import java.util.Map;

/**
 * 为Term聚合创建子DateHistogram聚合时返回这个对象
 * <p>
 * Copyright: (C), 2021-07-13 14:42
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class TermsSubDateHistogramAgg extends SubDateHistogramAgg {
	
	private ElasticTermsAggregationBuilder aggregationBuilder;
	
	public TermsSubDateHistogramAgg(ElasticTermsAggregationBuilder aggregationBuilder, String name, String field) {
		this.aggregationBuilder = aggregationBuilder;
		this.name = name;
		this.field = field;
	}
	
	@Override
	public TermsSubDateHistogramAgg fixedInterval(Integer n, FixedInterval interval) {
		super.fixedInterval(n, interval);
		return this;
	}
	
	@Override
	public TermsSubDateHistogramAgg calendarInterval(CalendarInterval interval) {
		super.calendarInterval(interval);
		return this;
	}
	
	@Override
	public TermsSubDateHistogramAgg minDocCount(Integer minDocCount) {
		super.minDocCount(minDocCount);
		return this;
	}
	
	@Override
	public TermsSubDateHistogramAgg extendedBounds(Long minBound, Long maxBound) {
		super.extendedBounds(minBound, maxBound);
		return this;
	}
	
	@Override
	public ElasticTermsAggregationBuilder and() {
		Aggregation subDateHistogramAgg = build();
		ReflectionUtils.invokeMethod("subAggregation", aggregationBuilder, new SubAggregation() {
			@Override
			public Aggregation build() {
				return subDateHistogramAgg;
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
	 * @param <T>
	 * @return Map<String, T>
	 */
	public <T> List<Map<String, T>> thenGet() {
		return and().get();
	}
}
