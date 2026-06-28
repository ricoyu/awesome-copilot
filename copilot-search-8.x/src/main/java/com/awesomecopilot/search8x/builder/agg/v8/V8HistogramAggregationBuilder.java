package com.awesomecopilot.search8x.builder.agg.v8;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramAggregation;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.builder.agg.AbstractAggregationBuilder;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Elasticsearch 8.x 原生 Histogram Aggregation Builder
 * <p>
 * 使用 co.elastic.clients API 实现直方图聚合，支持 interval、minDocCount、extendedBounds 等参数
 * 
 * <p>
 * Copyright: (C), 2026-06-20
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class V8HistogramAggregationBuilder extends AbstractAggregationBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(V8HistogramAggregationBuilder.class);
	
	private double interval;
	
	/**
	 * min_doc_count: 0 <br/>
	 * 就是说如果在interval指定的隔间内, 某个区间没有值, 在返回的结果中, 这个区间要不要包含? <br/>
	 * 强制返回所有 buckets，即使 buckets 可能为空
	 *
	 * <pre> {@code
	 * {
	 *   "key": 100.0,
	 *   "doc_count": 0
	 * }
	 * }</pre>
	 * <p>
	 * 上面的结果只有在min_doc_count: 0时才会返回, 如果我们要返回区间内有值的, 可以设置 min_doc_count: 1
	 */
	private Integer minDocCount;
	
	/**
	 * extended_bounds.min
	 * <p>
	 * min_doc_count 参数强制返回空 buckets，但是 Elasticsearch 默认只返回你的数据中最小值和最大值之间的 buckets。
	 * <p>
	 * 假设你的数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）。因此为了得到全年数据，
	 * 我们需要告诉 Elasticsearch 我们想要全部 buckets， 即便那些 buckets 可能落在最小日期 之前 或 最大日期 之后 。
	 */
	private Long minBound;
	
	/**
	 * extended_bounds.max
	 * <p>
	 * min_doc_count 参数强制返回空 buckets，但是 Elasticsearch 默认只返回你的数据中最小值和最大值之间的 buckets。
	 * <p>
	 * 假设你的数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）。因此为了得到全年数据，
	 * 我们需要告诉 Elasticsearch 我们想要全部 buckets， 即便那些 buckets 可能落在最小日期 之前 或 最大日期 之后 。
	 */
	private Long maxBound;
	
	private V8HistogramAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static V8HistogramAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new V8HistogramAggregationBuilder(indices);
	}
	
	/**
	 * 设置聚合名称和字段
	 *
	 * @param name  聚合名称
	 * @param field 要聚合的字段
	 */
	public V8HistogramAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}
	
	/**
	 * 设置查询条件
	 *
	 * @param queryBuilder 查询构建器
	 */
	public V8HistogramAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 设置桶的间隔
	 *
	 * @param interval 间隔值
	 */
	public V8HistogramAggregationBuilder interval(double interval) {
		this.interval = interval;
		return this;
	}
	
	/**
	 * min_doc_count: 0 <br/>
	 * 就是说如果在interval指定的隔间内, 某个区间没有值, 在返回的结果中, 这个区间要不要包含?
	 *
	 * <pre> {@code
	 * {
	 *   "key": 100.0,
	 *   "doc_count": 0
	 * }
	 * }</pre>
	 * <p>
	 * 上面的结果只有在min_doc_count: 0时才会返回, 如果我们要返回区间内有值的, 可以设置 min_doc_count: 1
	 *
	 * @param minDocCount 最小文档数
	 */
	public V8HistogramAggregationBuilder minDocCount(Integer minDocCount) {
		this.minDocCount = minDocCount;
		return this;
	}
	
	/**
	 * min_doc_count 参数强制返回空 buckets，但是 Elasticsearch 默认只返回你的数据中最小值和最大值之间的 buckets。
	 * <p>
	 * 因此如果你的数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）。因此为了得到全年数据，
	 * 我们需要告诉 Elasticsearch 我们想要全部 buckets， 即便那些 buckets 可能落在最小日期 之前 或 最大日期 之后 。
	 * 
	 * <pre> {@code
	 * "extended_bounds" : {
	 *     "min" : "2014-01-01",
	 *     "max" : "2014-12-31"
	 * }
	 * }</pre>
	 *
	 * @param minBound 最小边界
	 * @param maxBound 最大边界
	 */
	public V8HistogramAggregationBuilder extendedBounds(Long minBound, Long maxBound) {
		Objects.requireNonNull(minBound, "minBound cannot be null!");
		Objects.requireNonNull(maxBound, "maxBound cannot be null!");
		this.minBound = minBound;
		this.maxBound = maxBound;
		return this;
	}
	
	/**
	 * min_doc_count 参数强制返回空 buckets，但是 Elasticsearch 默认只返回你的数据中最小值和最大值之间的 buckets。
	 * <p>
	 * 因此如果你的数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）。因此为了得到全年数据，
	 * 我们需要告诉 Elasticsearch 我们想要全部 buckets， 即便那些 buckets 可能落在最小日期 之前 或 最大日期 之后 。
	 * 
	 * <pre> {@code
	 * "extended_bounds" : {
	 *     "min" : "2014-01-01",
	 *     "max" : "2014-12-31"
	 * }
	 * }</pre>
	 *
	 * @param minBound 最小边界
	 * @param maxBound 最大边界
	 */
	public V8HistogramAggregationBuilder extendedBounds(Integer minBound, Integer maxBound) {
		Objects.requireNonNull(minBound, "minBound cannot be null!");
		Objects.requireNonNull(maxBound, "maxBound cannot be null!");
		this.minBound = minBound.longValue();
		this.maxBound = maxBound.longValue();
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits 是否获取总命中数
	 */
	public V8HistogramAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
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
	public V8HistogramAggregationBuilder subAggregation(com.awesomecopilot.search8x.builder.agg.sub.SubAggregation subAggregation) {
		super.subAggregation(subAggregation);
		return this;
	}
	
	/**
	 * 构建 ES 8.x 原生 Histogram Aggregation
	 */
	private Aggregation buildV8Aggregation() {
		HistogramAggregation.Builder histogramBuilder = new HistogramAggregation.Builder();
		histogramBuilder.field(field);
		histogramBuilder.interval(interval);
		
		if (minDocCount != null) {
			histogramBuilder.minDocCount(minDocCount);
		}
		
		if (minBound != null && maxBound != null) {
			histogramBuilder.extendedBounds(bounds -> bounds
				.min(minBound.doubleValue())
				.max(maxBound.doubleValue())
			);
		}
		
		// 添加子聚合支持（转换 ES 7.x SubAggregation 到 ES 8.x Aggregation）
		if (!subAggregations.isEmpty()) {
			Map<String, Aggregation> subAggs = com.awesomecopilot.search8x.builder.agg.sub.SubAggregationToV8Converter.convert(subAggregations);
			if (!subAggs.isEmpty()) {
				return new Aggregation.Builder()
					.histogram(histogramBuilder.build())
					.aggregations(subAggs)
					.build();
			}
		}
		
		return new Aggregation.Builder().histogram(histogramBuilder.build()).build();
	}
	
	/**
	 * 执行聚合并返回结果
	 *
	 * @return Map<String, Object> 聚合结果，key 为桶的 key（数值），value 为文档数量
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
		return (Map<String, T>) V8AggResultSupport.histogramResult(searchResponse.aggregations(), name);
	}
}
