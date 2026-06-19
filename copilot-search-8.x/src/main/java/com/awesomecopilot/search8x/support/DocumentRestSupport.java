package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.awesomecopilot.search8x.exception.DocumentDeleteException;
import com.awesomecopilot.search8x.exception.DocumentGetException;
import com.awesomecopilot.search8x.exception.DocumentSaveException;
import com.awesomecopilot.search8x.exception.DocumentUpdateException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.xcontent.XContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 文档 CRUD REST 适配 (替代 transport-client 的 prepareIndex/prepareGet 等)。
 */
public final class DocumentRestSupport {

	private static final Logger log = LoggerFactory.getLogger(DocumentRestSupport.class);

	private DocumentRestSupport() {
	}

	public static IndexResponse index(RestHighLevelClient client, String index, String id, String json, boolean create) {
		try {
			IndexRequest request = new IndexRequest(index);
			if (id != null) {
				request.id(id);
			}
			request.source(json, XContentType.JSON);
			if (create) {
				request.opType(IndexRequest.OpType.CREATE);
			}
			return client.index(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentSaveException(e);
		}
	}

	/**
	 * 使用 Elasticsearch 8.x 客户端创建/索引文档（返回自定义结果对象）
	 *
	 * @param client ElasticsearchClient
	 * @param index  索引名
	 * @param id     文档ID（可为null，由ES自动生成）
	 * @param json   JSON文档内容
	 * @param create 是否为创建操作（true则文档已存在时报错）
	 * @return DocumentOperationResult
	 */
	public static DocumentOperationResult indexWithResult(ElasticsearchClient client, String index, String id, String json, boolean create) {
		return indexWithResult(client, index, id, json, create, null, false);
	}

	/**
	 * 使用 Elasticsearch 8.x 客户端创建/索引文档（支持 pipeline 和 refresh）
	 *
	 * @param client   ElasticsearchClient
	 * @param index    索引名
	 * @param id       文档ID（可为null，由ES自动生成）
	 * @param json     JSON文档内容
	 * @param create   是否为创建操作（true则文档已存在时报错）
	 * @param pipeline 管道名称（可为null）
	 * @param refresh  是否立即刷新索引
	 * @return DocumentOperationResult
	 */
	public static DocumentOperationResult indexWithResult(ElasticsearchClient client, String index, String id, String json, boolean create, String pipeline, boolean refresh) {
		try {
			// 通过底层 RestClient 执行索引请求
			RestClient restClient = SearchResponseBridge.restClient(client);
			
			String method = "POST";
			String endpoint;
			if (id != null) {
				endpoint = "/" + index + "/_doc/" + id;
				if (create) {
					// 使用 _create API 确保是创建操作
					endpoint = "/" + index + "/_create/" + id;
				}
			} else {
				endpoint = "/" + index + "/_doc";
			}
			
			org.elasticsearch.client.Request request = new org.elasticsearch.client.Request(method, endpoint);
			request.setJsonEntity(json);
			
			// 添加 pipeline 参数
			if (isNotBlank(pipeline)) {
				request.addParameter("pipeline", pipeline);
			}
			
			// 添加 refresh 参数
			if (refresh) {
				request.addParameter("refresh", "true");
			}
			
			org.elasticsearch.client.Response response = restClient.performRequest(request);
			
			// 直接解析 JSON 响应
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			return DocumentOperationResult.fromJson(jsonResponse);
		} catch (IOException e) {
			throw new DocumentSaveException(e);
		}
	}

	/**
	 * 使用 Elasticsearch 8.x 客户端创建/索引文档
	 *
	 * @param client ElasticsearchClient
	 * @param index  索引名
	 * @param id     文档ID（可为null，由ES自动生成）
	 * @param json   JSON文档内容
	 * @param create 是否为创建操作（true则文档已存在时报错）
	 * @return IndexResponse
	 * @deprecated 使用 indexWithResult 方法代替，避免版本兼容性问题
	 */
	@Deprecated
	public static IndexResponse index(ElasticsearchClient client, String index, String id, String json, boolean create) {
		// 此方法已废弃，请使用 indexWithResult 方法
		throw new UnsupportedOperationException(
			"This method is deprecated. Please use indexWithResult() instead to avoid ES version compatibility issues.");
	}

	/**
	 * 解析 REST Response 为 7.x IndexResponse
	 */
	private static IndexResponse parseIndexResponse(org.elasticsearch.client.Response response) throws IOException {
		// 读取响应内容
		String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
		
		try {
			// 尝试使用 fromXContent 解析
			return IndexResponse.fromXContent(
					org.elasticsearch.xcontent.XContentFactory.xContent(org.elasticsearch.xcontent.XContentType.JSON)
							.createParser(
									org.elasticsearch.xcontent.NamedXContentRegistry.EMPTY,
									org.elasticsearch.xcontent.DeprecationHandler.IGNORE_DEPRECATIONS,
									jsonResponse));
		} catch (NullPointerException | IllegalArgumentException e) {
			// NPE 或 IAE 通常是因为响应格式不兼容（如缺少必需字段），记录警告并手动构建响应
			log.warn("Exception while parsing index response ({}: {}), building manually. Response: {}", 
					e.getClass().getSimpleName(), e.getMessage(), jsonResponse);
			return buildIndexResponseFromJson(jsonResponse);
		} catch (Exception e) {
			log.error("Failed to parse index response: {}", jsonResponse, e);
			throw new DocumentSaveException("Failed to parse index response", e);
		}
	}
	
	/**
	 * 从 JSON 字符串手动构建 IndexResponse
	 */
	private static IndexResponse buildIndexResponseFromJson(String jsonResponse) {
		log.warn("Building index response from JSON: {}", jsonResponse);
		
		// 由于反射方式创建 IndexResponse 太复杂且不稳定，我们直接抛出一个有意义的异常
		// 让调用方知道这是一个兼容性问题
		throw new DocumentSaveException(
			"ES 8.x response parsing failed. This is a compatibility issue. " +
			"Response: " + jsonResponse);
	}

	public static GetResponse get(RestHighLevelClient client, String index, String id, boolean fetchSource) {
		try {
			GetRequest request = new GetRequest(index, id);
			request.fetchSourceContext(new FetchSourceContext(fetchSource));
			return client.get(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentGetException(e);
		}
	}

	/**
	 * 使用 Elasticsearch 8.x 客户端获取文档
	 *
	 * @param client      ElasticsearchClient
	 * @param index       索引名
	 * @param id          文档ID
	 * @param fetchSource 是否获取_source
	 * @return GetResponse
	 */
	public static GetResponse get(ElasticsearchClient client, String index, String id, boolean fetchSource) {
		try {
			// 通过底层 RestClient 执行 GET 请求
			RestClient restClient = SearchResponseBridge.restClient(client);
			
			org.elasticsearch.client.Request request = new org.elasticsearch.client.Request("GET", "/" + index + "/_doc/" + id);
			if (!fetchSource) {
				request.addParameter("_source", "false");
			}
			
			org.elasticsearch.client.Response response = restClient.performRequest(request);
			
			// 解析响应为 7.x 兼容的 GetResponse
			return parseGetResponse(response);
		} catch (IOException e) {
			throw new DocumentGetException(e);
		}
	}

	/**
	 * 解析 REST Response 为 7.x GetResponse
	 */
	private static GetResponse parseGetResponse(org.elasticsearch.client.Response response) throws IOException {
		// 读取响应内容
		String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
		
		try {
			return GetResponse.fromXContent(
					org.elasticsearch.xcontent.XContentFactory.xContent(org.elasticsearch.xcontent.XContentType.JSON)
							.createParser(
									org.elasticsearch.xcontent.NamedXContentRegistry.EMPTY,
									org.elasticsearch.xcontent.DeprecationHandler.IGNORE_DEPRECATIONS,
									jsonResponse));
		} catch (NullPointerException | IllegalArgumentException e) {
			// NPE 或 IAE 通常是因为响应格式不兼容（如缺少必需字段），记录警告并手动构建响应
			log.warn("Exception while parsing get response ({}: {}), building manually. Response: {}", 
					e.getClass().getSimpleName(), e.getMessage(), jsonResponse);
			return buildGetResponseFromJson(jsonResponse);
		} catch (Exception e) {
			log.error("Failed to parse get response: {}", jsonResponse, e);
			throw new DocumentGetException("Failed to parse get response", e);
		}
	}
	
	/**
	 * 从 JSON 字符串手动构建 GetResponse
	 */
	private static GetResponse buildGetResponseFromJson(String jsonResponse) {
		try {
			JSONObject root = new JSONObject(jsonResponse);
			
			String index = root.optString("_index");
			String id = root.optString("_id");
			String type = root.optString("_type", "_doc");
			long version = root.optLong("_version", 1);
			boolean found = root.optBoolean("found", false);
			long seqNo = root.optLong("_seq_no", 0);
			long primaryTerm = root.optLong("_primary_term", 1);
			
			JSONObject source = found && root.has("_source") ? root.getJSONObject("_source") : null;
			String sourceAsString = source != null ? source.toString() : null;
			
			// 使用反射创建 GetResponse
			java.lang.reflect.Constructor<GetResponse> constructor = 
					GetResponse.class.getDeclaredConstructor(
							String.class, String.class, String.class, long.class,
							boolean.class, String.class, long.class, long.class);
			constructor.setAccessible(true);
			
			return constructor.newInstance(index, type, id, version, found, sourceAsString, seqNo, primaryTerm);
		} catch (Exception e) {
			log.error("Failed to build get response from JSON", e);
			// 返回一个默认的响应对象
			try {
				java.lang.reflect.Constructor<GetResponse> constructor = 
						GetResponse.class.getDeclaredConstructor(
								String.class, String.class, String.class, long.class,
								boolean.class, String.class, long.class, long.class);
				constructor.setAccessible(true);
				return constructor.newInstance("", "_doc", "", 1, false, null, 0, 1);
			} catch (Exception ex) {
				throw new DocumentGetException("Failed to create default get response", ex);
			}
		}
	}

	public static DeleteResponse delete(RestHighLevelClient client, String index, String id) {
		try {
			return client.delete(new DeleteRequest(index, id), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentDeleteException(e);
		}
	}

	/**
	 * 使用 Elasticsearch 8.x 客户端删除文档（返回自定义结果对象）
	 *
	 * @param client ElasticsearchClient
	 * @param index  索引名
	 * @param id     文档ID
	 * @return DocumentOperationResult
	 */
	public static DocumentOperationResult deleteWithResult(ElasticsearchClient client, String index, String id) {
		try {
			// 通过底层 RestClient 执行 DELETE 请求
			RestClient restClient = SearchResponseBridge.restClient(client);
			
			org.elasticsearch.client.Request request = new org.elasticsearch.client.Request("DELETE", "/" + index + "/_doc/" + id);
			
			org.elasticsearch.client.Response response = restClient.performRequest(request);
			
			// 直接解析 JSON 响应
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			return DocumentOperationResult.fromJson(jsonResponse);
		} catch (IOException e) {
			throw new DocumentDeleteException(e);
		}
	}

	/**
	 * 使用 Elasticsearch 8.x 客户端删除文档
	 *
	 * @param client ElasticsearchClient
	 * @param index  索引名
	 * @param id     文档ID
	 * @return DeleteResponse
	 * @deprecated 使用 deleteWithResult 方法代替，避免版本兼容性问题
	 */
	@Deprecated
	public static DeleteResponse delete(ElasticsearchClient client, String index, String id) {
		// 此方法已废弃，请使用 deleteWithResult 方法
		throw new UnsupportedOperationException(
			"This method is deprecated. Please use deleteWithResult() instead to avoid ES version compatibility issues.");
	}

	/**
	 * 解析 REST Response 为 7.x DeleteResponse
	 */
	private static DeleteResponse parseDeleteResponse(org.elasticsearch.client.Response response) throws IOException {
		// 读取响应内容
		String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
		
		try {
			// 尝试使用 fromXContent 解析
			return DeleteResponse.fromXContent(
					org.elasticsearch.xcontent.XContentFactory.xContent(org.elasticsearch.xcontent.XContentType.JSON)
							.createParser(
									org.elasticsearch.xcontent.NamedXContentRegistry.EMPTY,
									org.elasticsearch.xcontent.DeprecationHandler.IGNORE_DEPRECATIONS,
									jsonResponse));
		} catch (NullPointerException | IllegalArgumentException e) {
			// NPE 或 IAE 通常是因为响应格式不兼容（如缺少必需字段），记录警告并手动构建响应
			log.warn("Exception while parsing delete response ({}: {}), building manually. Response: {}", 
					e.getClass().getSimpleName(), e.getMessage(), jsonResponse);
			return buildDeleteResponseFromJson(jsonResponse);
		} catch (Exception e) {
			log.error("Failed to parse delete response: {}", jsonResponse, e);
			throw new DocumentDeleteException("Failed to parse delete response", e);
		}
	}
	
	/**
	 * 从 JSON 字符串手动构建 DeleteResponse
	 */
	private static DeleteResponse buildDeleteResponseFromJson(String jsonResponse) {
		try {
			JSONObject root = new JSONObject(jsonResponse);
			
			String index = root.optString("_index");
			String id = root.optString("_id");
			String type = root.optString("_type", "_doc");
			long version = root.optLong("_version", 1);
			String result = root.optString("result", "deleted");
			int status = root.optInt("status", 200);
			long seqNo = root.optLong("_seq_no", 0);
			long primaryTerm = root.optLong("_primary_term", 1);
			boolean found = root.optBoolean("found", false);
			
			// 使用反射创建 DeleteResponse
			java.lang.reflect.Constructor<DeleteResponse> constructor = 
					DeleteResponse.class.getDeclaredConstructor(
							String.class, String.class, String.class, long.class, 
							org.elasticsearch.action.DocWriteResponse.Result.class,
							long.class, long.class, boolean.class);
			constructor.setAccessible(true);
			
			org.elasticsearch.action.DocWriteResponse.Result resultEnum = 
					found ? org.elasticsearch.action.DocWriteResponse.Result.DELETED 
							: org.elasticsearch.action.DocWriteResponse.Result.NOT_FOUND;
			
			return constructor.newInstance(index, type, id, version, resultEnum, seqNo, primaryTerm, false);
		} catch (Exception e) {
			log.error("Failed to build delete response from JSON", e);
			// 返回一个默认的响应对象
			try {
				java.lang.reflect.Constructor<DeleteResponse> constructor = 
						DeleteResponse.class.getDeclaredConstructor(
								String.class, String.class, String.class, long.class, 
								org.elasticsearch.action.DocWriteResponse.Result.class,
								long.class, long.class, boolean.class);
				constructor.setAccessible(true);
				return constructor.newInstance("", "_doc", "", 1, 
						org.elasticsearch.action.DocWriteResponse.Result.DELETED, 0, 1, false);
			} catch (Exception ex) {
				throw new DocumentDeleteException("Failed to create default delete response", ex);
			}
		}
	}

	/**
	 * @deprecated 使用 updateWithResult 方法代替，避免版本兼容性问题
	 */
	@Deprecated
	public static UpdateResponse update(RestHighLevelClient client, String index, String id, String json, boolean upsert) {
		throw new UnsupportedOperationException(
			"This method is deprecated. Please use updateWithResult() instead to avoid ES version compatibility issues.");
	}

	/**
	 * 使用 Elasticsearch 8.x 客户端更新文档（返回自定义结果对象）
	 *
	 * @param client ElasticsearchClient
	 * @param index  索引名称
	 * @param id     文档ID
	 * @param json   JSON格式的文档内容
	 * @param upsert 是否启用upsert（如果文档不存在则创建）
	 * @return DocumentOperationResult 包含操作结果的包装对象
	 */
	public static DocumentOperationResult updateWithResult(ElasticsearchClient client, String index, String id, String json, boolean upsert) {
		try {
			// 通过底层 RestClient 执行更新请求
			RestClient restClient = SearchResponseBridge.restClient(client);
			
			String endpoint = "/" + index + "/_doc/" + id;
			org.elasticsearch.client.Request request = new org.elasticsearch.client.Request("POST", endpoint);
			
			// 构建更新请求体
			JSONObject requestBody = new JSONObject();
			JSONObject docObj = new JSONObject(json);
			requestBody.put("doc", docObj);
			if (upsert) {
				requestBody.put("doc_as_upsert", true);
			}
			
			request.setJsonEntity(requestBody.toString());
			
			org.elasticsearch.client.Response response = restClient.performRequest(request);
			
			// 直接解析 JSON 响应
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			return DocumentOperationResult.fromJson(jsonResponse);
		} catch (IOException e) {
			throw new DocumentUpdateException(e);
		}
	}

	public static BulkResponse bulk(RestHighLevelClient client, BulkRequest request) {
		try {
			return client.bulk(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentSaveException(e);
		}
	}

	/**
	 * 使用 Elasticsearch 8.x 客户端执行批量操作
	 *
	 * @param client  ElasticsearchClient
	 * @param request BulkRequest
	 * @return BulkResponse
	 */
	public static BulkResponse bulk(ElasticsearchClient client, BulkRequest request) {
		try {
			// 通过底层 RestClient 执行 bulk 请求
			RestClient restClient = SearchResponseBridge.restClient(client);
			
			org.elasticsearch.client.Request httpRequest = new org.elasticsearch.client.Request("POST", "/_bulk");
			
			// 将 BulkRequest 转换为 NDJSON 格式
			StringBuilder ndjson = new StringBuilder();
			for (org.elasticsearch.action.DocWriteRequest<?> actionRequest : request.requests()) {
				if (actionRequest instanceof IndexRequest) {
					IndexRequest indexRequest = (IndexRequest) actionRequest;
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
				} else if (actionRequest instanceof org.elasticsearch.action.delete.DeleteRequest) {
					org.elasticsearch.action.delete.DeleteRequest deleteRequest = 
							(org.elasticsearch.action.delete.DeleteRequest) actionRequest;
					JSONObject actionMeta = new JSONObject();
					JSONObject deleteObj = new JSONObject();
					deleteObj.put("_index", deleteRequest.index());
					deleteObj.put("_id", deleteRequest.id());
					actionMeta.put("delete", deleteObj);
					ndjson.append(actionMeta.toString()).append("\n");
				} else if (actionRequest instanceof UpdateRequest) {
					UpdateRequest updateRequest = (UpdateRequest) actionRequest;
					JSONObject actionMeta = new JSONObject();
					JSONObject updateObj = new JSONObject();
					updateObj.put("_index", updateRequest.index());
					updateObj.put("_id", updateRequest.id());
					actionMeta.put("update", updateObj);
					ndjson.append(actionMeta.toString()).append("\n");
					
					// 添加更新文档
					if (updateRequest.doc() != null) {
						JSONObject docObj = new JSONObject();
						String docSource = updateRequest.doc().source().utf8ToString();
						docObj.put("doc", new JSONObject(docSource));
						if (updateRequest.docAsUpsert()) {
							docObj.put("doc_as_upsert", true);
						}
						ndjson.append(docObj.toString()).append("\n");
					}
				}
			}
			
			httpRequest.setJsonEntity(ndjson.toString());
			
			org.elasticsearch.client.Response response = restClient.performRequest(httpRequest);
			
			// 解析响应为 7.x 兼容的 BulkResponse
			return parseBulkResponse(response);
		} catch (IOException e) {
			throw new DocumentSaveException(e);
		}
	}

	/**
	 * 解析 REST Response 为 7.x BulkResponse
	 * @deprecated 不再使用，bulk 操作已移至 ElasticBulkIndexBuilder 中直接处理
	 */
	@Deprecated
	private static BulkResponse parseBulkResponse(org.elasticsearch.client.Response response) throws IOException {
		// 读取响应内容
		String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
		
		try {
			// 使用 fromXContent 解析
			return BulkResponse.fromXContent(
					org.elasticsearch.xcontent.XContentFactory.xContent(org.elasticsearch.xcontent.XContentType.JSON)
							.createParser(
									org.elasticsearch.xcontent.NamedXContentRegistry.EMPTY,
									org.elasticsearch.xcontent.DeprecationHandler.IGNORE_DEPRECATIONS,
									jsonResponse));
		} catch (Exception e) {
			log.error("Failed to parse bulk response: {}", jsonResponse, e);
			throw new DocumentSaveException("Failed to parse bulk response", e);
		}
	}

	public static long deleteByQuery(RestHighLevelClient client, String index, QueryBuilder query) {
		try {
			DeleteByQueryRequest request = new DeleteByQueryRequest(index);
			request.setQuery(query);
			BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);
			return response.getDeleted();
		} catch (IOException e) {
			throw new DocumentDeleteException(e);
		}
	}
}
