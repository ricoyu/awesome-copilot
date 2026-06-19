package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.awesomecopilot.json.jackson.JacksonUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * <p>
 * Copyright: (C), 2020-12-25 8:58
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticMultiGetBuilder<T> {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticMultiGetBuilder.class);
	
	private ElasticsearchClient client;
	
	private List<MultiGetItem> items = new ArrayList<>();
	
	private Class<T> clazz;
	
	public ElasticMultiGetBuilder(ElasticsearchClient client) {
		this.client = client;
	}
	
	public ElasticMultiGetBuilder add(String index, String id) {
		items.add(new MultiGetItem(index, id));
		return this;
	}
	
	public ElasticMultiGetBuilder add(String index, List<String> ids) {
		ids.stream()
				.filter(Objects::nonNull)
				.map(id -> new MultiGetItem(index, id))
				.forEach(items::add);
		return this;
	}
	
	public ElasticMultiGetBuilder resultType(Class<T> clazz) {
		this.clazz = clazz;
		return this;
	}
	
	public List<T> request() {
		try {
			// 构建 mget 请求的 JSON body
			JSONObject requestBody = new JSONObject();
			JSONArray docsArray = new JSONArray();
			
			for (MultiGetItem item : items) {
				JSONObject doc = new JSONObject();
				doc.put("_index", item.index);
				doc.put("_id", item.id);
				docsArray.put(doc);
			}
			
			requestBody.put("docs", docsArray);
			
			// 执行 HTTP 请求
			co.elastic.clients.transport.rest_client.RestClientTransport transport = 
					(co.elastic.clients.transport.rest_client.RestClientTransport) client._transport();
			org.elasticsearch.client.RestClient restClient = transport.restClient();
			
			org.elasticsearch.client.Request request = new org.elasticsearch.client.Request("GET", "/_mget");
			request.setJsonEntity(requestBody.toString());
			
			org.elasticsearch.client.Response response = restClient.performRequest(request);
			String jsonResponse = org.apache.http.util.EntityUtils.toString(response.getEntity(), "UTF-8");
			
			// 解析响应
			return parseMgetResponse(jsonResponse);
		} catch (Exception e) {
			log.error("Multi-get request failed", e);
			throw new RuntimeException("Multi-get request failed", e);
		}
	}
	
	private List<T> parseMgetResponse(String jsonResponse) {
		List<String> resultJsons = new ArrayList<>();
		
		try {
			JSONObject root = new JSONObject(jsonResponse);
			if (!root.has("docs")) {
				return clazz != null ? new ArrayList<>() : (List<T>) new ArrayList<String>();
			}
			
			JSONArray docsArray = root.getJSONArray("docs");
			for (int i = 0; i < docsArray.length(); i++) {
				JSONObject doc = docsArray.getJSONObject(i);
				
				// 检查是否有错误
				if (doc.has("error")) {
					log.warn("Document fetch failed: {}", doc.optString("error"));
					continue;
				}
				
				// 检查文档是否存在
				if (doc.optBoolean("found", false) && doc.has("_source")) {
					JSONObject source = doc.getJSONObject("_source");
					resultJsons.add(source.toString());
				}
			}
		} catch (Exception e) {
			log.error("Failed to parse mget response", e);
		}
		
		// 如果指定了类型，转换为对象
		if (clazz != null) {
			return resultJsons.stream()
					.map(json -> JacksonUtils.toObject(json, clazz))
					.collect(toList());
		}
		
		return (List<T>) resultJsons;
	}
	
	/**
	 * 内部类，存储单个 mget 项
	 */
	private static class MultiGetItem {
		private final String index;
		private final String id;
		
		public MultiGetItem(String index, String id) {
			this.index = index;
			this.id = id;
		}
	}
}
