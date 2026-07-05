package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示这是一个子聚合
 * 子聚合还可以添加子聚合
 * <p>
 * Copyright: (C), 2021-08-20 15:19
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class SubAggregation {
	
	/**
	 * 聚合名字
	 */
	protected String name;
	
	/**
	 * 这里表示aggregationBuilder的子聚合
	 */
	protected final List<SubAggregation> subAggregations = new ArrayList<>();
	
	public SubAggregation() {
		
	}

	/**
	 * 返回聚合名字
	 *
	 * @return 聚合名字
	 */
	public String getName() {
		return name;
	}

	/**
	 * 为当前子聚合继续添加子聚合。
	 *
	 * <p>注意：这里不强制校验 subAggregation 类型是否合法（例如 pipeline agg 不能再挂子聚合），
	 * 构建时由子类决定是否真正挂载。</p>
	 *
	 * @param subAggregation 子聚合
	 * @return 当前 SubAggregation
	 */
	public SubAggregation subAggregation(SubAggregation subAggregation) {
		if (subAggregation != null) {
			subAggregations.add(subAggregation);
		}
		return this;
	}
	
	/**
	 * 真正开始构建Elasticsearch的Aggregation, 子聚合关系都建立起来了
	 * @return Aggregation
	 */
	public abstract Aggregation build();
	
	/**
	 * 在子聚合中嵌套子聚合时, API调用可以通过and()返回上一次的子聚合, <br/>
	 * 然后上一级的子聚合可以继续添加子聚合
	 * @return SubAggregation
	 */
	public abstract SubAggregation and();
	
	/**
	 * 递归构建子聚合Map, 用于挂载到Aggregation.Builder.aggregations()
	 *
	 * @param subs 子聚合列表
	 * @return 子聚合Map (name -> Aggregation), 如果没有子聚合返回null
	 */
	protected Map<String, Aggregation> buildSubAggregationsMap(List<SubAggregation> subs) {
		if (subs == null || subs.isEmpty()) {
			return null;
		}
		Map<String, Aggregation> subAggsMap = new HashMap<>();
		for (SubAggregation sub : subs) {
			Aggregation subAgg = sub.build();
			if (subAgg != null) {
				subAggsMap.put(sub.name, subAgg);
			}
		}
		return subAggsMap.isEmpty() ? null : subAggsMap;
	}
}
