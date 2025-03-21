package com.copilot.search.builder.agg.support;

import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;

/**
 * <p>
 * Copyright: (C), 2023-08-16 17:36
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class KeyUnboundToRange implements Range{
	
	private String key;
	
	private double to;
	
	public KeyUnboundToRange(String key, double to) {
		this.key = key;
		this.to = to;
	}
	
	@Override
	public void addRange(RangeAggregationBuilder rangeAggregationBuilder) {
		rangeAggregationBuilder.addUnboundedTo(key, to);
	}
}
