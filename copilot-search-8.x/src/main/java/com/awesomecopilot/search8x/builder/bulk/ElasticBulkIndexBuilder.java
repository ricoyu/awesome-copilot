package com.awesomecopilot.search8x.builder.bulk;

import com.awesomecopilot.search8x.cache.ElasticCacheUtils;
import com.awesomecopilot.search8x.support.BulkResult;
import com.awesomecopilot.search8x.support.DocumentRestSupport;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.xcontent.XContentType;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.search8x.ElasticUtils.CLIENT;

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
		BulkRequest bulkRequest = new BulkRequest();
		if (refresh != null && refresh.booleanValue()) {
			bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		}
		docs.stream()
				.filter(Objects::nonNull)
				.forEach((doc) -> {
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
					bulkRequest.add(indexRequest);
				});
		BulkResponse itemResponses = DocumentRestSupport.bulk(CLIENT, bulkRequest);
		BulkResult bulkResult = new BulkResult();
		
		for (Iterator<BulkItemResponse> iterator = itemResponses.iterator(); iterator.hasNext(); ) {
			BulkItemResponse itemResponse = iterator.next();
			if (itemResponse.isFailed()) {
				bulkResult.fail();
				bulkResult.addFailMessage(itemResponse.getFailureMessage());
			} else {
				bulkResult.success();
				bulkResult.addId(itemResponse.getId());
			}
		}
		
		return bulkResult;
	}
}
