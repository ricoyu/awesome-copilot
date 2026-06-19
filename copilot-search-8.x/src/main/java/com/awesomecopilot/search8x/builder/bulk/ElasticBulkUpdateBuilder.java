package com.awesomecopilot.search8x.builder.bulk;

import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.awesomecopilot.search8x.support.BulkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.awesomecopilot.search8x.ElasticUtils.QUERY_CLIENT;

/**
 * 批量更新
 * <p>
 * Copyright: (C), 2021-09-10 16:00
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticBulkUpdateBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticBulkUpdateBuilder.class);
	
	/**
	 * 是否要立即刷新
	 */
	private Boolean refresh;
	
	private List<UpdateDoc> updateDocs = new ArrayList<>();
	
	/**
	 * 内部类：封装更新文档的信息
	 */
	private static class UpdateDoc {
		String index;
		String id;
		Map<String, Object> doc;
		
		UpdateDoc(String index, String id, Map<String, Object> doc) {
			this.index = index;
			this.id = id;
			this.doc = doc;
		}
	}
	
	public ElasticBulkUpdateBuilder() {
	}
	
	/**
	 * 要批量更新的文档
	 * @param index 索引名
	 * @param id  文档的_id
	 * @param doc 要更新的文档的一部分, 如果doc里面还包含嵌套文档, 也要用Map来表示, 不能直接用一个对象
	 * @return ElasticBulkUpdateBuilder
	 */
	public ElasticBulkUpdateBuilder doc(String index, String id, Map<String, Object> doc) {
		updateDocs.add(new UpdateDoc(index, id, doc));
		return this;
	}
	
	/**
	 * 要批量更新的文档
	 * @param index 索引名
	 * @param id  文档的_id
	 * @param doc 要更新的文档的一部分, 这里跟直接传Map类型不同, 这边可变参数是: 字段名, 字段值, 字段名, 字段值 这样成对出现
	 * @return ElasticBulkUpdateBuilder
	 */
	public ElasticBulkUpdateBuilder doc(String index, String id, Object... doc) {
		// 将可变参数转换为 Map
		if (doc.length % 2 != 0) {
			throw new IllegalArgumentException("doc parameters must be in pairs: field, value, field, value...");
		}
		Map<String, Object> docMap = new java.util.HashMap<>();
		for (int i = 0; i < doc.length; i += 2) {
			String field = String.valueOf(doc[i]);
			Object value = doc[i + 1];
			docMap.put(field, value);
		}
		updateDocs.add(new UpdateDoc(index, id, docMap));
		return this;
	}
	
	/**
	 * 批量更新后是否强制刷新
	 * @param refresh
	 * @return ElasticBulkIndexBuilder
	 */
	public ElasticBulkUpdateBuilder refresh(Boolean refresh) {
		this.refresh = refresh;
		return this;
	}
	
	public BulkResult execute() {
		try {
			// 构建 BulkOperation 列表
			List<BulkOperation> bulkOperations = new ArrayList<>();
			
			for (UpdateDoc updateDoc : updateDocs) {
				BulkOperation operation =
						BulkOperation.of(op -> op
								.update(update -> update
										.index(updateDoc.index)
										.id(updateDoc.id)
										.action(action -> action
												.doc(updateDoc.doc)
										)
								)
						);
				bulkOperations.add(operation);
			}
			
			// 构建 BulkRequest
			BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder()
					.operations(bulkOperations);
			
			// 设置刷新策略
			if (refresh != null && refresh) {
				bulkRequestBuilder.refresh(co.elastic.clients.elasticsearch._types.Refresh.True);
			}
			
			BulkRequest bulkRequest = bulkRequestBuilder.build();
			
			// 执行 bulk 请求
			BulkResponse bulkResponse = QUERY_CLIENT.bulk(bulkRequest);
			
			// 解析响应
			return parseBulkResponse(bulkResponse);
		} catch (Exception e) {
			log.error("Bulk update failed", e);
			throw new RuntimeException("Bulk update failed", e);
		}
	}
	
	private BulkResult parseBulkResponse(BulkResponse bulkResponse) {
		BulkResult bulkResult = new BulkResult();
		
		if (bulkResponse.errors()) {
			log.warn("Bulk update has errors");
		}
		
		List<BulkResponseItem> items = bulkResponse.items();
		for (BulkResponseItem item : items) {
			String id = item.id();
			
			if (item.error() != null) {
				// 失败
				bulkResult.fail();
				ErrorCause error = item.error();
				String failureMessage = error.reason() != null ? error.reason() : "Unknown error";
				bulkResult.addFailMessage(failureMessage);
				log.error("Bulk update failed for id {}: {}", id, failureMessage);
			} else {
				// 成功
				bulkResult.success();
				if (id != null && !id.isEmpty()) {
					bulkResult.addId(id);
				}
			}
		}
		
		return bulkResult;
	}
}
