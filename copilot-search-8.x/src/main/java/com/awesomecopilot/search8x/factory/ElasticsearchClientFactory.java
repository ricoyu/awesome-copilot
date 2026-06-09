package com.awesomecopilot.search8x.factory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.awesomecopilot.common.lang.resource.PropertyReader;
import com.awesomecopilot.json.jackson.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.awesomecopilot.search8x.exception.TransportClientInitException;
import com.awesomecopilot.search8x.support.RestSupport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 创建 Elasticsearch 客户端。
 * <ul>
 * <li>{@link #createQueryClient()} — 查询模块使用 co.elastic.clients elasticsearch-java 8.x</li>
 * <li>{@link #createHighLevelClient()} — 聚合/索引管理等仍使用 RestHighLevelClient（7.x REST 适配）</li>
 * </ul>
 * 两者共享同一 {@link RestClient} 连接池。
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public final class ElasticsearchClientFactory {

	private static final Logger log = LoggerFactory.getLogger(ElasticsearchClientFactory.class);

	private static final String USERNAME = "elastic.username";
	private static final String PASSWORD = "elastic.password";

	private static volatile RestClientBuilder sharedRestClientBuilder;
	private static volatile RestClient sharedRestClient;

	private ElasticsearchClientFactory() {
	}

	/**
	 * 查询模块官方 8.x 客户端。
	 */
	public static ElasticsearchClient createQueryClient() {
		ObjectMapper mapper = ObjectMapperFactory.createOrFromBeanFactory();
		return new ElasticsearchClient(new RestClientTransport(restClient(), new JacksonJsonpMapper(mapper)));
	}

	/**
	 * 聚合等模块暂用的 HLRC（7.9.3 仅接受 RestClientBuilder，与查询侧各持一个连接池）。
	 */
	public static RestHighLevelClient createHighLevelClient() {
		return new RestHighLevelClient(restClientBuilder());
	}

	private static RestClientBuilder restClientBuilder() {
		if (sharedRestClientBuilder != null) {
			return sharedRestClientBuilder;
		}
		synchronized (ElasticsearchClientFactory.class) {
			if (sharedRestClientBuilder != null) {
				return sharedRestClientBuilder;
			}
			sharedRestClientBuilder = createRestClientBuilder();
			return sharedRestClientBuilder;
		}
	}

	private static RestClient restClient() {
		if (sharedRestClient != null) {
			return sharedRestClient;
		}
		synchronized (ElasticsearchClientFactory.class) {
			if (sharedRestClient != null) {
				return sharedRestClient;
			}
			sharedRestClient = restClientBuilder().build();
			return sharedRestClient;
		}
	}

	private static RestClientBuilder createRestClientBuilder() {
		PropertyReader propertyReader = new PropertyReader("elastic");
		String username = propertyReader.getString(USERNAME);
		String password = propertyReader.getString(PASSWORD);
		List<String> hosts = RestSupport.HOSTS;
		if (hosts.isEmpty()) {
			throw new TransportClientInitException("elastic.rest.hosts 未配置");
		}

		HttpHost[] httpHosts = hosts.stream()
				.map(ElasticsearchClientFactory::toHttpHost)
				.toArray(HttpHost[]::new);

		RestClientBuilder builder = RestClient.builder(httpHosts);
		if (username != null && password != null) {
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(username, password));
			builder.setHttpClientConfigCallback(httpClientBuilder ->
					httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
		}

		return builder;
	}

	private static HttpHost toHttpHost(String hostUrl) {
		try {
			java.net.URI uri = java.net.URI.create(hostUrl);
			String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
			int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(scheme) ? 443 : 9200);
			return new HttpHost(uri.getHost(), port, scheme);
		} catch (Exception e) {
			log.error("解析 ES REST 地址失败: {}", hostUrl, e);
			throw new TransportClientInitException("无效的 elastic.rest.hosts: " + hostUrl, e);
		}
	}
}
