package com.awesomecopilot.search8x.builder;

import com.awesomecopilot.search8x.cache.ElasticCacheUtils;
import com.awesomecopilot.search8x.support.DocumentRestSupport;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.search8x.ElasticUtils.QUERY_CLIENT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
public class ElasticIndexDocBuilder {
	
	private String index;
	
	private String doc;
	
	private String id;
	
	private String pipeline;
	/**
	 * 是否要立即刷新
	 */
	private boolean refresh;
	
	public ElasticIndexDocBuilder(String index) {
		this.index = index;
	}
	
	/**
	 * 要批量插入的文档
	 *
	 * @param doc
	 * @return ElasticBulkIndexBuilder
	 */
	public ElasticIndexDocBuilder doc(Object doc) {
		notNull(doc, "docs cannot be null!");
		if (doc instanceof String) {
			this.doc = (String) doc;
			return this;
		}
		String extractedId = ElasticCacheUtils.getIdValue(doc);
		if (isNotBlank(extractedId)) {
			this.id = extractedId;
		}
		this.doc = toJson(doc);
		return this;
	}
	
	public ElasticIndexDocBuilder pipeline(String pipeline) {
		this.pipeline = pipeline;
		return this;
	}
	
	public ElasticIndexDocBuilder id(String id) {
		this.id = id;
		return this;
	}
	
	public ElasticIndexDocBuilder id(int id) {
		this.id = String.valueOf(id);
		return this;
	}
	
	
	/**
	 * 插入后是否强制刷新, 这样可以立即查询到新插入的文档
	 *
	 * @param refresh
	 * @return ElasticBulkIndexBuilder
	 */
	public ElasticIndexDocBuilder refresh(boolean refresh) {
		this.refresh = refresh;
		return this;
	}

	/**
	 * 执行创建
	 * @return docId
	 */
	public String execute() {
		// 使用 Elasticsearch 8.x ElasticsearchClient API
		boolean createMode = false; // 默认是索引模式（存在则更新）
		com.awesomecopilot.search8x.support.DocumentOperationResult result = 
			DocumentRestSupport.indexWithResult(QUERY_CLIENT, index, id, doc, createMode, pipeline, refresh);
		if (!result.isSuccess()) {
			throw new RuntimeException("Failed to index document: " + result.getErrorMessage());
		}
		return result.getId();
	}
}
