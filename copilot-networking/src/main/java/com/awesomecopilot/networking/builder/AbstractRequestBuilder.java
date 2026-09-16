package com.awesomecopilot.networking.builder;

import com.awesomecopilot.common.lang.concurrent.Concurrent;
import com.awesomecopilot.common.lang.transformer.Transformers;
import com.awesomecopilot.common.lang.utils.DateUtils;
import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.common.lang.utils.StringUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.networking.constants.HttpHeaders;
import com.awesomecopilot.common.lang.exception.BusinessException;
import com.awesomecopilot.networking.enums.HttpMethod;
import com.awesomecopilot.networking.enums.Scheme;
import com.awesomecopilot.networking.exception.HttpRequestException;
import com.awesomecopilot.networking.utils.ErrorUtils;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 一个完整的URL包含的各部分如下:<p>
 * [protocol:][//host[:port]][path][?query][#fragment]<p>
 *
 * <pre>
 *                                                         | request ----------------------------------------------- |
 *                                                         | path ------------------- |                              |
 *         | authorization | | domain -------------- |     | directory ---- || file - | | query ---------------- |   |
 *         |               | |                       |     |                ||        | |                        |   |
 * https://username:password@www.subdomain.example.com:1234/folder/subfolder/index.html?search=products&sort=false#top
 * |       |        |        |   |         |       |   |   |       |         |     |    |      |        |    |     |
 * |       username |        |   |         |       |   |   folder  folder    |     |    |      value    |    value |
 * protocol         password |   |         |       |   port                  |     |    parameter       parameter  |
 *                           |   |         |       1st-level-domain          |     file-extension                  fragment
 *                           |   |         2nd-level-domain                  filename
 *                           |   3rd-level-domain
 *                           4th-level-domain
 *
 * </pre>
 *
 * <p>
 * Copyright: Copyright (c) 2021-03-22 16:57
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class AbstractRequestBuilder {

	private static final Logger log = LoggerFactory.getLogger(AbstractRequestBuilder.class);

	public static final String DEFAULT_CHARSET = "UTF-8";

	protected static PoolingHttpClientConnectionManager connectionManager;

	protected static SSLConnectionSocketFactory sslConnectionSocketFactory;

	static {
		TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;

		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
		} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
			log.error("初始化SSL上下文失败, 无法建立HTTPS连接", e);
			throw new RuntimeException(e);
		}
		sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslConnectionSocketFactory)
				.register("http", new PlainConnectionSocketFactory())
				.build();

		connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		connectionManager.setMaxTotal(100);// 整个连接池最大连接数
		/*
		 * 设置每一个路由的最大连接数, 这里的路由是指 IP+PORT.
		 * 例如连接池大小(MaxTotal)设置为300, 路由连接数设置为200(DefaultMaxPerRoute),
		 * 对于www.a.com 与 www.b.com 两个路由来说,
		 * 发起服务的主机连接到每个路由的最大连接数(并发数)不能超过200,
		 * 两个路由的总连接数不能超过300。
		 */
		connectionManager.setDefaultMaxPerRoute(20);
	}


	public static final Charset UTF8 = StandardCharsets.UTF_8;

	/**
	 * 完整的URL, 比如 http://192.168.100.101:9200/rico/_mapping
	 */
	protected String url;

	/**
	 * URL的协议部分, 默认 http
	 */
	protected Scheme scheme = Scheme.HTTP;

	/**
	 * URL的端口部分, 默认 80
	 */
	protected int port = 80;

	/**
	 * URL的主机名部分, 如 www.163.com, 192.168.100.101
	 */
	protected String host;

	/**
	 * 请求的path部分, 如: /rico/_mapping<p>
	 * 应该以/开头
	 */
	protected String path;

	/**
	 * http.connection.timeout
	 * <p>
	 * 与远程主机建立连接的超时时间
	 * <p>
	 * 超时会抛出org.apache.http.conn.ConnectTimeoutException
	 */
	protected Long connectionTimeout;

	/**
	 * http.socket.timeout
	 * <p>
	 * 建立连接后, 传输数据的超时时间
	 * <p>
	 * 超时会抛出 java.net.SocketTimeoutException
	 * <p>
	 * The time waiting for data – after establishing the connection; maximum time of inactivity between two data
	 * packets
	 */
	protected Long soTimeout;

	/**
	 * 从连接池中获取连接的超时时间, 在高负载情况下比较有必要设置
	 * <p>
	 * http.connection-manager.timeout
	 * <p>
	 * The time to wait for a connection from the connection manager/pool
	 */
	protected Long connectionManagerTimeout;

	/**
	 * 请求生命周期超时时间, 大致= connectionTimeout + soTimeout
	 */
	protected Long timeout;

	/**
	 * 设置失败重试次数
	 * <p>
	 * 如果发生了以下几种异常, 不会重试
	 * <ul>
	 *     <li/>InterruptedIOException, SocketTimeoutException
	 *     <li/>UnknownHostException
	 *     <li/>ConnectException
	 *     <li/>SSLException
	 * </ul>
	 */
	protected Integer retries;

	/**
	 * 如果接口是幂等的, 可以放心重试, 设为true, 否则设为false
	 * true if it's OK to retry non-idempotent requests that have been sent
	 */
	protected boolean requestSentRetryEnabled = false;

	/**
	 * 返回结果以byte[]形式返回
	 */
	protected boolean returnBytes;

	protected Map<String, Object> headers = new HashMap<>(12);

	protected BasicCookieStore cookieStore = new BasicCookieStore();

	protected Class responseType;

	protected HttpMethod method = HttpMethod.GET;

	protected MultiMap params = new MultiValueMap();

	/**
	 * 用来处理添加多个同名的参数, 后端接口通过一个数组来接收参数
	 */
	protected List<NameValuePair> pairs = new ArrayList<NameValuePair>();

	/**
	 * HTTP请求报错时回调函数
	 */
	protected Consumer<Exception> errorCallback;

	/**
	 * 通过连接池获取HttpClient
	 *
	 * @return CloseableHttpClient
	 */
	protected CloseableHttpClient buildHttpClient() {
		//超时设置
		RequestConfig.Builder builder = RequestConfig.custom();
		if (connectionManagerTimeout != null) {
			builder.setConnectionRequestTimeout(connectionManagerTimeout.intValue());
		}
		if (connectionTimeout != null) {
			builder.setConnectTimeout(connectionTimeout.intValue());
		}
		if (soTimeout != null) {
			builder.setSocketTimeout(soTimeout.intValue());
		}
		HttpClientBuilder httpClientBuilder = HttpClients.custom()
				.setSSLSocketFactory(sslConnectionSocketFactory)
				.setConnectionManager(connectionManager)
				/*
				 * 声明连接管理器(池)不归这个客户端所有。不声明时, HttpClientBuilder 会给客户端注册
				 * 一条"close() 时顺手 shutdown 连接管理器"的动作(4.5.13 源码 HttpClientBuilder.java:1244),
				 * 而本类的 connectionManager 是 static 全进程共享的——request() 里
				 * try-with-resources 关闭客户端时就会把共享池一起关掉, 导致同进程后续所有
				 * HttpUtils 请求抛 "Connection pool shut down" (评审报告 P0-1)。
				 */
				.setConnectionManagerShared(true)
				.setDefaultCookieStore(cookieStore)
				.setDefaultRequestConfig(builder.build());
		/*
		 * 如果设置了重试次数
		 * requestSentRetryEnabled 如果调用的接口是幂等的, 可以设为true, 如果不幂等, 设为false, 避免重复提交
		 */
		if (retries != null) {
			httpClientBuilder.setRetryHandler(new StandardHttpRequestRetryHandler(retries, requestSentRetryEnabled));
		}

		CloseableHttpClient httpClient = httpClientBuilder.build();

		int leased = connectionManager.getTotalStats().getLeased();
		int available = connectionManager.getTotalStats().getAvailable();
		int total = leased + available;

		if (log.isDebugEnabled()) {
			log.debug("HttpClient连接池\n" +
							"最大连接数: {}\n" +
							"已创建的连接数: {}\n" +
							"当前正在执行任务的连接数: {}\n" +
							"当前空闲的连接数: {}\n" +
							"当前等待获取连接的任务数: {}",
					connectionManager.getTotalStats().getMax(),
					total,
					leased,
					available,
					connectionManager.getTotalStats().getPending());
		}

		return httpClient;
	}

	/**
	 * 指定完整的URL, 如: http://192.168.100.101:9200/rico/_mapping<p>
	 * 如果设置了URL, 就不需要设置scheme, host, port, path<p>
	 * 但是请求参数还是可以设置的, 如果URL带参数部分, 又另外设置了参数, 那么取两者合集
	 *
	 * @param url
	 * @return AbstractRequestBuilder
	 */
	public AbstractRequestBuilder url(String url) {
		this.url = url;
		return this;
	}

	/**
	 * 指定 URL的协议部分, 如 http<p>
	 * 如果指定了完整的url就不需要指定scheme, 设置了也无效
	 *
	 * @param scheme
	 * @return AbstractRequestBuilder
	 */
	public AbstractRequestBuilder scheme(Scheme scheme) {
		this.scheme = scheme;
		return this;
	}

	/**
	 * URL的主机名部分, 如 www.163.com, 192.168.100.101<p>
	 * 如果指定了完整的url就不需要指定host, 设置了也无效<p>
	 * 不需要尾部的/
	 *
	 * @param host
	 * @return AbstractRequestBuilder
	 */
	public AbstractRequestBuilder host(String host) {
		/*
		 * 如果主机名部分带/结尾, 那么去掉尾部的/
		 */
		if (host != null && host.lastIndexOf("/") == host.length() - 1) {
			host = host.substring(0, host.length() - 1);
		}
		this.host = host;
		return this;
	}

	/**
	 * 设置端口号<p>
	 * 如果指定了完整的url就不需要指定port, 设置了也无效
	 *
	 * @param port
	 * @return
	 */
	public AbstractRequestBuilder port(int port) {
		this.port = port;
		return this;
	}

	/**
	 * 一个完整URL的path部分, 以/开头<p>
	 * 如http://192.168.100.101:9200/rico/_mapping 的 /rico/_mapping<p>
	 * 如果指定了完整的url就不需要指定path, 设置了也无效
	 *
	 * @param path
	 * @return
	 */
	public AbstractRequestBuilder path(String path) {
		//如果没有以/开头, 自动给他加上/
		if (path != null && path.indexOf("/") != 0) {
			path = '/' + path;
		}
		this.path = path;
		return this;
	}

	/**
	 * 设置 HTTP 请求方法
	 *
	 * @param method
	 * @return AbstractRequestBuilder
	 */
	public AbstractRequestBuilder method(HttpMethod method) {
		this.method = method;
		return this;
	}

	protected AbstractRequestBuilder addHeader(String headerName, Object headerValue) {
		notNull(headerName, "Header name cannot be null");
		notNull(headerValue, "Header value cannot be null");
		headers.put(headerName, headerValue);
		return this;
	}

	/**
	 * 添加请求参数
	 *
	 * @param paramName
	 * @param paramValue
	 * @return AbstractRequestBuilder
	 */
	protected AbstractRequestBuilder addParam(String paramName, Object paramValue) {
		notNull(paramName, "paramName cannot be null!");
		while (paramName.startsWith("&") || paramName.startsWith("?")) {
			paramName = paramName.substring(1);
		}
		if (params.containsKey(paramName)) {
			pairs.add(new BasicNameValuePair(paramName, paramValue.toString()));
		} else {
			params.put(paramName, paramValue);
		}
		return this;
	}

	/**
	 * Basic Authentication
	 * 设置请求头: Authorization, 值为: "Basic XXXX" 形式
	 *
	 * @param username
	 * @param password
	 */
	protected AbstractRequestBuilder basicAuth(String username, String password) {
		notNull(username, "Username must not be null");
		notNull(password, "Password must not be null");

		CharsetEncoder encoder = UTF8.newEncoder();
		if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
			throw new IllegalArgumentException(
					"Username or password contains characters that cannot be encoded to " + UTF8.displayName());
		}

		String credentialsString = username + ":" + password;
		byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(UTF8));
		String encodedCredentials = new String(encodedBytes, UTF8);
		headers.put(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);

		return this;
	}

	/**
	 * Bearer Token Authentication
	 * 设置请求头: Authorization, 值为: "Bearer XXXX" 形式
	 *
	 * @param token
	 */
	protected AbstractRequestBuilder bearerAuth(String token) {
		notNull(token, "token must not be null");
		headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		return this;
	}

	protected AbstractRequestBuilder onError(Consumer<Exception> errorCallback) {
		this.errorCallback = errorCallback;
		return this;
	}

	/**
	 * 添加Cookie
	 *
	 * @param name
	 * @param value
	 * @return CookieBuilder
	 */
	protected abstract AbstractRequestBuilder addCookie(String name, String value);

	protected abstract AbstractRequestBuilder addCookie(String name, String value, String domain, String path);

	/**
	 * 将Map中的参数名/值对转成HTTPClient的NameValuePair
	 *
	 * @param params
	 * @return List<NameValuePair>
	 */
	protected List<NameValuePair> toNameValuePairs(Map<String, Object> params) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			Object raw = entry.getValue();
			String value;
			if (raw instanceof List) {
				/*
				 * 评审报告 P1-2 修复。两种 List 形态都要处理:
				 * ① MultiValueMap 聚合出的 ["java","go"] —— 同名参数逐值收集;
				 * ② 用户把一个 List 整体塞进 MultiValueMap 产生的嵌套形态 [["java","go"]]
				 *    (MultiValueMap.put 不展开传入集合, form.param("tag", List.of(..)) 就是这种)。
				 * 旧代码直接把 List 交给可变参数的 StringUtils.joinWith, 整个 List 被当成一个
				 * 元素调 toString, 服务端收到 "tag=[java, go]"(带方括号)。
				 * 按方法原注释的语义: 多个值用逗号连成一个值, 所以这里逐层摊平后逗号 join。
				 */
				List<String> flattened = new ArrayList<>();
				for (Object item : (List<?>) raw) {
					if (item instanceof List) {
						for (Object inner : (List<?>) item) {
							flattened.add(String.valueOf(inner));
						}
					} else if (item != null) {
						flattened.add(String.valueOf(item));
					}
				}
				value = StringUtils.joinWith(",", flattened.toArray());
			} else {
				value = String.valueOf(raw);
			}
			pairs.add(new BasicNameValuePair(entry.getKey(), value));
		}
		pairs.addAll(this.pairs);

		return pairs;
	}

	/**
	 * 构造HttpRequest对象, 同时设置请求头
	 *
	 * @param builder
	 * @return HttpRequest
	 */
	protected HttpUriRequest buildHttpRequest(URIBuilder builder) {
		URI uri = null;
		try {
			uri = builder.build();
		} catch (URISyntaxException e) {
			log.error("URI构建失败(通常因为URL或参数含非法字符)", e);
			throw new IllegalArgumentException(e);
		}

		HttpRequestBase request;
		switch (method) {
			case GET:
				request = new HttpGet(uri);
				break;
			case POST:
				request = new HttpPost(uri);
				break;
			case PUT:
				request = new HttpPut(uri);
				break;
			case DELETE:
				request = new HttpDelete(uri);
				break;
			case HEAD:
				request = new HttpHead(uri);
				break;
			case OPTIONS:
				request = new HttpOptions(uri);
				break;
			case TRACE:
				request = new HttpTrace(uri);
				break;
			case PATCH:
				//评审报告 P1-1 修复: HttpMethod 枚举里有 PATCH, 原先 switch 漏了分支,
				//request 保持 null 走到 addHeader 时抛 NullPointerException
				request = new HttpPatch(uri);
				break;
			default:
				//method 为 null 或新增枚举值漏配分支时, 立刻抛异常说清原因, 不再让 null 往下漏
				throw new IllegalArgumentException("不支持的HTTP方法: " + method);
		}

		addHeader(request);
		return request;
	}

	/**
	 * 通过HttpClient发送请求
	 *
	 * @return T
	 */
