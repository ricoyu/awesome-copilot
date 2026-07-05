package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Copyright: (C), 2021-08-26 10:39
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class SubAggregationSupport {
	
	/**
	 * 将SubAggregation列表构建为Aggregation Map, 用于挂载到Aggregation的aggregations()方法
	 *
	 * @param subAggregations 子聚合列表
	 * @return 子聚合Map (name -> Aggregation), 如果没有子聚合返回null
	 */
	public static Map<String, Aggregation> buildSubAggregationsMap(List<SubAggregation> subAggregations) {
		if (subAggregations == null || subAggregations.isEmpty()) {
			return null;
		}
		Map<String, Aggregation> subAggsMap = new HashMap<>();
		for (SubAggregation sub : subAggregations) {
			Aggregation subAgg = sub.build();
			if (subAgg != null) {
				subAggsMap.put(sub.name, subAgg);
			}
		}
		return subAggsMap.isEmpty() ? null : subAggsMap;
	}
}
