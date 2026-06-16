package com.awesomecopilot.search8x.builder.bulk;

import com.awesomecopilot.search8x.cache.ElasticCacheUtils;
import com.awesomecopilot.search8x.support.BulkResult;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.search8x.ElasticUtils.QUERY_CLIENT;

/**
 * <p>
 * Copyright: (C), 2021-09-10 16:00
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticBulkIndexBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticBulkIndexBuilder.class);
	
	private String index;
	
	private List<?> docs;
	
	/**
	 * 是否要立即刷新
	 */
	private Boolean refresh;
	
	public ElasticBulkIndexBuilder(String index) {
		this.index = index;
	}
	
	/**
	 * 要批量插入的文档
	 * @param docs
	 * @return ElasticBulkIndexBuilder
	 */
	public ElasticBulkIndexBuilder docs(String... docs) {
		notNull(docs, "docs cannot be null!");
		if (docs.length == 0) {
			throw new IllegalArgumentException("docs cannot be empty!");
		}
		this.docs = Arrays.asList(docs);
		return this;
	}
	
	/**
	 * 要批量插入的文档
	 * @param docs
	 * @return ElasticBulkIndexBuilder
	 */
	public ElasticBulkIndexBuilder docs(List docs) {
		notNull(docs, "docs cannot be null!");
		if (docs.isEmpty()) {
			throw new IllegalArgumentException("docs cannot be empty!");
		}
		this.docs = docs;
		return this;
	}
	
	/**
	 * 批量插入后是否强制刷新, 这样可以立即查询到新插入的文档
	 * @param refresh
	 * @return ElasticBulkIndexBuilder
	 */
	public ElasticBulkIndexBuilder refresh(Boolean refresh) {
		this.refresh = refresh;
		return this;
	}
	
	public BulkResult execute() {
		try {
			// 构建 NDJSON
			StringBuilder ndjson = new StringBuilder();
			for (Object doc : docs) {
				if (doc == null) continue;
				
				IndexRequest indexRequest;
				if (doc instanceof String) {
					indexRequest = new IndexRequest(index).source((String) doc, XContentType.JSON);
				} else {
					String id = ElasticCacheUtils.getIdValue(doc);
					indexRequest = new IndexRequest(index).source(toJson(doc), XContentType.JSON);
					if (id != null) {
						indexRequest.id(id);
					}
				}
				
				// 构建 action metadata
				JSONObject actionMeta = new JSONObject();
				JSONObject indexObj = new JSONObject();
				indexObj.put("_index", indexRequest.index());
				if (indexRequest.id() != null) {
					indexObj.put("_id", indexRequest.id());
				}
				actionMeta.put("index", indexObj);
				ndjson.append(actionMeta.toString()).append("\n");
				
				// 添加文档 source
				String source = indexRequest.source().utf8ToString();
				ndjson.append(source).append("\n");
			}
			
			// 执行 HTTP 请求
			co.elastic.clients.transport.rest_client.RestClientTransport transport = 
					(co.elastic.clients.transport.rest_client.RestClientTransport) QUERY_CLIENT._transport();
			org.elasticsearch.client.RestClient restClient = transport.restClient();
			
			org.elasticsearch.client.Request request = new org.elasticsearch.client.Request("POST", "/_bulk");
			if (refresh != null && refresh) {
				request.addParameter("refresh", "true");
			}
			request.setJsonEntity(ndjson.toString());
			
			org.elasticsearch.client.Response response = restClient.performRequest(request);
			String jsonResponse = org.apache.http.util.EntityUtils.toString(response.getEntity(), "UTF-8");
			
			// 解析响应
			return parseBulkResponse(jsonResponse);
		} catch (Exception e) {
			log.error("Bulk index failed", e);
			throw new RuntimeException("Bulk index failed", e);
		}
	}
	
	private BulkResult parseBulkResponse(String jsonResponse) {
		BulkResult bulkResult = new BulkResult();
		try {
			JSONObject root = new JSONObject(jsonResponse);
			if (!root.has("items")) {
				return bulkResult;
			}
			
			JSONArray itemsArray = root.getJSONArray("items");
			for (int i = 0; i < itemsArray.length(); i++) {
				JSONObject itemObj = itemsArray.getJSONObject(i);
				String operation = itemObj.keySet().iterator().next();
				JSONObject operationObj = itemObj.getJSONObject(operation);
				
				String id = operationObj.optString("_id");
				
				if (operationObj.has("error")) {
					// 失败
					bulkResult.fail();
					JSONObject errorObj = operationObj.getJSONObject("error");
					String failureMessage = errorObj.optString("reason", "Unknown error");
					bulkResult.addFailMessage(failureMessage);
				} else {
					// 成功
					bulkResult.success();
					if (id != null && !id.isEmpty()) {
						bulkResult.addId(id);
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to parse bulk response", e);
		}
		return bulkResult;
	}
}
