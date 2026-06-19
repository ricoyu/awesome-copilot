package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.exception.IndexCreateException;
import com.awesomecopilot.search8x.exception.ListIndicesException;
import com.awesomecopilot.search8x.exception.MappingException;
import com.awesomecopilot.search8x.exception.PutMappingException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 索引/集群管理 REST 适配 (替代 transport-client 的 admin().indices() 等)。
 */
public final class IndicesRestSupport {

	private IndicesRestSupport() {
	}

	/**
	 * 使用 ES 7.x RestHighLevelClient 创建索引（已废弃）
	 * @deprecated 请使用 createIndex(ElasticsearchClient, String, Map, Map) 方法
	 */
	@Deprecated
	public static boolean createIndex(RestHighLevelClient client, CreateIndexRequest request) {
		try {
			// 直接通过底层 RestClient 执行 HTTP 请求，避免 include_type_name 参数问题
			RestClient restClient = client.getLowLevelClient();
			
			// 构建请求体
			String endpoint = "/" + request.indices()[0];
			Request httpRequest = new Request("PUT", endpoint);
			
			// 从 CreateIndexRequest 提取 settings 和 mappings
			Map<String, Object> requestBody = new HashMap<>();
			if (request.settings() != null) {
				// 通过反射获取 Settings 的 Map 表示
				try {
					java.lang.reflect.Method method = request.settings().getClass().getMethod("getAsMap");
					method.setAccessible(true);
					@SuppressWarnings("unchecked")
					Map<String, String> settingsMap = (Map<String, String>) method.invoke(request.settings());
					requestBody.put("settings", settingsMap);
				} catch (Exception e) {
					// 如果反射失败，忽略 settings
				}
			}
			if (request.mappings() != null) {
				requestBody.put("mappings", request.mappings());
			}
			
			if (!requestBody.isEmpty()) {
				String jsonBody = JacksonUtils.toJson(requestBody);
				httpRequest.setJsonEntity(jsonBody);
			}
			
			// 执行请求
			Response response = restClient.performRequest(httpRequest);
			return response.getStatusLine().getStatusCode() == 200;
		} catch (IOException e) {
			throw new IndexCreateException(e);
		}
	}

	/**
	 * 使用 ES 8.x Java Client 创建索引
	 *
	 * @param client   ElasticsearchClient
	 * @param index    索引名称
	 * @param settings 索引配置（可为 null）
	 * @param mappings 映射定义（可为 null）
	 * @return boolean 是否创建成功
	 */
	public static boolean createIndex(ElasticsearchClient client,
	                                  String index,
	                                  Map<String, Object> settings,
	                                  Map<String, Object> mappings) {
		try {
			// 构建创建索引请求
			co.elastic.clients.elasticsearch.indices.CreateIndexRequest.Builder builder =
					new co.elastic.clients.elasticsearch.indices.CreateIndexRequest.Builder()
							.index(index);

			// 设置 mappings
			if (mappings != null && !mappings.isEmpty()) {
				// 将 Map 转换为 JsonData
				String mappingsJson = JacksonUtils.toJson(mappings);
				co.elastic.clients.elasticsearch._types.mapping.TypeMapping typeMapping =
						co.elastic.clients.elasticsearch._types.mapping.TypeMapping.of(m -> m
								.withJson(new java.io.StringReader(mappingsJson))
						);
				builder.mappings(typeMapping);
			}

			// 设置 settings
			if (settings != null && !settings.isEmpty()) {
				String settingsJson = JacksonUtils.toJson(settings);
				co.elastic.clients.elasticsearch.indices.IndexSettings indexSettings =
						co.elastic.clients.elasticsearch.indices.IndexSettings.of(s -> s
								.withJson(new java.io.StringReader(settingsJson))
						);
				builder.settings(indexSettings);
			}

			// 执行创建索引请求
			co.elastic.clients.elasticsearch.indices.CreateIndexResponse response =
					client.indices().create(builder.build());

			return response.acknowledged();
		} catch (Exception e) {
			throw new IndexCreateException(e);
		}
	}

