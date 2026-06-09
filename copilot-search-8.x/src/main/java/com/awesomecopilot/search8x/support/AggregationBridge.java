package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import com.awesomecopilot.search8x.exception.ElasticQueryException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

/**
 * 聚合请求执行桥接：使用 elasticsearch-java 8.x 客户端执行聚合查询，
 * 并将响应转换为 7.x SearchResponse 供现有 AggResultSupport 继续使用。
 */
public final class AggregationBridge {

	private AggregationBridge() {
	}

	/**
	 * 使用 ES 8.x ElasticsearchClient 执行聚合查询，返回 7.x SearchResponse
	 *
	 * @param client         ES 8.x 客户端
	 * @param indices        索引名称数组
	 * @param aggregations   聚合构建器 Map (name -> Aggregation)
	 * @param queryBuilder   可选的查询条件 JSON
	 * @return 7.x SearchResponse
	 */
	public static org.elasticsearch.action.search.SearchResponse search(ElasticsearchClient client, String[] indices,
			Map<String, Aggregation> aggregations, String queryBuilder) {
		try {
			// 构建聚合请求的 JSON
			JSONObject aggJson = new JSONObject();
			aggregations.forEach((name, agg) -> {
				// 将 ES 8.x Aggregation 序列化为 JSON
				String aggJsonStr = serializeAggregation(agg);
				aggJson.put(name, new JSONObject(aggJsonStr));
			});

			// 构建完整的搜索请求体
			JSONObject requestBody = new JSONObject();
			if (queryBuilder != null && !queryBuilder.isEmpty()) {
				requestBody.put("query", new JSONObject(queryBuilder));
			}
			requestBody.put("aggs", aggJson);
			requestBody.put("size", 0);

			// 通过底层 RestClient 执行请求
			String indexPath = String.join(",", indices);
			org.elasticsearch.client.Request request = new org.elasticsearch.client.Request(
					"POST", "/" + indexPath + "/_search");
			request.addParameter("ignore_unavailable", "true");
			request.addParameter("allow_no_indices", "true");
			request.setJsonEntity(requestBody.toString());

			org.elasticsearch.client.Response response = SearchResponseBridge.restClient(client).performRequest(request);
			
			// 解析响应为 7.x SearchResponse
			return SearchResponseBridge.parseSearchResponse(response);
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}

	/**
	 * 序列化 ES 8.x Aggregation 为 JSON 字符串
	 */
	private static String serializeAggregation(Aggregation aggregation) {
		try {
			// 使用 Jackson 序列化
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			return mapper.writeValueAsString(aggregation);
		} catch (Exception e) {
			throw new ElasticQueryException("Failed to serialize aggregation", e);
		}
	}
}
