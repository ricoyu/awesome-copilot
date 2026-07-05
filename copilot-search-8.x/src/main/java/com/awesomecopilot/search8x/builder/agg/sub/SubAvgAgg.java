package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

/**
 * <p>
 * Copyright: (C), 2021-08-18 14:45
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class SubAvgAgg {
	
	/**
	 * 聚合名字
	 */
	protected String name;
	
	/**
	 * 要对哪个字段聚合
	 */
	protected String field;
	
	
	protected Aggregation build() {
		return Aggregation.of(a -> a.avg(avg -> avg.field(field)));
	}
	
	public abstract Object and();
}
