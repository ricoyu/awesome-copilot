package com.awesomecopilot.search8x.factory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.awesomecopilot.common.lang.resource.PropertyReader;
import com.awesomecopilot.json.jackson.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.awesomecopilot.search8x.exception.TransportClientInitException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 创建 Elasticsearch 客户端。
 * <ul>
 * <li>{@link #createQueryClient()} — 查询模块使用 co.elastic.clients elasticsearch-java 8.x</li>
 * <li>{@link #createHighLevelClient()} — 聚合/索引管理等仍使用 RestHighLevelClient（7.17.x REST 适配）</li>
 * </ul>
 * 两者共享同一 {@link RestClient} 连接池（7.17.23 版本）。
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public final class ElasticsearchClientFactory {

	private static final Logger log = LoggerFactory.getLogger(ElasticsearchClientFactory.class);

	private static final String CONFIG_PREFIX = "elastic";
	private static final String REST_HOSTS_KEY = CONFIG_PREFIX + ".rest.hosts";
	private static final String USERNAME_KEY = CONFIG_PREFIX + ".username";
	private static final String PASSWORD_KEY = CONFIG_PREFIX + ".password";

	private static volatile ElasticsearchClient sharedQueryClient;
	private static volatile RestClient sharedRestClient;

	private ElasticsearchClientFactory() {
	}

	/**
	 * 查询模块官方 8.x 客户端(单例模式)。
	 * <p>
	 * 配置加载优先级:
	 * <ol>
	 * <li>Spring Environment (支持 Nacos 配置中心)</li>
	 * <li>系统属性 (-Delastic.rest.hosts=...)</li>
	 * <li>环境变量 (ELASTIC_REST_HOSTS)</li>
	 * <li>classpath:elastic.properties (向后兼容)</li>
	 * </ol>
	 *
	 * @return ElasticsearchClient
	 */
	public static ElasticsearchClient createQueryClient() {
		if (sharedQueryClient == null) {
			synchronized (ElasticsearchClientFactory.class) {
				if (sharedQueryClient == null) {
					sharedQueryClient = doCreateQueryClient();
				}
			}
		}
		return sharedQueryClient;
	}

	private static ElasticsearchClient doCreateQueryClient() {
		try {
			// 1. 读取配置
			String hostsStr = getConfigValue(REST_HOSTS_KEY, "http://localhost:9200");
			String username = getConfigValue(USERNAME_KEY, "");
			String password = getConfigValue(PASSWORD_KEY, "");

			log.info("初始化 ES 8.x 客户端, hosts={}, username={}", hostsStr, username);

			// 2. 解析 HTTP Hosts
			List<HttpHost> httpHosts = parseHosts(hostsStr);

			// 3. 创建 RestClient
			RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]));

			// 4. 配置认证(如果需要)
			if (username != null && !username.trim().isEmpty()) {
				final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY,
						new UsernamePasswordCredentials(username, password));

				builder.setHttpClientConfigCallback(httpClientBuilder ->
						httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
				);
			}

			sharedRestClient = builder.build();

			// 5. 创建 ES 8.x Transport
			ObjectMapper mapper = ObjectMapperFactory.createOrFromBeanFactory();
			RestClientTransport transport = new RestClientTransport(
					sharedRestClient,
					new JacksonJsonpMapper(mapper)
			);

			// 6. 创建 ElasticsearchClient
			ElasticsearchClient client = new ElasticsearchClient(transport);

			log.info("ES 8.x 客户端初始化成功");
			return client;

		} catch (Exception e) {
			log.error("ES 8.x 客户端初始化失败", e);
			throw new TransportClientInitException("Failed to initialize ES 8.x client", e);
		}
	}

	/**
	 * 聚合等模块暂用的 HLRC（7.17.x REST 适配）。
	 * TODO: Migrate to ES 8.x Java Client API
	 */
	public static Object createHighLevelClient() {
		throw new UnsupportedOperationException("ES 7.x RestHighLevelClient has been removed. Please migrate to ES 8.x Java Client API.");
	}

	/**
	 * 获取配置值,按优先级读取:
	 * 1. Spring Environment (支持 Nacos)
	 * 2. 系统属性
	 * 3. 环境变量(将.转换为_并转大写)
	 * 4. 本地配置文件
	 */
	private static String getConfigValue(String key, String defaultValue) {
		// 1. 尝试 Spring Environment (如果存在)
		String value = getFromSpringEnvironment(key);
		if (value != null && !value.trim().isEmpty()) {
			log.debug("从 Spring Environment 读取配置: {}={}", key, value);
			return value;
		}

		// 2. 系统属性
		value = System.getProperty(key);
		if (value != null && !value.trim().isEmpty()) {
			log.debug("从系统属性读取配置: {}={}", key, value);
			return value;
		}

		// 3. 环境变量 (elastic.rest.hosts -> ELASTIC_REST_HOSTS)
		String envKey = key.toUpperCase().replace('.', '_');
		value = System.getenv(envKey);
		if (value != null && !value.trim().isEmpty()) {
			log.debug("从环境变量读取配置: {}={}", envKey, value);
			return value;
		}

		// 4. 本地配置文件 (classpath:elastic.properties)
		// PropertyReader 期望完整的资源名称(不带 .properties 后缀)
		// getString() 期望完整的属性名,如 "elastic.rest.hosts"
		PropertyReader reader = new PropertyReader(CONFIG_PREFIX);
		value = reader.getString(key);  // 直接使用完整的 key
		if (value != null && !value.trim().isEmpty()) {
			log.debug("从本地配置文件读取配置: {}={}", key, value);
			return value;
		}

		log.debug("使用默认值: {}={}", key, defaultValue);
		return defaultValue;
	}

	/**
	 * 尝试从 Spring Environment 读取配置(如果 Spring 存在)
	 * 支持 Nacos 配置中心
	 * 
	 * 注意: 由于这是静态工厂类,不在 Spring 容器中,无法直接注入配置。
	 * 推荐使用以下方式之一:
	 * 1. 在 Spring Boot 应用中创建配置类,通过 @PostConstruct 调用 initClient(config)
	 * 2. 通过环境变量或系统属性传递配置
	 * 3. 使用 Spring Cloud Bootstrap 阶段的监听器
	 */
	private static String getFromSpringEnvironment(String key) {
		try {
			// 检测 Spring 是否存在
			Class.forName("org.springframework.core.env.Environment");
			log.debug("检测到 Spring 环境");
			
			// 方案1: 尝试从已注册的 Spring Context 获取(需要应用主动注册)
			// 这是最可靠的方式,但需要应用在启动时调用 registerContext()
			if (SPRING_CONTEXT != null) {
				try {
					java.lang.reflect.Method getPropertyMethod = SPRING_CONTEXT.getClass().getMethod("getProperty", String.class);
					String value = (String) getPropertyMethod.invoke(SPRING_CONTEXT, key);
					if (value != null && !value.trim().isEmpty()) {
						log.info("从 Spring Environment 读取配置成功: {}={}", key, value);
						return value;
					}
				} catch (Exception e) {
					log.warn("从 Spring Context 读取配置失败: {}", e.getMessage());
				}
			}
			
			// 方案2: 尝试通过 Spring Boot 的 Binder API 读取配置
			// 这需要 Spring Boot 2.x+ 且 Environment 已初始化
			try {
				Class<?> binderClass = Class.forName("org.springframework.boot.context.properties.bind.Binder");
				Class<?> environmentClass = Class.forName("org.springframework.core.env.Environment");
				
				// 如果能加载 Binder,说明 Spring Boot 存在
				// 但这里我们无法直接获取 Environment 实例,需要应用层提供
				log.debug("检测到 Spring Boot 环境,建议通过 @ConfigurationProperties 注入配置");
			} catch (ClassNotFoundException e) {
				// Spring Boot 不存在,只有 Spring Framework
			}
			
		} catch (ClassNotFoundException e) {
			// Spring 不存在,返回 null
			log.trace("Spring 环境未检测到");
		}
		
		return null;
	}
	
	/**
	 * 注册 Spring ApplicationContext,使工厂能够从 Spring Environment 读取配置
	 * 
	 * 在 Spring Boot 应用中,创建如下配置类:
	 * <pre>{@code
	 * @Configuration
	 * public class ElasticSearchConfig {
	 *     @Autowired
	 *     private ConfigurableApplicationContext context;
	 *     
	 *     @PostConstruct
	 *     public void init() {
	 *         ElasticsearchClientFactory.registerContext(context);
	 *     }
	 * }
	 * }</pre>
	 * 
	 * @param applicationContext Spring 应用上下文
	 */
	public static void registerContext(Object applicationContext) {
		SPRING_CONTEXT = applicationContext;
		log.info("已注册 Spring ApplicationContext,将优先从 Spring Environment 读取配置");
	}
	
	// Spring ApplicationContext 引用(可选)
	private static volatile Object SPRING_CONTEXT = null;

	/**
	 * 解析多个 ES 主机地址
	 * 支持格式: http://host1:9200,http://host2:9200
	 */
	private static List<HttpHost> parseHosts(String hostsStr) {
		if (hostsStr == null || hostsStr.trim().isEmpty()) {
			throw new TransportClientInitException("elastic.rest.hosts 不能为空");
		}

		return Arrays.stream(hostsStr.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(ElasticsearchClientFactory::toHttpHost)
				.collect(Collectors.toList());
	}

	/**
	 * 将字符串转换为 HttpHost
	 * 支持以下格式:
	 * - http://host:9200 (完整URL)
	 * - host:9200 (主机:端口)
	 * - host (默认端口9200)
	 */
	private static HttpHost toHttpHost(String hostUrl) {
		try {
			// 如果没有协议前缀,添加 http://
			String normalizedUrl = hostUrl.trim();
			if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
				normalizedUrl = "http://" + normalizedUrl;
			}
			
			java.net.URI uri = java.net.URI.create(normalizedUrl);
			String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
			int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(scheme) ? 443 : 9200);
			return new HttpHost(uri.getHost(), port, scheme);
		} catch (Exception e) {
			log.error("解析 ES REST 地址失败: {}", hostUrl, e);
			throw new TransportClientInitException("无效的 elastic.rest.hosts: " + hostUrl, e);
		}
	}
}
