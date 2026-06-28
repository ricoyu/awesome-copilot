package com.awesomecopilot.search8x.builder.agg.v8;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import com.awesomecopilot.common.lang.concurrent.Concurrent;
import com.awesomecopilot.common.lang.concurrent.FutureResult;
import com.awesomecopilot.search8x.builder.agg.AbstractAggregationBuilder;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import com.awesomecopilot.search8x.vo.ElasticPage;
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

	/**
	 * 聚合分页的时候计算分页信息
	 */
	private com.awesomecopilot.common.lang.vo.Page page;

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
	 * 设置查询条件（重写父类方法，返回正确的子类类型以支持链式调用）
	 *
	 * @param queryBuilder 查询构建器
	 * @return 当前聚合构建器实例
	 */
	@Override
	public V8MultiTermsAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
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
	 * 设置排序方式
	 *
	 * @param sort 排序字符串，如 "key:asc"、"count:desc"
	 * @return V8MultiTermsAggregationBuilder
	 */
	public V8MultiTermsAggregationBuilder sort(String sort) {
		// TODO: ES 8.x MultiTerms Aggregation 支持排序，待实现
		// 目前暂时忽略排序参数
		return this;
	}

	/**
	 * 设置是否获取总命中数（重写父类方法，返回正确的子类类型以支持链式调用）
	 *
	 * @param fetchTotalHits 是否获取总命中数
	 * @return 当前聚合构建器实例
	 */
	@Override
	public V8MultiTermsAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		super.fetchTotalHits(fetchTotalHits);
		return this;
	}

	/**
	 * 添加子聚合（重写父类方法，返回正确的子类类型以支持链式调用）
	 *
	 * @param subAggregation 子聚合
	 * @return 当前聚合构建器实例
	 */
	@Override
	public V8MultiTermsAggregationBuilder subAggregation(com.awesomecopilot.search8x.builder.agg.sub.SubAggregation subAggregation) {
		// 如果是 bucketSort 子聚合，提取分页信息
		if (subAggregation instanceof com.awesomecopilot.search8x.builder.agg.sub.ElasticBucketSortSubAggregation) {
			this.page = ((com.awesomecopilot.search8x.builder.agg.sub.ElasticBucketSortSubAggregation) subAggregation).toPage();
		}
		super.subAggregation(subAggregation);
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

	/**
	 * 获取分页结果
	 *
	 * @return ElasticPage
	 */
	public ElasticPage getPage() {
		// 异步获取总桶数（不带子聚合，size设为最大值）
		FutureResult<Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate>> totalBucketsResultFuture = Concurrent.submit(() -> {
			Map<String, Aggregation> aggregations = new HashMap<>();
			// 构建不带子聚合的聚合
			Aggregation aggWithoutSub = buildV8AggregationWithoutSubs();
			aggregations.put(name, aggWithoutSub);
			
			co.elastic.clients.elasticsearch.core.SearchResponse searchResponse = searchWithV8Client(aggregations);
			return searchResponse.aggregations();
		});

		// 异步获取分页结果（带子聚合）
		FutureResult<Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate>> pagingResultFuture = Concurrent.submit(() -> {
			Map<String, Aggregation> aggregations = new HashMap<>();
			aggregations.put(name, buildV8Aggregation());
			
			co.elastic.clients.elasticsearch.core.SearchResponse searchResponse = searchWithV8Client(aggregations);
			return searchResponse.aggregations();
		});

		Concurrent.await();

		// 获取总桶数
		Integer totalBucketsCount = V8AggResultSupport.termsTotalBuckets(totalBucketsResultFuture.get());

		// 获取分页结果
		List<Map<String, Object>> results = V8AggResultSupport.termsResult(pagingResultFuture.get());

		ElasticPage elasticPage = ElasticPage.<Map<String, Object>>builder()
				.results(results)
				.build();

		// 设置分页信息
		if (page != null) {
			elasticPage.setPageSize(page.getPageSize());
			elasticPage.setPageNum(page.getPageNum());
		}
		elasticPage.setTotalCount(totalBucketsCount);

		return elasticPage;
	}

	/**
	 * 构建不带子聚合的 ES 8.x 原生 Multi Terms Aggregation（用于计算总桶数）
	 */
	private Aggregation buildV8AggregationWithoutSubs() {
		if (fields == null || fields.length == 0) {
			throw new IllegalStateException("Fields must be set using of() method before calling getPage()");
		}

		// 构建 MultiTermLookup 列表
		List<MultiTermLookup> termLookups = Arrays.stream(fields)
				.map(field -> MultiTermLookup.of(mt -> mt.field(field)))
				.collect(Collectors.toList());

		// 构建 MultiTermsAggregation，size 设为最大值以获取所有桶
		MultiTermsAggregation multiTermsAgg = MultiTermsAggregation.of(mt -> mt
				.terms(termLookups)
				.size(Integer.MAX_VALUE)
				.shardSize(shardSize)
		);

		return new Aggregation.Builder().multiTerms(multiTermsAgg).build();
	}
}
