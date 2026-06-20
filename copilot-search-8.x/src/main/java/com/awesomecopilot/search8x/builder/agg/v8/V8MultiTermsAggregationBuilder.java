package com.awesomecopilot.search8x.builder.agg.v8;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import com.awesomecopilot.search8x.builder.agg.AbstractAggregationBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ES 8.x 原生 Multi Terms 聚合 Builder
 * 
 * Multi Terms 聚合基于多个字段的组合来计算分桶，是 Bucket Aggregation 的一种
 * 
 * 使用示例:
 * <pre>
 * // 单字段
 * List<Map<String, Object>> result = ElasticUtils.AggsV8.multiTerms("employees")
 *     .of("job_agg", "job")
 *     .size(20)
 *     .get();
 * 
 * // 多字段
 * List<Map<String, Object>> result = ElasticUtils.AggsV8.multiTerms("products")
 *     .of("category_price_agg", "category", "price")
 *     .size(50)
 *     .get();
 * </pre>
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8MultiTermsAggregationBuilder extends AbstractAggregationBuilder {

	private static final Logger log = LoggerFactory.getLogger(V8MultiTermsAggregationBuilder.class);

	private Integer size;
	private Integer shardSize;
	
	/**
	 * 要对哪些字段聚合
	 */
	private String[] fields;

	private V8MultiTermsAggregationBuilder(String[] indices) {
		this.indices = indices;
	}

	public static V8MultiTermsAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new V8MultiTermsAggregationBuilder(indices);
	}

	/**
	 * 设置聚合名称和单个字段
	 */
	public V8MultiTermsAggregationBuilder of(String name, String field) {
		this.name = name;
		this.fields = new String[]{field};
		return this;
	}

	/**
	 * 设置聚合名称和多个字段
	 */
	public V8MultiTermsAggregationBuilder of(String name, String... fields) {
		this.name = name;
		this.fields = fields;
		return this;
	}

	/**
	 * 设置聚合名称和字段列表
	 */
	public V8MultiTermsAggregationBuilder of(String name, List<String> fields) {
		Objects.requireNonNull(fields, "fields cannot be null!");
		this.name = name;
		this.fields = fields.toArray(new String[0]);
		return this;
	}

	/**
	 * 设置返回桶的数量
	 */
	public V8MultiTermsAggregationBuilder size(Integer size) {
		this.size = size;
		return this;
	}

	/**
	 * 设置 shard_size 提高精确度
	 * 帮助解决 Terms 不准的问题：数据分散在多个不同的分片上，Coordinating Node 无法获取数据全貌
	 * 原理：每次从 Shard 上额外多获取数据，提升准确率
	 */
	public V8MultiTermsAggregationBuilder shardSize(Integer shardSize) {
		this.shardSize = shardSize;
		return this;
	}

	/**
	 * 构建 ES 8.x 原生 Multi Terms Aggregation
	 */
	private Aggregation buildV8Aggregation() {
		if (fields == null || fields.length == 0) {
			throw new IllegalStateException("Fields must be set using of() method before calling get()");
		}

		// 构建 MultiTermLookup 列表
		List<MultiTermLookup> termLookups = Arrays.stream(fields)
				.map(field -> MultiTermLookup.of(mt -> mt.field(field)))
				.collect(Collectors.toList());
		
		// 构建 MultiTermsAggregation
		MultiTermsAggregation multiTermsAgg = MultiTermsAggregation.of(mt -> mt
				.terms(termLookups)
				.size(size != null ? size : 10)
				.shardSize(shardSize)
		);

		return new Aggregation.Builder().multiTerms(multiTermsAgg).build();
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
