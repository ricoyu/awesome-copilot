package com.awesomecopilot.search8x.builder.agg;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.common.lang.context.ThreadContext;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregation;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.constants.ElasticConstants;
import com.awesomecopilot.search8x.support.AggregationBridge;
import com.awesomecopilot.search8x.support.SearchRequestSupport;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Copyright: (C), 2021-05-10 11:51
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class AbstractAggregationBuilder{
	
	private static final Logger log = LoggerFactory.getLogger(AbstractAggregationBuilder.class);
	
	protected String[] indices;
	
	/**
	 * 聚合名字
	 */
	protected String name;
	
	/**
	 * 要对哪个字段聚合
	 */
	protected String field;
	
	///**
	// * 要对聚合的KEY还是COUNT排序
	// */
	//protected OrderBy orderBy;
	//
	///**
	// * 升序, 降序?
	// */
	//protected Direction direction;
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 */
	protected boolean fetchTotalHits = false;
	
	protected ElasticCompositeAggregationBuilder compositeAggregationBuilder;
	
	protected BaseQueryBuilder baseQueryBuilder;
	
	/**
	 * 添加的子聚合
	 */
	protected List<SubAggregation> subAggregations = new ArrayList<>();
	
	protected void logDsl(SearchSourceBuilder builder) {
		if (log.isDebugEnabled()) {
			log.debug("Aggregation DSL:\n{}", new JSONObject(builder.toString()).toString(2));
		}
	}
	
	/**
	 * 设置查询条件
	 *
	 * @param queryBuilder 查询构建器
	 * @return 当前聚合构建器实例
	 */
	public AbstractAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		this.baseQueryBuilder = queryBuilder;
		return this;
	}
	
	/**
	 * 设置是否获取总命中数
	 *
	 * @param fetchTotalHits 是否获取总命中数
	 * @return 当前聚合构建器实例
	 */
	public AbstractAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	/**
	 * 添加子聚合
	 *
	 * @param subAggregation 子聚合
	 * @return 当前聚合构建器实例
	 */
	public AbstractAggregationBuilder subAggregation(SubAggregation subAggregation) {
		this.subAggregations.add(subAggregation);
		return this;
	}
	
	protected SearchSourceBuilder searchSourceBuilder() {
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		if (baseQueryBuilder != null) {
			QueryBuilder queryBuilder = ReflectionUtils.invokeMethod("builder", baseQueryBuilder);
			sourceBuilder.query(queryBuilder);
		}
		sourceBuilder.trackTotalHits(true);
		return sourceBuilder;
	}

	protected org.elasticsearch.action.search.SearchResponse search(AggregationBuilder... aggregations) {
		SearchSourceBuilder sourceBuilder = searchSourceBuilder();
		for (AggregationBuilder aggregation : aggregations) {
			sourceBuilder.aggregation(aggregation);
		}
		return SearchRequestSupport.search(ElasticUtils.QUERY_CLIENT, indices, sourceBuilder);
	}

	/**
	 * 执行搜索
	 */
	protected org.elasticsearch.action.search.SearchResponse executeSearch(SearchSourceBuilder sourceBuilder) {
		return SearchRequestSupport.search(ElasticUtils.QUERY_CLIENT, indices, sourceBuilder);
	}

	/**
	 * 使用 ES 8.x ElasticsearchClient 执行聚合查询
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregation)
	 * @return ES 8.x SearchResponse
	 */
	protected SearchResponse searchWithV8Client(Map<String, Aggregation> aggregations) {
		String queryJson = null;
		if (baseQueryBuilder != null) {
			// TODO: 将 BaseQueryBuilder 转换为 ES 8.x Query
			// 暂时跳过，后续实现
		}
		return AggregationBridge.search(ElasticUtils.QUERY_CLIENT, indices, aggregations, queryJson);
	}

	/**
	 * 从 ES 7.x SearchResponse 中提取 total hits
	 */
	protected void addTotalHitsToThreadLocal(org.elasticsearch.action.search.SearchResponse searchResponse) {
		if (fetchTotalHits) {
			org.elasticsearch.search.SearchHits hits = searchResponse.getHits();
			if (hits != null && hits.getTotalHits() != null) {
				ThreadContext.put(ElasticConstants.TOTAL_HITS, hits.getTotalHits().value);
			}
		} else {
			ThreadContext.remove(ElasticConstants.TOTAL_HITS);
		}
	}

	/**
	 * 从 ES 8.x SearchResponse 中提取 total hits
	 */
	protected void addTotalHitsToThreadLocal(co.elastic.clients.elasticsearch.core.SearchResponse searchResponse) {
		if (fetchTotalHits) {
			if (searchResponse.hits() != null && searchResponse.hits().total() != null) {
				ThreadContext.put(ElasticConstants.TOTAL_HITS, searchResponse.hits().total().value());
			}
		} else {
			ThreadContext.remove(ElasticConstants.TOTAL_HITS);
		}
	}
}