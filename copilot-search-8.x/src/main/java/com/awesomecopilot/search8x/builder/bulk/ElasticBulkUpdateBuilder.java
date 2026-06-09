package com.awesomecopilot.search8x.builder.bulk;

import com.awesomecopilot.search8x.support.BulkResult;
import com.awesomecopilot.search8x.support.DocumentRestSupport;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.awesomecopilot.search8x.ElasticUtils.CLIENT;

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
	
	/**
	 * 是否要立即刷新
	 */
	private Boolean refresh;
	
	private List<UpdateRequest> updateRequests = new ArrayList<>();
	
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
		updateRequests.add(new UpdateRequest(index, id).doc(doc));
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
		updateRequests.add(new UpdateRequest(index, id).doc(doc));
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
		BulkRequest bulkRequest = new BulkRequest();
		if (refresh != null && refresh.booleanValue()) {
			bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		}
		updateRequests.forEach(bulkRequest::add);
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
