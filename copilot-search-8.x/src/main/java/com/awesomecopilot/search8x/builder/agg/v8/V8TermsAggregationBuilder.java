package com.awesomecopilot.search8x.builder.agg.v8;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import com.awesomecopilot.search8x.builder.agg.AbstractAggregationBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ES 8.x 原生 Terms 聚合 Builder 示例
 * 
 * 这个类展示了如何使用 ES 8.x 的 co.elastic.clients API 进行聚合
 * 
 * 使用示例:
 * <pre>
 * List<Map<String, Object>> result = ElasticUtils.AggsV8.terms("bank")
 *     .of("age_agg", "age")
 *     .size(20)
 *     .get();
 * </pre>
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8TermsAggregationBuilder extends AbstractAggregationBuilder {

	private static final Logger log = LoggerFactory.getLogger(V8TermsAggregationBuilder.class);

	private Integer size;
	private Integer shardSize;

	private V8TermsAggregationBuilder(String[] indices) {
		this.indices = indices;
	}

	public static V8TermsAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new V8TermsAggregationBuilder(indices);
	}

	/**
	 * 设置聚合名称和字段
	 */
	public V8TermsAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}

	/**
	 * 设置返回桶的数量
	 */
	public V8TermsAggregationBuilder size(Integer size) {
		this.size = size;
		return this;
	}

	/**
	 * 设置 shard_size 提高精确度
	 */
	public V8TermsAggregationBuilder shardSize(Integer shardSize) {
		this.shardSize = shardSize;
		return this;
	}

	/**
	 * 构建 ES 8.x 原生 Aggregation
	 */
	private Aggregation buildV8Aggregation() {
		TermsAggregation.Builder builder = new TermsAggregation.Builder();
		builder.field(field);
		
		if (size != null) {
			builder.size(size);
		}
		if (shardSize != null) {
			builder.shardSize(shardSize);
		}

		return new Aggregation.Builder().terms(builder.build()).build();
	}

	/**
	 * 执行聚合并返回结果
	 */
	public <T> List<Map<String, T>> get() {
		// 构建 ES 8.x 聚合
		Map<String, Aggregation> aggregations = new HashMap<>();
		aggregations.put(name, buildV8Aggregation());

		// 使用 ES 8.x 客户端执行查询
		co.elastic.clients.elasticsearch.core.SearchResponse searchResponse = searchWithV8Client(aggregations);
		addTotalHitsToThreadLocal(searchResponse);
		
		// 使用 ES 8.x 原生解析器解析结果
		return V8AggResultSupport.termsResult(searchResponse.aggregations());
	}
}
