package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregation;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.support.AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Filter聚合, Bucket聚合的一种
 * <p>
 * 定义一个满足过滤条件的文档桶, 可以对过滤后的文档做子聚合分析
 * <p>
 * 等价 DSL:
 * <pre>
 * "aggs": {
 *   "paid_order": {
 *     "filter": {
 *       "term": {"pay_status": 1}
 *     },
 *     "aggs": {
 *       "group_by_brand": {
 *         "terms": {"field": "brand"}
 *       }
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * Copyright: (C), 2021-05-10 16:55
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticFilterAggregationBuilder extends AbstractAggregationBuilder implements ElasticAggregationBuilder, SubAggregatable {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticFilterAggregationBuilder.class);
	
	/**
	 * filter聚合使用的查询条件
	 */
	private BaseQueryBuilder filterQuery;
	
	private ElasticFilterAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static ElasticFilterAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new ElasticFilterAggregationBuilder(indices);
	}
	
	/**
	 * 设置聚合名称和过滤查询条件
	 * <p>
	 * filter聚合不需要field参数, 这里的field参数被忽略, 仅为了保持API一致性
	 *
	 * @param name  聚合名称
	 * @param field 忽略, filter聚合不使用此参数
	 * @return ElasticFilterAggregationBuilder
	 */
	@Override
	public ElasticFilterAggregationBuilder of(String name, String field) {
		this.name = name;
		return this;
	}
	
	/**
	 * 设置聚合名称
	 *
	 * @param name 聚合名称
	 * @return ElasticFilterAggregationBuilder
	 */
	public ElasticFilterAggregationBuilder of(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * 设置过滤查询条件
	 * <p>
	 * 支持任意查询构建器: term, match, bool, range等
	 *
	 * @param queryBuilder 查询构建器
	 * @return ElasticFilterAggregationBuilder
	 */
	public ElasticFilterAggregationBuilder filter(BaseQueryBuilder queryBuilder) {
		this.filterQuery = queryBuilder;
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits
	 * @return ElasticFilterAggregationBuilder
	 */
	public ElasticFilterAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	@Override
	public ElasticFilterAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 添加子聚合
	 *
	 * @param subAggregation 子聚合对象
	 * @return ElasticFilterAggregationBuilder
	 */
	@Override
	public ElasticFilterAggregationBuilder subAggregation(SubAggregation subAggregation) {
		subAggregations.add(subAggregation);
		return this;
	}
	
	@Override
	public Aggregation build() {
		if (filterQuery == null) {
			throw new IllegalStateException("filter aggregation requires a filter query, call filter() first");
		}
		
		// 通过反射调用buildQuery获取Query对象
		Query query = (Query) ReflectionUtils.invokeMethod("buildQuery", filterQuery);
		if (query == null) {
			throw new IllegalStateException("Failed to build filter query");
		}
		
		Map<String, Aggregation> subAggsMap = buildSubAggregationsMap(subAggregations);
		
		return Aggregation.of(a -> {
			a.filter(query);
			if (subAggsMap != null) {
				a.aggregations(subAggsMap);
			}
			return a;
		});
	}
	
	/**
	 * 执行filter聚合并返回子聚合结果
	 * <p>
	 * filter聚合本身只返回doc_count, 真正有意义的是其子聚合结果
	 *
	 * @return Map 包含 doc_count 和所有子聚合结果
	 */
	public Map<String, Object> get() {
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
			log.error("Filter aggregation failed: {}", errorDetail, e);
			throw new RuntimeException(errorDetail, e);
		}
		addTotalHitsToThreadLocal(searchResponse);
		Map<String, Aggregate> aggregations = searchResponse.aggregations();
		
		return AggResultSupport.filterResult(aggregations, name);
	}
}
