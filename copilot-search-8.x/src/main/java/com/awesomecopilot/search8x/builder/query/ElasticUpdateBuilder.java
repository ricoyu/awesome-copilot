package com.awesomecopilot.search8x.builder.query;

import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.exception.DocumentUpdateException;
import com.awesomecopilot.search8x.support.UpdateResult;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.search8x.support.UpdateResult.Result.VERSION_CONFLICT;

/**
 * <p>
 * Copyright: (C), 2021-09-08 16:20
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticUpdateBuilder {
	
	private String index;
	
	private String id;
	
	/**
	 * 更新文档的一部分, 文档不存在时创建文档
	 */
	private Boolean upsert;
	
	/**
	 * 文档
	 */
	private Object doc;
	
	/**
	 * 更新后是否立即刷新? 立即刷新可以马上搜索到, 不立即刷新可能要1s过后
	 */
	private Boolean refresh;

	/**
	 * 这个文档每次修改, ifSeqNo都会增加1
	 */
	private Long ifSeqNo;

	/**
	 * 每个主分片的当前期号。如果主分片因故障而重新分配, 期号会增加。
	 * 这可以用来识别文档所在的主分片是否在你上次读取后发生了变化。
	 */
	private Long ifPrimaryTerm;
	
	public ElasticUpdateBuilder(String index) {
		Objects.requireNonNull(index, "index cannot be null!");
		this.index = index;
	}
	
	/**
	 * 要更新的文档的ID
	 *
	 * @param id
	 * @return ElasticUpdateBuilder
	 */
	public ElasticUpdateBuilder id(Integer id) {
		Objects.requireNonNull(id, "id cannot be null!");
		this.id = id.toString();
		return this;
	}

	/**
	 * 要更新的文档的ID
	 *
	 * @param id
	 * @return ElasticUpdateBuilder
	 */
	public ElasticUpdateBuilder id(String id) {
		Objects.requireNonNull(id, "id cannot be null!");
		this.id = id;
		return this;
	}

	/**
	 * 更新文档的一部分, 文档不存在时创建文档
	 *
	 * @param upsert
	 * @return ElasticUpdateBuilder
	 */
	public ElasticUpdateBuilder upsert(Boolean upsert) {
		this.upsert = upsert;
		return this;
	}
	
	/**
	 * 要更新的内容, 可以是一串JSON字符串, 也可以是对象类型(会自动序列化成JSON)
	 *
	 * @param doc
	 * @return ElasticUpdateBuilder
	 */
	public ElasticUpdateBuilder doc(Object doc) {
		Objects.requireNonNull(doc, "doc cannot be null!");
		this.doc = doc;
		return this;
	}
	
	/**
	 * 更新后是否立即刷新? 立即刷新可以马上搜索到, 不立即刷新可能要1s过后
	 *
	 * @param refresh
	 * @return ElasticUpdateBuilder
	 */
	public ElasticUpdateBuilder refresh(Boolean refresh) {
		this.refresh = refresh;
		return this;
	}

	/**
	 * Elasticsearch 为索引中的每次变更 (包括添加、更新、删除操作) 维护一个全局序列号。
	 * 这个序列号是在索引级别上的, 不是针对单个文档的。每当索引中发生更改时, 序列号递增。
	 * @param ifSeqNo
	 * @return
	 */
	public ElasticUpdateBuilder ifSeqNo(Long ifSeqNo) {
		this.ifSeqNo = ifSeqNo;
		return this;
	}

	/**
	 * 每个主分片的当前期号。如果主分片因故障而重新分配, 期号会增加。
	 * 这可以用来识别文档所在的主分片是否在你上次读取后发生了变化。
	 * @param ifPrimaryTerm
	 * @return
	 */
	public ElasticUpdateBuilder ifPrimaryTerm(Long ifPrimaryTerm) {
		this.ifPrimaryTerm = ifPrimaryTerm;
		return this;
	}
	
	public UpdateResult update() {
		String document;
		if (doc instanceof String) {
			document = (String) doc;
		} else {
			document = toJson(doc);
		}
		
		// 使用底层 RestClient 执行更新请求
		try {
			co.elastic.clients.transport.rest_client.RestClientTransport transport = 
					(co.elastic.clients.transport.rest_client.RestClientTransport) ElasticUtils.QUERY_CLIENT._transport();
			org.elasticsearch.client.RestClient restClient = transport.restClient();
			
			String endpoint = "/" + index + "/_doc/" + id;
			org.elasticsearch.client.Request request = new org.elasticsearch.client.Request("POST", endpoint);
			
			// 添加 refresh 参数
			if (refresh != null && refresh.booleanValue()) {
				request.addParameter("refresh", "true");
			}
			
			// 构建更新请求体
			JSONObject requestBody = new JSONObject();
			JSONObject docObj = new JSONObject(document);
			requestBody.put("doc", docObj);
			if (upsert != null && upsert.booleanValue()) {
				requestBody.put("doc_as_upsert", true);
			}
			
			request.setJsonEntity(requestBody.toString());
			
			org.elasticsearch.client.Response response = restClient.performRequest(request);
			String jsonResponse = org.apache.http.util.EntityUtils.toString(response.getEntity(), "UTF-8");
			
			// 解析响应
			return parseUpdateResponse(jsonResponse);
		} catch (VersionConflictEngineException e) {
			UpdateResult updateResult = new UpdateResult();
			updateResult.setResult(VERSION_CONFLICT);
			return updateResult;
		} catch (IOException e) {
			throw new DocumentUpdateException(e);
		}
	}
	
	/**
	 * 解析更新操作的 JSON 响应为 UpdateResult
	 */
	private UpdateResult parseUpdateResponse(String jsonResponse) {
		try {
			org.json.JSONObject root = new org.json.JSONObject(jsonResponse);
			
			String resultStr = root.optString("result", "noop");
			
			UpdateResult updateResult = new UpdateResult();
			// 根据 result 字符串设置对应的枚举值
			if ("updated".equals(resultStr)) {
				updateResult.setResult(UpdateResult.Result.UPDATED);
			} else if ("created".equals(resultStr)) {
				updateResult.setResult(UpdateResult.Result.CREATED);
			} else {
				updateResult.setResult(UpdateResult.Result.NOOP);
			}
			
			// 从响应中提取 version, seq_no, primary_term
			Long version = root.optLong("_version", 0);
			Long seqNo = root.optLong("_seq_no", 0);
			Long primaryTerm = root.optLong("_primary_term", 1);
			
			// 使用反射设置私有字段（因为 UpdateResult 没有提供 setter）
			java.lang.reflect.Field versionField = UpdateResult.class.getDeclaredField("version");
			versionField.setAccessible(true);
			versionField.set(updateResult, version);
			
			updateResult.setIfSeqNo(seqNo);
			updateResult.setIfPrimaryTerm(primaryTerm);
			
			return updateResult;
		} catch (Exception e) {
			throw new com.awesomecopilot.search8x.exception.DocumentUpdateException("Failed to parse update response: " + jsonResponse, e);
		}
	}
}
