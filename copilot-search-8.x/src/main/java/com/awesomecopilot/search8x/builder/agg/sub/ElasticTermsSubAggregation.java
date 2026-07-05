package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

/**
 * Terms子聚合
 * <p>
 * Copyright: (C), 2021-08-23 15:54
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticTermsSubAggregation extends SubAggregation {
	
	private SubAggregation parentAggregation;
	
	/**
	 * 要对哪个字段聚合
	 */
	protected String field;
	
	private Integer size;
	
	public ElasticTermsSubAggregation(String name, String field) {
		this.name = name;
		this.field = field;
	}
	
	public ElasticTermsSubAggregation(SubAggregation parentAggregation, String name, String field) {
		this.parentAggregation = parentAggregation;
		this.name = name;
		this.field = field;
	}
	
	public ElasticTermsSubAggregation size(int size) {
		this.size = size;
		return this;
	}
	
	@Override
	public Aggregation build() {
		return Aggregation.of(a -> a.terms(t -> {
			t.field(field);
			if (this.size != null) {
				t.size(this.size);
			}
			return t;
		}));
	}
	
	@Override
	public SubAggregation and() {
		return parentAggregation;
	}
}
