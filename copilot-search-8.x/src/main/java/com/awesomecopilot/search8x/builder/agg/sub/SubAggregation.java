package com.awesomecopilot.search8x.builder.agg.sub;

import org.elasticsearch.search.aggregations.BaseAggregationBuilder;

import java.util.ArrayList;
import java.util.List;

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
	 * 这里表示aggregationBuilder的子聚合
	 */
	protected final List<SubAggregation> subAggregations = new ArrayList<>();
	
	public SubAggregation() {
		
	}

	/**
	 * 为当前子聚合继续添加子聚合。
	 *
	 * <p>注意：这里不强制校验 subAggregation 类型是否合法（例如 pipeline agg 不能再挂子聚合），
	 * 构建时由 {@link SubAggregationSupport} 决定是否真正挂载。</p>
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
	 * 获取聚合名称
	 * @return 聚合名称
	 */
	public abstract String getName();
	
	/**
	 * 真正开始构建Elasticsearch的AggregationBuilder, 子聚合关系都建立起来了
	 * @return AggregationBuilder
	 */
	public abstract BaseAggregationBuilder build();
	
	/**
	 * 在子聚合中嵌套子聚合时, API调用可以通过and()返回上一次的子聚合, <br/>
	 * 然后上一级的子聚合可以继续添加子聚合
	 * @return SubAggregation
	 */
	public abstract SubAggregation and();
}
