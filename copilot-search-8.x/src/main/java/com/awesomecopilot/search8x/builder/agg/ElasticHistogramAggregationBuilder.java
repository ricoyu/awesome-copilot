package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ExtendedBounds;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Copyright: (C), 2021-07-12 18:03
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticHistogramAggregationBuilder extends AbstractAggregationBuilder implements ElasticAggregationBuilder, Compositable {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticHistogramAggregationBuilder.class);
	
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
	
	private ElasticHistogramAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static ElasticHistogramAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticHistogramAggregationBuilder(indices);
	}
	
	/**
	 * 给聚合起个名字, 后续获取聚合数据时需要
	 *
	 * @param name
	 * @param field
	 * @return ElasticHistogramAggregationBuilder
	 */
	@Override
	public ElasticHistogramAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}
	
	public ElasticHistogramAggregationBuilder interval(double interval) {
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
	 * @param minDocCount
	 * @return
	 */
	public ElasticHistogramAggregationBuilder minDocCount(Integer minDocCount) {
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
	 * @param minBound
	 * @param maxBound
	 * @return ElasticHistogramAggregationBuilder
	 */
	public ElasticHistogramAggregationBuilder extendedBounds(Long minBound, Long maxBound) {
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
	 * @param minBound
	 * @param maxBound
	 * @return ElasticHistogramAggregationBuilder
	 */
	public ElasticHistogramAggregationBuilder extendedBounds(Integer minBound, Integer maxBound) {
		Objects.requireNonNull(minBound, "minBound cannot be null!");
		Objects.requireNonNull(maxBound, "maxBound cannot be null!");
		this.minBound = minBound.longValue();
		this.maxBound = maxBound.longValue();
		return this;
	}
	
	@Override
	public ElasticHistogramAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数 
	 * @param fetchTotalHits
	 * @return ElasticHistogramAggregationBuilder
	 */
	public ElasticHistogramAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	@Override
	public Aggregation build() {
		return Aggregation.of(a -> a.histogram(h -> {
			h.field(field).interval(interval);
			if (minDocCount != null) {
				h.minDocCount(minDocCount);
			}
			if (minBound != null && maxBound != null) {
				h.extendedBounds(ExtendedBounds.of(eb -> eb
						.min(minBound.doubleValue())
						.max(maxBound.doubleValue())));
			}
			return h;
		}));
	}
	
	@Override
	public ElasticCompositeAggregationBuilder and() {
		// 使用 this.name 作为聚合名称
		compositeAggregationBuilder.add(this.name, build());
		return compositeAggregationBuilder;
	}
	
	public <T> Map<String, T> get() {
		Aggregation aggregation = build();
		
		SearchRequest.Builder builder = searchRequestBuilder();
		builder.aggregations(name, aggregation).size(0);
		SearchRequest request = builder.build();
		logDsl(request);
		
		SearchResponse<Map> searchResponse = null;
		try {
			searchResponse = ElasticUtils.QUERY_CLIENT.search(request, Map.class);
		} catch (Exception e) {
			String errorDetail = extractEsErrorDetails(e);
			log.error("Histogram aggregation failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations = searchResponse.aggregations();
		
		return (Map<String, T>) V8AggResultSupport.histogramResult(aggregations, name);
	}
}
