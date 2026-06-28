package com.awesomecopilot.search8x.builder.agg.v8;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import com.awesomecopilot.search8x.builder.agg.AbstractAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregation;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
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
	 * 设置排序方式
	 *
	 * @param sort 排序字符串，如 "key:asc"、"count:desc"
	 * @return V8TermsAggregationBuilder
	 */
	public V8TermsAggregationBuilder sort(String sort) {
		// TODO: ES 8.x Terms Aggregation 支持排序，待实现
		// 目前暂时忽略排序参数
		return this;
	}
	
	/**
	 * 设置嵌套路径（用于 nested 字段聚合）
	 *
	 * @param path 嵌套路径
	 * @param field 字段名
	 * @return V8TermsAggregationBuilder
	 */
	public V8TermsAggregationBuilder nestedPath(String path, String field) {
		// TODO: ES 8.x Nested Aggregation 需要特殊处理，待实现
		this.field = field;
		return this;
	}

	/**
	 * 设置查询条件
	 *
	 * @param queryBuilder 查询构建器
	 * @return V8TermsAggregationBuilder
	 */
	public V8TermsAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}

	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits 是否获取总命中数
	 * @return V8TermsAggregationBuilder
	 */
	public V8TermsAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		super.fetchTotalHits(fetchTotalHits);
		return this;
	}

	/**
	 * 添加子聚合
	 * <p>
	 * 在当前 Terms 聚合的基础上添加子聚合，可以实现多层嵌套聚合<br/>
	 * 例如：在按类别聚合的基础上，再按日期进行子聚合
	 * <p>
	 * 支持的子聚合类型包括：
	 * <ul>
	 * <li/>Metric 聚合 - sum, avg, min, max, stats 等
	 * <li/>Bucket 聚合 - terms, date_histogram, histogram 等
	 * <li/>Pipeline 聚合 - bucket_sort, bucket_selector 等
	 * </ul>
	 *
	 * @param subAggregation 子聚合对象
	 * @return V8TermsAggregationBuilder
	 */
	public V8TermsAggregationBuilder subAggregation(SubAggregation subAggregation) {
		super.subAggregation(subAggregation);
		return this;
	}

	/**
	 * 构建 ES 8.x 原生 Aggregation
	 */
	private Aggregation buildV8Aggregation() {
		TermsAggregation.Builder termsBuilder = new TermsAggregation.Builder();
		termsBuilder.field(field);
		
		if (size != null) {
			termsBuilder.size(size);
		}
		if (shardSize != null) {
			termsBuilder.shardSize(shardSize);
		}

		// 添加子聚合支持（转换 ES 7.x SubAggregation 到 ES 8.x Aggregation）
		if (!subAggregations.isEmpty()) {
			Map<String, Aggregation> subAggs = com.awesomecopilot.search8x.builder.agg.sub.SubAggregationToV8Converter.convert(subAggregations);
			if (!subAggs.isEmpty()) {
				return new Aggregation.Builder()
					.terms(termsBuilder.build())
					.aggregations(subAggs)
					.build();
			}
		}

		return new Aggregation.Builder().terms(termsBuilder.build()).build();
	}

	/**
	 * 公开构建 ES 8.x 原生 Aggregation
	 * 
	 * @return co.elastic.clients.elasticsearch._types.aggregations.Aggregation
	 */
	public Aggregation build() {
		return buildV8Aggregation();
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
	
	/**
	 * 获取分页结果（需要配合 bucketSort 子聚合）
	 *
	 * @return ElasticPage
	 */
	public com.awesomecopilot.search8x.vo.ElasticPage getPage() {
		// TODO: ES 8.x Terms Aggregation 的分页支持待实现
		// 目前暂时返回空页面
		return com.awesomecopilot.search8x.vo.ElasticPage.builder()
			.results(new java.util.ArrayList<>())
			.sort(new Object[0])
			.build();
	}
}
