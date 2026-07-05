package com.awesomecopilot.search8x.builder.agg.support;

import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;

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
public class FromToRange implements Range {
	
	private double from;
	
	private double to;
	
	public FromToRange(double from, double to) {
		this.from = from;
		this.to = to;
	}
	
	@Override
	public AggregationRange toAggregationRange() {
		return AggregationRange.of(ar -> ar.from(from).to(to));
	}
}
