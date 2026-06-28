package com.awesomecopilot.search8x.builder.agg.v8;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.builder.agg.AbstractAggregationBuilder;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Elasticsearch 8.x 原生 Range Aggregation Builder
 * <p>
 * 使用 co.elastic.clients API 实现范围聚合，支持查询条件、子聚合等完整功能
 * 
 * <p>
 * Copyright: (C), 2026-06-20
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class V8RangeAggregationBuilder extends AbstractAggregationBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(V8RangeAggregationBuilder.class);
	
	/**
	 * 范围定义内部类
	 */
	public static class Range {
		private String key;
		private Double from;
		private Double to;
		
		public Range(String key, Double from, Double to) {
			this.key = key;
			this.from = from;
			this.to = to;
		}
		
		public String getKey() {
			return key;
		}
		
		public Double getFrom() {
			return from;
		}
		
		public Double getTo() {
			return to;
		}
	}
	
	private List<Range> ranges = new ArrayList<>();
	
	/**
	 * 添加一个范围
	 *
	 * @param key  范围的键名（可选）
	 * @param from 下界（包含），null 表示无下界
	 * @param to   上界（不包含），null 表示无上界
	 */
	public V8RangeAggregationBuilder addRange(String key, Double from, Double to) {
		ranges.add(new Range(key, from, to));
		return this;
	}
	
	/**
	 * 添加一个范围（不指定 key）
	 */
	public V8RangeAggregationBuilder addRange(int from, int to) {
		return addRange(null, Double.valueOf(from), Double.valueOf(to));
	}
	
	/**
	 * 添加一个范围（不指定 key）
	 */
	public V8RangeAggregationBuilder addRange(Double from, Double to) {
		return addRange(null, from, to);
	}
	
	/**
	 * 添加一个只有上界的范围（从负无穷到 to）
	 *
	 * @param key 范围的键名（可选）
	 * @param to  上界
	 */
	public V8RangeAggregationBuilder addUnboundedTo(String key, Double to) {
		return addRange(key, null, to);
	}
	
	/**
	 * 添加一个只有上界的范围（不指定 key）
	 */
	public V8RangeAggregationBuilder addUnboundedTo(int to) {
		return addUnboundedTo(null, Double.valueOf(to));
	}
	
	/**
	 * 添加一个只有上界的范围（不指定 key）
	 */
	public V8RangeAggregationBuilder addUnboundedTo(Double to) {
		return addUnboundedTo(null, to);
	}
	
	/**
	 * 添加一个只有下界的范围（从 from 到正无穷）
	 *
	 * @param key  范围的键名（可选）
	 * @param from 下界
	 */
	public V8RangeAggregationBuilder addUnboundedFrom(String key, int from) {
		return addRange(key, Double.valueOf(from), null);
	}
	
	/**
	 * 添加一个只有下界的范围（从 from 到正无穷）
	 *
	 * @param key  范围的键名（可选）
	 * @param from 下界
	 */
	public V8RangeAggregationBuilder addUnboundedFrom(String key, Double from) {
		return addRange(key, from, null);
	}
	
	/**
	 * 添加一个只有下界的范围（不指定 key）
	 */
	public V8RangeAggregationBuilder addUnboundedFrom(int from) {
		return addUnboundedFrom(null, Double.valueOf( from));
	}
	
	/**
	 * 添加一个只有下界的范围（不指定 key）
	 */
	public V8RangeAggregationBuilder addUnboundedFrom(Double from) {
		return addUnboundedFrom(null, from);
	}
	
	private V8RangeAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static V8RangeAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new V8RangeAggregationBuilder(indices);
	}
	
	/**
	 * 设置聚合名称和字段
	 *
	 * @param name  聚合名称
	 * @param field 要聚合的字段
	 */
	public V8RangeAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}
	
	/**
	 * 设置查询条件
	 *
	 * @param queryBuilder 查询构建器
	 */
	public V8RangeAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits 是否获取总命中数
	 */
	public V8RangeAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	/**
	 * 添加子聚合
	 *
	 * @param subAggregation 子聚合
	 * @return 当前聚合构建器实例
	 */
	@Override
	public V8RangeAggregationBuilder subAggregation(com.awesomecopilot.search8x.builder.agg.sub.SubAggregation subAggregation) {
		super.subAggregation(subAggregation);
		return this;
	}
	

	
	/**
	 * 构建 ES 8.x 原生 Range Aggregation
	 */
	private Aggregation buildV8Aggregation() {
		// 使用函数式 API 构建 Range Aggregation
		return Aggregation.of(agg -> agg
			.range(r -> {
				r.field(field);
				
				// 添加所有范围
				for (Range range : ranges) {
					r.ranges(rangeBuilder -> {
						if (isNotBlank(range.getKey())) {
							rangeBuilder.key(range.getKey());
						}
						if (range.getFrom() != null) {
							rangeBuilder.from(range.getFrom());
						}
						if (range.getTo() != null) {
							rangeBuilder.to(range.getTo());
						}
						return rangeBuilder;
					});
				}
				
				// TODO: 添加子聚合支持
				// if (!subAggregations.isEmpty()) {
				//     r.aggs(...);
				// }
				
				return r;
			})
		);
	}
	
	/**
	 * 执行聚合并返回结果
	 *
	 * @return Map<String, Object> 聚合结果，key 为范围标识，value 为文档数量
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> get() {
		// 构建 ES 8.x 聚合
		Map<String, Aggregation> aggregations = new HashMap<>();
		aggregations.put(name, buildV8Aggregation());
		
		// 使用 ES 8.x 客户端执行查询
		SearchResponse searchResponse = searchWithV8Client(aggregations);
		addTotalHitsToThreadLocal(searchResponse);
		
		// 使用 ES 8.x 原生解析器解析结果
		return (Map<String, T>) V8AggResultSupport.rangeResult(searchResponse.aggregations(), name);
	}
}
