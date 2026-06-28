package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.exception.ElasticQueryException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;

/**
 * <p>
 * Copyright: (C), 2021-03-11 13:53
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticReindexBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticReindexBuilder.class);
	
	private QueryBuilder filter;
	
	private String srcIndex;
	
	private String destIndex;
	
	/**
	 * 可以把Reindex拆分成几个子任务并发执行
	 * Reindex supports Sliced scroll to parallelize the reindexing process. 
	 * This parallelization can improve efficiency and provide a convenient way to break the request down into smaller parts.
	 */
	private int slices;
	
	/**
	 * 一次操作多少文档
	 */
	private int size;
	
	public ElasticReindexBuilder(String srcIndex, String destIndex) {
		this.srcIndex = srcIndex;
		this.destIndex = destIndex;
	}
	
	/**
	 * 过滤出一部分文档进行Reindex
	 * @param filter
	 * @return ElasticReindexBuilder
	 */
	public ElasticReindexBuilder filter(QueryBuilder filter) {
		this.filter = filter;
		return this;
	}
	
	/**
	 * Reindex的目标索引
	 * @param destIndex
	 * @return ElasticReindexBuilder
	 */
	public ElasticReindexBuilder dest(String destIndex) {
		this.destIndex = destIndex;
		return this;
	}
	
	/**
	 * 可以把Reindex拆分成几个子任务并发执行
	 * @param slices
	 * @return ElasticReindexBuilder
	 */
	public ElasticReindexBuilder slices(int slices) {
		this.slices = slices;
		return this;
	}
	
	/**
	 * 一次batch Reindex多少文档
	 * @param size
	 * @return ElasticReindexBuilder
	 */
	public ElasticReindexBuilder size(int size) {
		this.size = size;
		return this;
	}
	
	/**
	 * 构造 ReindexRequest 对象, 方便添加更多Reindex选项
	 * @return ReindexRequest
	 */
	public org.elasticsearch.index.reindex.ReindexRequest build() {
		notNull(srcIndex, "srcIndex 不能为null");
		notNull(destIndex, "destIndex 不能为null");
		
		org.elasticsearch.index.reindex.ReindexRequest request = new org.elasticsearch.index.reindex.ReindexRequest();
		request.setSourceIndices(srcIndex);
		request.setDestIndex(destIndex);
		if (filter != null) {
			request.setSourceQuery(filter);
		}
		if (size != 0) {
			request.setSourceBatchSize(size);
		}
		if (slices != 0) {
			request.setSlices(slices);
		}
		return request;
	}
	
	/**
	 * 直接执行Reindex操作, 返回Reindex结果
	 * @return BulkByScrollResponse
	 */
	public org.elasticsearch.index.reindex.BulkByScrollResponse get() {
		try {
			// 使用底层 RestClient 执行 HTTP 请求，避免 API 兼容性问题
			RestClientTransport transport =
					(RestClientTransport) ElasticUtils.QUERY_CLIENT._transport();
			RestClient restClient = transport.restClient();
			
			// 构建 reindex 请求体
			Map<String, Object> requestBody = new HashMap<>();
			Map<String, Object> source = new HashMap<>();
			source.put("index", srcIndex);
			if (size != 0) {
				source.put("size", size);
			}
			if (filter != null) {
				// 将 QueryBuilder 转换为 Map
				String queryJson = JacksonUtils.toJson(filter);
				@SuppressWarnings("unchecked")
				Map<String, Object> queryMap = JacksonUtils.toObject(queryJson, Map.class);
				source.put("query", queryMap);
			}
			
			Map<String, Object> dest = new HashMap<>();
			dest.put("index", destIndex);
			
			requestBody.put("source", source);
			requestBody.put("dest", dest);
			
			if (slices != 0) {
				requestBody.put("slices", slices);
			}
			
			// 执行 HTTP POST 请求
			Request request = new Request("POST", "/_reindex");
			String jsonBody = JacksonUtils.toJson(requestBody);
			request.setJsonEntity(jsonBody);
			
			Response response = restClient.performRequest(request);
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			
			log.info("Reindex response:\n{}", jsonResponse);
			
			// 由于 BulkByScrollResponse 是 7.x 的类，这里我们暂时返回 null
			// TODO: 如果需要返回完整的响应对象，需要创建一个桥接类来解析 JSON 响应
			return null;
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}
}
