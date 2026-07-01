package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.core.UpdateResponse;
import lombok.Data;

import java.util.Map;

/**
 * ES 8.x 更新结果封装
 * 
 * @author Rico Yu ricoyu520@gmail.com
 */
@Data
public class UpdateResult {
	
	private Long version;
	
	private Long ifSeqNo;
	
	private Long ifPrimaryTerm;
	
	private Result result;
	
	public static enum Result {
		
		/**
		 * 创建了文档
		 */
		CREATED,
		
		/**
		 * 更新了文档
		 */
		UPDATED,
		
		/**
		 * 没有做任何操作
		 */
		NOOP,
		
		/**
		 * 版本冲突, 更新失败
		 */
		VERSION_CONFLICT;
	}
	
	/**
	 * 从 ES 8.x UpdateResponse 封装 UpdateResult
	 * @param response ES 8.x UpdateResponse
	 * @return UpdateResult
	 */
	public static UpdateResult from(UpdateResponse<Map<String, Object>> response) {
		UpdateResult updateResult = new UpdateResult();
		updateResult.version = response.version();
		updateResult.ifSeqNo = response.seqNo();
		updateResult.ifPrimaryTerm = response.primaryTerm();
		
		switch (response.result()) {
			case Created:
				updateResult.result = Result.CREATED;
				break;
			case Updated:
				updateResult.result = Result.UPDATED;
				break;
			case NoOp:
				updateResult.result = Result.NOOP;
				break;
			default:
				updateResult.result = Result.NOOP;
		}
		
		return updateResult;
	}
}
