package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.awesomecopilot.search8x.exception.DocumentDeleteException;
import com.awesomecopilot.search8x.exception.DocumentGetException;
import com.awesomecopilot.search8x.exception.DocumentSaveException;
import com.awesomecopilot.search8x.exception.DocumentUpdateException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
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
import java.io.InputStream;

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
	 * 使用 Elasticsearch 8.x 客户端创建/索引文档
	 *
	 * @param client ElasticsearchClient
	 * @param index  索引名
	 * @param id     文档ID（可为null，由ES自动生成）
	 * @param json   JSON文档内容
	 * @param create 是否为创建操作（true则文档已存在时报错）
	 * @return IndexResponse
	 */
	public static IndexResponse index(ElasticsearchClient client, String index, String id, String json, boolean create) {
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
			
			org.elasticsearch.client.Response response = restClient.performRequest(request);
			
			// 解析响应为 7.x 兼容的 IndexResponse
			return parseIndexResponse(response);
		} catch (IOException e) {
			throw new DocumentSaveException(e);
		}
	}

	/**
	 * 解析 REST Response 为 7.x IndexResponse
	 */
	private static IndexResponse parseIndexResponse(org.elasticsearch.client.Response response) throws IOException {
		try (InputStream in = response.getEntity().getContent()) {
			return IndexResponse.fromXContent(
					org.elasticsearch.xcontent.XContentFactory.xContent(org.elasticsearch.xcontent.XContentType.JSON)
							.createParser(
									org.elasticsearch.xcontent.NamedXContentRegistry.EMPTY,
									org.elasticsearch.xcontent.DeprecationHandler.IGNORE_DEPRECATIONS,
									in));
		}
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
		try (InputStream in = response.getEntity().getContent()) {
			return GetResponse.fromXContent(
					org.elasticsearch.xcontent.XContentFactory.xContent(org.elasticsearch.xcontent.XContentType.JSON)
							.createParser(
									org.elasticsearch.xcontent.NamedXContentRegistry.EMPTY,
									org.elasticsearch.xcontent.DeprecationHandler.IGNORE_DEPRECATIONS,
									in));
		}
	}

	public static DeleteResponse delete(RestHighLevelClient client, String index, String id) {
		try {
			return client.delete(new DeleteRequest(index, id), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentDeleteException(e);
		}
	}

	public static UpdateResponse update(RestHighLevelClient client, String index, String id, String json, boolean upsert) {
		try {
			UpdateRequest request = new UpdateRequest(index, id);
			request.doc(json, XContentType.JSON);
			if (upsert) {
				request.docAsUpsert(true);
			}
			request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
			return client.update(request, RequestOptions.DEFAULT);
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
		String jsonResponse = org.apache.http.util.EntityUtils.toString(response.getEntity(), "UTF-8");
		
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
