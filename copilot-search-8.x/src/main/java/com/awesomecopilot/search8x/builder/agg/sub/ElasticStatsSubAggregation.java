package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

/**
 * Stats子聚合 
 * <p>
 * Copyright: Copyright (c) 2023-07-29 17:44
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticStatsSubAggregation extends SubAggregation {
	
	private SubAggregation parentAggregation;
	
	/**
	 * 要对哪个字段聚合
	 */
	protected String field;
	
	public ElasticStatsSubAggregation(String name, String field) {
		this.name = name;
		this.field = field;
	}
	
	public ElasticStatsSubAggregation(SubAggregation parentAggregation, String name, String field) {
		this.parentAggregation = parentAggregation;
		this.name = name;
		this.field = field;
	}
	
	@Override
	public Aggregation build() {
		return Aggregation.of(a -> a.stats(s -> s.field(field)));
	}
	
	@Override
	public SubAggregation and() {
		return parentAggregation;
	}
}
