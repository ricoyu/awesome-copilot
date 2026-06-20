package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.search8x.exception.ElasticQueryException;

import java.io.IOException;
import java.util.Map;

/**
 * 聚合请求执行桥接：使用 elasticsearch-java 8.x 客户端执行聚合查询，
 * 并返回 ES 8.x 原生 SearchResponse。
 */
public final class AggregationBridge {

	private AggregationBridge() {
	}

	/**
	 * 使用 ES 8.x ElasticsearchClient 执行聚合查询，返回 ES 8.x SearchResponse
	 *
	 * @param client         ES 8.x 客户端
	 * @param indices        索引名称数组
	 * @param aggregations   聚合构建器 Map (name -> Aggregation)
	 * @param queryBuilder   可选的查询条件 JSON
	 * @return ES 8.x SearchResponse
	 */
	public static SearchResponse search(ElasticsearchClient client, String[] indices,
			Map<String, Aggregation> aggregations, String queryBuilder) {
		try {
			// 构建搜索请求
			SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
			
			// 设置索引
			for (String index : indices) {
				requestBuilder.index(index);
			}
			
			// 设置 ignore_unavailable 和 allow_no_indices
			requestBuilder.ignoreUnavailable(true);
			requestBuilder.allowNoIndices(true);
			
			// 设置 size=0（聚合查询不需要返回文档）
			requestBuilder.size(0);
			
			// 添加查询条件（如果有）
			if (queryBuilder != null && !queryBuilder.isEmpty()) {
				// TODO: 将 JSON 转换为 Query 对象
				// 暂时跳过，后续实现
			}
			
			// 添加聚合
			if (aggregations != null && !aggregations.isEmpty()) {
				requestBuilder.aggregations(aggregations);
			}
			
			// 执行查询
			return client.search(requestBuilder.build(), Void.class);
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}


}