public <T> T request() {
		URIBuilder builder;
		String effectiveHost;
		if (isNotBlank(url)) {
			/*
			 * 完整 URL 交给 JDK 的 URI 解析器（评审报告 P1-4 修复: 原先用自研正则拆解,
			 * IPv6 地址和单位数端口都会解析错位; teardown 解析失败返回 null 后这里还会 NPE）。
			 * 参数处理规则（P0-3/P1-3 修复）: URI 解析出的 query 保持"已编码"原样,
			 * 用 URLEncodedUtils 取出后按原始文本放进 builder——这样既不会把 %20 再编成
			 * %2520（旧实现把已编码值当原始文本重新编码）, 也不会丢掉值里带 = 的参数
			 * （旧实现按 = 硬切两段, 三段以上或无 = 的直接不报错地丢弃）。
			 */
			URI urlUri;
			try {
				urlUri = new URI(url);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("不合法的URL: " + url, e);
			}
			builder = new URIBuilder(urlUri, UTF8);
			// 只把用户显式 addParam 的参数重新编码追加(它们的值本来就是原始文本);
			// URL 自带的参数同名时, URIBuilder 里已编码的原值与显式值并存, 语义见 url() 注释
			if (!params.isEmpty()) {
				builder.addParameters(toNameValuePairs(params));
			}
			effectiveHost = urlUri.getHost();
		} else {
			builder = new URIBuilder();
			builder.setScheme(scheme == null ? null : scheme.name().toLowerCase());
			builder.setHost(host);
			builder.setPort(port);
			builder.setPath(path);
			/*
			 * 分段构建路径没有"URL 自带参数"要合并, params 全按原始文本编码一次
			 */
			List<NameValuePair> pairs = toNameValuePairs(new HashMap<String, Object>(params));
			builder.setParameters(pairs);
			effectiveHost = host;
		}
		// P1-5 配套修复: 给未指定域的 cookie 回填本次请求主机名(见 assignDomainlessCookies)
		assignDomainlessCookies(effectiveHost);

		try {
			/*
			 * 根据请求方法创建HttpGet, HttpPost等对象
			 * 同时设置请求头
			 */
			HttpUriRequest httpRequest = buildHttpRequest(builder);

			/*
			 * 钩子方法, 提供子类去实现。
			 * 判断依据从 "instanceof HttpPost || HttpPut" 放宽为"请求类支持携带实体":
			 * HttpPatch 同样是 HttpEntityEnclosingRequestBase 的子类, 修 P1-1 时必须一起放开,
			 * 否则 method(PATCH) 不再抛 NPE 了, body 却仍然发不出去（评审报告 P1-1 的完整修复）。
			 */
			if (httpRequest instanceof HttpEntityEnclosingRequestBase) {
				//发送JSON数据/表单数据才需要执行
				if (this instanceof JsonRequestBuilder) {
					addBody((HttpEntityEnclosingRequestBase) httpRequest);
				} else if (this instanceof FormRequestBuilder) {
					//表单提交时设置表单数据
					addFormData((HttpEntityEnclosingRequestBase) httpRequest);
				}
			}

			try (CloseableHttpClient httpClient = buildHttpClient()) {
				/*
				 * 如果设置了整个请求生命周期的超时时间, 超时后中断请求
				 */
				if (timeout != null) {
					TimerTask task = new TimerTask() {
						@Override
						public void run() {
							if (httpRequest != null) {
								httpRequest.abort();
							}
						}
					};
					new Timer(true).schedule(task, timeout);
				}

				try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
					/*
					 * 拿response entity之前先检查状态码。实测 405 这类错误 HttpClient 不抛异常,
					 * 只能自己判断。原来这里靠反射读内部类 HttpResponseProxy 的 "original" 字段
					 * 再取 reasonPhrase——proxiedResponse 本身就实现了 getStatusLine()/getEntity(),
					 * 直接调即可, 不需要反射(反射在类结构变化时会静默拿到 null)。
					 */
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode < 200 || statusCode >= 300) {
						// P0-2 修复: checkError 现在对一切非 2xx 抛异常, 2xx 直接放行(含 204/206)
						ErrorUtils.checkError(statusCode, response.getStatusLine().getReasonPhrase());
					}
					HttpEntity entity = response.getEntity();
					if (entity == null) {
						/*
						 * P1-7 修复: 响应没有实体(HEAD/204 等)时, 旧实现不声明任何异常地走到
						 * 方法末尾 return null, 调用方拿到 null 一头雾水。这里保持返回 null
						 * (语义正确: 确实没有内容), 但打一条 info 说明原因, 便于排查。
						 */
						log.info("HTTP响应无内容(entity为空), 状态码: {}, 返回null", statusCode);
						return null;
					}
					//表示结果要以byte[]数组形式返回; 声明 responseType=byte[].class 视同开启
					//(P1-7 修复: 旧实现两者不联动, byte[].class 会走 Jackson 分支抛
					// ClassCastException, 被包成完全看不出根因的 HttpRequestException)
					if (returnBytes || responseType == byte[].class) {
						try (java.io.InputStream in = entity.getContent()) {
							return (T) IOUtils.toByteArray(in);
						}
					}

					String result = EntityUtils.toString(entity, "UTF-8");

					if (responseType != null) {
						if (isBlank(result)) {
							return null;
						}
						if (responseType == String.class) {
							return (T) result;
						}
						return (T) JacksonUtils.toObject(result, responseType);
					}
					return (T) result;
				}
			}
		} catch (BusinessException | HttpRequestException e) {
			/*
			 * P1-6 修复: checkError 抛出的"HTTP语义异常"不再被兜底 catch 包一层
			 * HttpRequestException——调用方 catch (BusinessException) 能按 404/405 分支处理,
			 * 错误码信息不再丢失。errorCallback 也照常收到它。
			 */
			if (errorCallback != null) {
				errorCallback.accept(e);
				return null;
			}
			log.error("HTTP请求返回错误状态", e);
			throw e;
		} catch (Exception e) {
			if (errorCallback != null) {
				errorCallback.accept(e);
				return null;
			} else {
				log.error("HTTP请求执行失败, url: {}", url != null ? url : (scheme + "://" + host + ":" + port + path), e);
				throw new HttpRequestException(e);
			}
		}
	}

	/**
	 * 异步执行HTTP请求, 拿到结果后回调callback
	 *
	 * @param callback
	 */
	public void request(Consumer<Object> callback) {
		Objects.requireNonNull(callback, "callbacl cannot be null!");
		Concurrent.execute(() -> {
			Object result = this.request();
			callback.accept(result);
		});
	}

	/**
	 * 为HTTP POST请求添加请求体
	 *
	 * @param request
	 */
	protected void addBody(HttpEntityEnclosingRequestBase request) {
	}

	/**
	 * 表单提交时设置表单对象
	 *
	 * @param request
	 */
	protected void addFormData(HttpEntityEnclosingRequestBase request) {
	}

	/**
	 * 把 addCookie 时未指定域名的 cookie 归属到本次请求的主机（评审报告 P1-5 的配套修复）。
	 * <p>
	 * 背景：RFC6265 cookie 规范（HttpClient 4.x 默认实现）拒收"既没有 Domain 属性也没有
	 * HostClientCookie 标记"的手工 cookie——旧代码靠写死 domain="sexy-uncle.com" 让它过了
	 * 检查，代价是换个主机就查不到域不匹配不发。删掉硬编码后必须补上这一步：发请求前把
	 * 请求主机回填给所有 domain 为空的 cookie，效果 = 浏览器对 Set-Cookie 不带 Domain 时的
	 * host-only 归属。
	 */
	private void assignDomainlessCookies(String requestHost) {
		if (requestHost == null) {
			return;
		}
		for (org.apache.http.impl.cookie.BasicClientCookie cookie :
				cookieStore.getCookies().stream()
						.filter(c -> c instanceof org.apache.http.impl.cookie.BasicClientCookie)
						.map(c -> (org.apache.http.impl.cookie.BasicClientCookie) c)
						.collect(java.util.stream.Collectors.toList())) {
			if (cookie.getDomain() == null) {
				cookie.setDomain(requestHost);
				// 标记域属性是显式设定的, RFC6265 规范下才不会被拒收
				cookie.setAttribute(org.apache.http.cookie.ClientCookie.DOMAIN_ATTR, "true");
			}
		}
	}

	private void addHeader(HttpRequestBase request) {
		for (Entry<String, Object> entry : headers.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Date) {
				request.addHeader(entry.getKey(), DateUtils.formatToRfc((Date) value));
				continue;
			}
			if (value instanceof LocalDateTime) {
				request.addHeader(entry.getKey(), DateUtils.formatToRfc((LocalDateTime) value));
				continue;
			}
			request.addHeader(entry.getKey(), Transformers.convert(value, String.class));
		}
	}

}
