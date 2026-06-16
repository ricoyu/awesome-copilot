package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.awesomecopilot.search8x.exception.ElasticQueryException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 通过 elasticsearch-java 8.x 底层 RestClient 执行搜索，并将响应解析为 7.x {@link SearchResponse}，
 * 供现有查询 Builder / {@link SearchHitsSupport} 继续使用。
 */
public final class SearchResponseBridge {

	private SearchResponseBridge() {
	}

	public static RestClient restClient(ElasticsearchClient client) {
		return ((RestClientTransport) client._transport()).restClient();
	}

	public static SearchResponse search(ElasticsearchClient client, String[] indices,
			SearchSourceBuilder sourceBuilder, TimeValue scroll) {
		try {
			Request request = new Request("POST", "/" + indexPath(indices) + "/_search");
			request.addParameter("ignore_unavailable", "true");
			request.addParameter("allow_no_indices", "true");
			if (scroll != null) {
				request.addParameter("scroll", scroll.getStringRep());
			}
			request.setJsonEntity(sourceBuilder.toString());
			return parseSearchResponse(restClient(client).performRequest(request));
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}

	public static SearchResponse scroll(ElasticsearchClient client, String scrollId, TimeValue keepAlive) {
		try {
			JSONObject body = new JSONObject();
			body.put("scroll_id", scrollId);
			if (keepAlive != null) {
				body.put("scroll", keepAlive.getStringRep());
			}
			Request request = new Request("POST", "/_search/scroll");
			request.setJsonEntity(body.toString());
			return parseSearchResponse(restClient(client).performRequest(request));
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}

	public static SearchResponse search(ElasticsearchClient client, String[] indices, SearchSourceBuilder sourceBuilder) {
		return search(client, indices, sourceBuilder, null);
	}

	public static long deleteByQuery(ElasticsearchClient client, String[] indices, String queryInnerJson, boolean refresh) {
		try {
			JSONObject body = new JSONObject();
			body.put("query", new JSONObject(queryInnerJson));
			if (refresh) {
				body.put("refresh", true);
			}
			Request request = new Request("POST", "/" + indexPath(indices) + "/_delete_by_query");
			request.addParameter("ignore_unavailable", "true");
			request.addParameter("allow_no_indices", "true");
			request.setJsonEntity(body.toString());
			Response response = restClient(client).performRequest(request);
			String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
			JSONObject root = new JSONObject(json);
			return root.optLong("deleted", 0L);
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}

	public static SearchResponse searchTemplate(ElasticsearchClient client, String[] indices, String templateId,
			java.util.Map<String, Object> scriptParams) {
		try {
			JSONObject body = new JSONObject();
			body.put("id", templateId);
			if (scriptParams != null && !scriptParams.isEmpty()) {
				body.put("params", new JSONObject(scriptParams));
			}
			Request request = new Request("POST", "/" + indexPath(indices) + "/_search/template");
			request.setJsonEntity(body.toString());
			Response response = restClient(client).performRequest(request);
			String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
			JSONObject root = new JSONObject(json);
			if (root.has("response")) {
				return parseSearchResponseJson(root.getJSONObject("response").toString());
			}
			return parseSearchResponseJson(json);
		} catch (IOException e) {
			throw new ElasticQueryException(e);
		}
	}

	private static String indexPath(String[] indices) {
		return Arrays.stream(indices).collect(Collectors.joining(","));
	}

	/**
	 * 解析 REST Response 为 7.x SearchResponse
	 */
	public static SearchResponse parseSearchResponse(Response response) throws IOException {
		try (InputStream in = response.getEntity().getContent()) {
			return SearchResponse.fromXContent(
					XContentFactory.xContent(XContentType.JSON).createParser(
							NamedXContentRegistry.EMPTY,
							DeprecationHandler.IGNORE_DEPRECATIONS,
							in));
		}
	}

	private static SearchResponse parseSearchResponseJson(String json) throws IOException {
		return SearchResponse.fromXContent(
				XContentFactory.xContent(XContentType.JSON).createParser(
						NamedXContentRegistry.EMPTY,
						DeprecationHandler.IGNORE_DEPRECATIONS,
						json));
	}
}