	public static boolean existsIndex(RestHighLevelClient client, String... indices) {
		try {
			GetIndexRequest request = new GetIndexRequest().indices(indices);
			return client.indices().exists(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	public static boolean deleteIndex(RestHighLevelClient client, String... indices) {
		try {
			DeleteIndexRequest request = new DeleteIndexRequest(indices);
			AcknowledgedResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
			return response.isAcknowledged();
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	public static List<String> listIndexNames(RestHighLevelClient client) {
		return Arrays.asList(getIndices(client, "*").getIndices());
	}

	/**
	 * 使用 ES 8.x Java Client 列出所有索引名称
	 *
	 * @param client ElasticsearchClient
	 * @return List<String> 索引名称列表
	 */
	public static List<String> listIndexNames(ElasticsearchClient client) {
		try {
			// 使用 cat.indices API 获取所有索引
			IndicesResponse response =
					client.cat().indices(builder -> builder);
			
			// 提取索引名称列表
			List<String> indexNames = new ArrayList<>();
			if (response.valueBody() != null) {
				for (IndicesRecord record : response.valueBody()) {
					if (record.index() != null) {
						indexNames.add(record.index());
					}
				}
			}
			return indexNames;
		} catch (Exception e) {
			throw new ListIndicesException(e);
		}
	}

	/**
	 * 使用 ES 8.x Java Client 列出所有索引详细信息
	 *
	 * @param client ElasticsearchClient
	 * @return List<Index> 索引详细信息列表
	 */
	public static List<com.awesomecopilot.search8x.vo.Index> listIndices(ElasticsearchClient client) {
		try {
			// 使用 cat.indices API 获取所有索引的详细信息
			// 指定需要的字段：index, uuid, pri (主分片数), rep (副本数)
			IndicesResponse response = client.cat().indices(builder -> builder
					.h("index", "uuid", "pri", "rep")
			);
			
			List<com.awesomecopilot.search8x.vo.Index> indexList = new ArrayList<>();
			if (response.valueBody() != null) {
				for (IndicesRecord record : response.valueBody()) {
					com.awesomecopilot.search8x.vo.Index index = new com.awesomecopilot.search8x.vo.Index();
					index.setName(record.index());
					index.setUuid(record.uuid());
					
					// 解析主分片数
					if (record.pri() != null) {
						try {
							index.setNumberOfShards(Integer.parseInt(record.pri()));
						} catch (NumberFormatException e) {
							index.setNumberOfShards(1); // 默认值
						}
					} else {
						index.setNumberOfShards(1); // 默认值
					}
					
					// 解析副本数
					if (record.rep() != null) {
						try {
							index.setNumberOfReplicas(Integer.parseInt(record.rep()));
						} catch (NumberFormatException e) {
							index.setNumberOfReplicas(0); // 默认值
						}
					} else {
						index.setNumberOfReplicas(0); // 默认值
					}
					
					indexList.add(index);
				}
			}
			return indexList;
		} catch (Exception e) {
			throw new ListIndicesException(e);
		}
	}

	public static GetIndexResponse getIndices(RestHighLevelClient client, String... indices) {
		try {
			GetIndexRequest request = new GetIndexRequest().indices(indices);
			return client.indices().get(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	public static boolean updateAliases(RestHighLevelClient client, IndicesAliasesRequest request) {
		try {
			AcknowledgedResponse response = client.indices().updateAliases(request, RequestOptions.DEFAULT);
			return response.isAcknowledged();
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	public static boolean addAlias(RestHighLevelClient client, String index, String alias) {
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		request.addAliasAction(IndicesAliasesRequest.AliasActions.add().index(index).alias(alias));
		return updateAliases(client, request);
	}

	public static boolean addAlias(RestHighLevelClient client, String[] indices, String alias, QueryBuilder filter) {
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		request.addAliasAction(IndicesAliasesRequest.AliasActions.add().indices(indices).alias(alias).filter(filter));
		return updateAliases(client, request);
	}

	public static boolean removeAlias(RestHighLevelClient client, String index, String alias) {
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		request.addAliasAction(IndicesAliasesRequest.AliasActions.remove().index(index).alias(alias));
		return updateAliases(client, request);
	}

	public static GetIndexTemplatesResponse getIndexTemplates(RestHighLevelClient client, String templateName) {
		try {
			GetIndexTemplatesRequest request = new GetIndexTemplatesRequest(templateName);
			return client.indices().getTemplate(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	public static boolean deleteIndexTemplate(RestHighLevelClient client, String templateName) {
		try {
			DeleteIndexTemplateRequest request = new DeleteIndexTemplateRequest(templateName);
			AcknowledgedResponse response = client.indices().deleteTemplate(request, RequestOptions.DEFAULT);
			return response.isAcknowledged();
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	public static boolean putIndexTemplate(RestHighLevelClient client, PutIndexTemplateRequest request) {
		try {
			AcknowledgedResponse response = client.indices().putTemplate(request, RequestOptions.DEFAULT);
			return response.isAcknowledged();
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	public static boolean updateIndexSettings(RestHighLevelClient client, String[] indices, Settings settings) {
		try {
			UpdateSettingsRequest request = new UpdateSettingsRequest(indices);
			request.settings(settings);
			AcknowledgedResponse response = client.indices().putSettings(request, RequestOptions.DEFAULT);
			return response.isAcknowledged();
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	/**
	 * 使用 ES 8.x Java Client 更新索引的 Settings
	 *
	 * @param client   ElasticsearchClient
	 * @param indices  索引名称数组
	 * @param settings 索引配置（如 blocks.read_only、refresh_interval 等）
	 * @return boolean 是否更新成功
	 */
	public static boolean updateIndexSettings(ElasticsearchClient client, String[] indices, Map<String, Object> settings) {
		try {
			// 使用底层 RestClient 执行 HTTP 请求，避免 API 兼容性问题
			RestClientTransport transport =
					(RestClientTransport) client._transport();
			RestClient restClient = transport.restClient();
			
			// 执行 HTTP PUT 请求更新 settings
			String indexName = String.join(",", indices);
			Request request = new Request("PUT", "/" + indexName + "/_settings");
			String jsonBody = JacksonUtils.toJson(settings);
			request.setJsonEntity(jsonBody);
			
			Response response = restClient.performRequest(request);
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			
			// 解析响应获取 acknowledged 状态
			@SuppressWarnings("unchecked")
			Map<String, Object> responseMap = JacksonUtils.toObject(jsonResponse, Map.class);
			if (responseMap != null && responseMap.containsKey("acknowledged")) {
				Object acknowledged = responseMap.get("acknowledged");
				return Boolean.TRUE.equals(acknowledged);
			}
			
			return false;
		} catch (Exception e) {
			throw new ListIndicesException(e);
		}
	}

	public static ForceMergeResponse forceMerge(RestHighLevelClient client, String index) {
		try {
			ForceMergeRequest request = new ForceMergeRequest(index);
			request.maxNumSegments(1);
			return client.indices().forcemerge(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	public static Map<String, Object> getMapping(RestHighLevelClient client, String index) {
		try {
			GetMappingsRequest request = new GetMappingsRequest().indices(index);
			GetMappingsResponse response = client.indices().getMapping(request, RequestOptions.DEFAULT);
			MappingMetadata mappingMetadata = resolveMappingMetadata(response.mappings(), index);
			if (mappingMetadata != null) {
				return mappingMetadata.sourceAsMap();
			}
			return new HashMap<>(12);
		} catch (IOException e) {
			throw new MappingException(e);
		}
	}

	/**
	 * 使用 ES 8.x Java Client 更新索引的 Mapping
	 *
	 * @param client   ElasticsearchClient
	 * @param index    索引名称
	 * @param mappings Mapping 定义（包含 properties、dynamic 等）
	 * @return boolean 是否更新成功
	 */
	public static boolean putMapping(ElasticsearchClient client, String index, Map<String, Object> mappings) {
		try {
			// 使用底层 RestClient 执行 HTTP 请求，避免 API 兼容性问题
			RestClientTransport transport =
					(RestClientTransport) client._transport();
			RestClient restClient = transport.restClient();
			
			// 执行 HTTP PUT 请求更新 mapping
			Request request = new Request("PUT", "/" + index + "/_mapping");
			String jsonBody = JacksonUtils.toJson(mappings);
			request.setJsonEntity(jsonBody);
			
			Response response = restClient.performRequest(request);
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			
			// 解析响应获取 acknowledged 状态
			@SuppressWarnings("unchecked")
			Map<String, Object> responseMap = JacksonUtils.toObject(jsonResponse, Map.class);
			if (responseMap != null && responseMap.containsKey("acknowledged")) {
				Object acknowledged = responseMap.get("acknowledged");
				return Boolean.TRUE.equals(acknowledged);
			}
			
			return false;
		} catch (Exception e) {
			throw new PutMappingException(e);
		}
	}

	/**
	 * 使用 ES 8.x Java Client 获取索引的 Mapping
	 *
	 * @param client ElasticsearchClient
	 * @param index  索引名称
	 * @return Map<String, Object> Mapping 定义（properties 部分）
	 */
	public static Map<String, Object> getMapping(ElasticsearchClient client, String index) {
		try {
			// 使用底层 RestClient 执行 HTTP 请求
			RestClientTransport transport =
					(RestClientTransport) client._transport();
			RestClient restClient = transport.restClient();
			
			// 执行 HTTP GET 请求获取 mapping
			Request request = new Request(
					"GET", "/" + index + "/_mapping");
			
			Response response = restClient.performRequest(request);
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			
			// 解析 JSON 响应
			@SuppressWarnings("unchecked")
			Map<String, Object> responseMap = JacksonUtils.toObject(jsonResponse, Map.class);
			
			if (responseMap == null || responseMap.isEmpty()) {
				return new HashMap<>(12);
			}
			
			// 提取指定索引的 mapping 信息
			// 响应格式: { "index_name": { "mappings": { ... } } }
			Object indexData = responseMap.get(index);
			if (indexData instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> indexMap = (Map<String, Object>) indexData;
				Object mappings = indexMap.get("mappings");
				if (mappings instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> mappingMap = (Map<String, Object>) mappings;
					return mappingMap;
				}
			}
			
			return new HashMap<>(12);
		} catch (Exception e) {
			throw new MappingException(e);
		}
	}

	public static Map<String, Map<String, Object>> getFieldMapping(RestHighLevelClient client, String index, String... fields) {
		try {
			GetFieldMappingsRequest request = new GetFieldMappingsRequest().indices(index).fields(fields);
			GetFieldMappingsResponse response = client.indices().getFieldMapping(request, RequestOptions.DEFAULT);
			Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> indexMappings =
					response.mappings().get(index);
			if (indexMappings == null) {
				return Map.of();
			}
			// ES 8.x 不再有 type，直接取第一个 mapping
			Map<String, GetFieldMappingsResponse.FieldMappingMetadata> fieldMappings = indexMappings.get(ElasticUtils.ONLY_TYPE);
			if (fieldMappings == null && !indexMappings.isEmpty()) {
				fieldMappings = indexMappings.values().iterator().next();
			}
			if (fieldMappings == null) {
				return Map.of();
			}
			Map<String, Map<String, Object>> fieldMappingMap = new HashMap<>(fieldMappings.size());
			for (GetFieldMappingsResponse.FieldMappingMetadata metadata : fieldMappings.values()) {
				Map<String, ?> source = metadata.sourceAsMap();
				for (Map.Entry<String, ?> entry : source.entrySet()) {
					fieldMappingMap.put(entry.getKey(), (Map<String, Object>) entry.getValue());
				}
			}
			return fieldMappingMap;
		} catch (IOException e) {
			throw new MappingException(e);
		}
	}

	public static ClusterHealthResponse clusterHealth(RestHighLevelClient client) {
		try {
			return client.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new ListIndicesException(e);
		}
	}

	private static MappingMetadata resolveMappingMetadata(
			ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetadata>> mappings, String index) {
		if (mappings == null || mappings.isEmpty()) {
			return null;
		}
		ImmutableOpenMap<String, MappingMetadata> indexMappings = mappings.get(index);
		if (indexMappings == null || indexMappings.isEmpty()) {
			return null;
		}
		// ES 8.x 不再有 type，优先尝试 _doc，否则取第一个
		if (indexMappings.containsKey(ElasticUtils.ONLY_TYPE)) {
			return indexMappings.get(ElasticUtils.ONLY_TYPE);
		}
		Iterator<MappingMetadata> iterator = indexMappings.valuesIt();
		return iterator.hasNext() ? iterator.next() : null;
	}
}
