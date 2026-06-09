package com.awesomecopilot.search8x.support;

import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.exception.IndexCreateException;
import com.awesomecopilot.search8x.exception.ListIndicesException;
import com.awesomecopilot.search8x.exception.MappingException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
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
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.cluster.metadata.IndexTemplateMetadata;
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

	public static boolean createIndex(RestHighLevelClient client, CreateIndexRequest request) {
		try {
			CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
			return response.isAcknowledged();
		} catch (IOException e) {
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

	public static Map<String, Map<String, Object>> getFieldMapping(RestHighLevelClient client, String index, String... fields) {
		try {
			GetFieldMappingsRequest request = new GetFieldMappingsRequest().indices(index).fields(fields);
			GetFieldMappingsResponse response = client.indices().getFieldMapping(request, RequestOptions.DEFAULT);
			Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> indexMappings =
					response.mappings().get(index);
			if (indexMappings == null) {
				return Map.of();
			}
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
		if (indexMappings.containsKey(ElasticUtils.ONLY_TYPE)) {
			return indexMappings.get(ElasticUtils.ONLY_TYPE);
		}
		Iterator<MappingMetadata> iterator = indexMappings.valuesIt();
		return iterator.hasNext() ? iterator.next() : null;
	}
}
