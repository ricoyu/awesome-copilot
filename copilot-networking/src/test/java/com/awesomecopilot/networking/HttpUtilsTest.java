package com.awesomecopilot.networking;

import com.awesomecopilot.common.lang.concurrent.CopilotExecutors;
import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import com.awesomecopilot.networking.enums.HttpMethod;
import com.awesomecopilot.networking.enums.Scheme;
import com.awesomecopilot.networking.exception.HttpRequestException;
import com.awesomecopilot.networking.utils.HttpUtils;
import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>
 * Copyright: (C), 2020/4/22 16:52
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class HttpUtilsTest {
	
	private static final Logger log = LoggerFactory.getLogger(HttpUtilsTest.class);
	
	@Test
	public void testGetModels() {
		String modelJson = HttpUtils.get("https://aiberm.com/v1/models")
				.contentType("application/json")
				.bearerAuth("sk-3wTuRBeuOK7X5aIyDFNYOVAZrQuJfOuvOmkTt6qFmO04tLvn")
				.connectionTimeout(3, SECONDS)
				.soTimeout(200, MILLISECONDS)
				.request();
		
		List<String> models = JsonPathUtils.readListNode(modelJson, "$.data[*].id");
		models.forEach(System.out::println);
	}
	
	@Test
	public void testGet() {
		String url = "http://192.168.100.101:9200/rico/_mapping";
		String response = HttpUtils.get(url).request();
		System.out.println(response);
		
		String response2 = HttpUtils.get()
				.scheme(Scheme.HTTP)
				.host("192.168.100.101")
				.port(9200)
				.path("/rico/_mapping")
				.request();
		
		assertEquals(response, response2);
	}
	
	@Test
	public void testGetWithHeadersAndParams() {
		String response = HttpUtils.get("http://localhost:8080/hello-boot/hello?name=三少爷")
				.addParam("name", "俞雪华")
				.addParam("age", "25")
				.addHeader("origin", "https://www.baeldung.com")
				.addHeader("date", LocalDateTime.now())
				.basicAuth("ricoyu", "123456")
				.request();
		System.out.println(response);
	}
	
	@Test
	public void testPost() {
		String response = HttpUtils.post("http://localhost:8080/hello-boot/body?name=三少爷")
				.addParam("name", "俞雪华")
				.addParam("age", "25")
				.addHeader("origin", "https://www.baeldung.com")
				.addHeader("date", LocalDateTime.now())
				.body("This is a message from 三少爷: 叼~")
				.basicAuth("ricoyu", "123456")
				.request();
		System.out.println(response);
	}
	
	@Test
	public void testElasticAuth() {
		String response = HttpUtils.get("http://192.168.100.104:9200").request();
		assertThat(response).contains("401");
		System.out.println(response);
		
		response = HttpUtils.get("http://192.168.100.104:9200")
				.basicAuth("elastic", "123456")
				.request();
		assertThat(response).contains("version");
		System.out.println(response);
	}
	
	@Test
	public void testFormSubmit() {
		Object response = HttpUtils.form("http://localhost:8080/form-submit")
				.formData("switcher", true)
				.request();
		System.out.println(response);
	}
	
	@Test
	public void testFileUpload() {
		Object response = HttpUtils.form("http://localhost:8080/upload")
				.file("file", Paths.get("D:\\Software\\redis-5.0.8.tar.gz").toFile())
				.formData("name", "俞雪华")
				.formData("switcher", true)
				.request();
		System.out.println(response);
	}
	
	@Test
	public void testFormSubmit2() {
		String json = HttpUtils.get("http://10.10.26.22:8090/token/get").request();
		String token = JsonPathUtils.readNode(json, "token");
		Object result = HttpUtils.form("http://10.10.26.22:8090/tasks/create/file")
				.bearerAuth(token)
				.file("file", IOUtils.readFile("D://ThreatEventViewServiceImpl.class"))
				//.param("priority", 3)
				.formData("priority", 3)
				.request();
		System.out.println(result);
	}
	
	@Test
	public void testSwitch() {
		String json = HttpUtils.get("http://10.10.26.22:8090/token/get").request();
		String token = JsonPathUtils.readNode(json, "token");
		Object result = HttpUtils.form("http://10.10.26.22:8090/av/set/switch")
				.bearerAuth(token)
				.formData("switch", true)
				.request();
		System.out.println(result);
	}
	
	@Test
	public void testBasicAuth() {
		Object response = HttpUtils.get("http://localhost:8080/security")
				.request();
		System.out.println(response);
		response = HttpUtils.get("http://localhost:8080/security")
				.basicAuth("rico", "654321")
				.request();
		System.out.println(response);
	}
	
	@Test
	public void testSSLAcceptAll() {
		TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
		try {
			SSLContext sslContext = SSLContexts.custom()
					.loadTrustMaterial(null, acceptingTrustStrategy)
					.build();
			
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
			
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("https", sslsf)
					.register("http", new PlainConnectionSocketFactory())
					.build();
			
			BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);
			CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLSocketFactory(sslsf)
					.setConnectionManager(connectionManager)
					.build();
			
			HttpEntity entity = httpClient.execute(new HttpGet("https://192.168.100.101:9200/_cat/nodeattrs?v")).getEntity();
			String response = EntityUtils.toString(entity);
			System.out.println(response);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	@Test
	public void testNodeAttr() {
		Object response = HttpUtils.get("https://192.168.100.101:9200/_cat/nodeattrs?v").request();
		System.out.println(JacksonUtils.toPrettyJson(response));
	}
	
	
	@Test
	public void testPostBody() {
		String response = HttpUtils.post("http://localhost:8081/body?name=三少爷")
				.addParam("name", "俞雪华")
				.addParam("age", "25")
				.addHeader("origin", "https://www.baeldung.com")
				.addHeader("date", LocalDateTime.now())
				.body("This is a message from 三少爷: 叼~")
				.basicAuth("ricoyu", "123456")
				.request();
		System.out.println(response);
	}
	
	@Test
	public void testAuth() {
		String responseJson = HttpUtils.get("http://localhost:8083/pic-code").request();
		String codeId = JsonPathUtils.readNode(responseJson, "$.data.codeId");
		
	}
	
	@Test
	public void testSetLocale() {
		HttpUtils.form("http://localhost:8080/login")
				.addCookie("lang", "en")
				.request();
	}
	
	@SneakyThrows
	@Test
	public void testGetOctetStream() {
		byte[] data = HttpUtils.get("http://localhost:8080/downloadFile")
				.bearerAuth("XwGnyZ5TWY1d-3jToBhTkA")
				.returnBytes(true)
				.request();
		System.out.println(data.length);
		IOUtils.write("D://a.zip", data);
	}
	
	@Test
	public void testTimeout() {
		byte[] data = null;
		try {
			data = HttpUtils.get("http://localhost:8080/downloadFile")
					.bearerAuth("XwGnyZ5TWY1d-3jToBhTkA")
					.returnBytes(true)
					.connectionManagerTimeout(5, SECONDS)
					.connectionTimeout(1, SECONDS)
					.soTimeout(2, SECONDS)
					.request();
		} catch (Exception e) {
			if (e instanceof ConnectTimeoutException) {
				log.error("连接超时", e);
			} else if (e instanceof SocketTimeoutException) {
				log.error("数据传输超时", e);
			}
		}
		
		if (data != null) {
			System.out.println(data.length);
			IOUtils.write("D://a.zip", data);
		}
		
		data = HttpUtils.get("http://localhost:8080/downloadFile")
				.bearerAuth("XwGnyZ5TWY1d-3jToBhTkA")
				.returnBytes(true)
				.request();
		
		
		if (data != null) {
			System.out.println(data.length);
			IOUtils.write("D://b.zip", data);
		}
	}
	
	@Test
	public void testRequestTimeout() {
		byte[] data = null;
		try {
			data = HttpUtils.get("http://localhost:8080/downloadFile")
					.bearerAuth("XwGnyZ5TWY1d-3jToBhTkA")
					.returnBytes(true)
					.connectionManagerTimeout(5, SECONDS)
					.connectionTimeout(6, SECONDS)
					.soTimeout(7, SECONDS)
					.timeout(1, SECONDS)
					.request();
		} catch (HttpRequestException e) {
			if (e.getCause() instanceof ConnectTimeoutException) {
				log.error("连接超时", e);
			} else if (e.getCause() instanceof SocketTimeoutException) {
				log.error("数据传输超时", e);
			} else if (e.getCause() instanceof SocketException) {
				log.error("请求超时被取消了", e);
			}
		}
		
		if (data != null) {
			System.out.println(data.length);
			IOUtils.write("D://a.zip", data);
		}
		
		data = HttpUtils.get("http://localhost:8080/downloadFile")
				.bearerAuth("XwGnyZ5TWY1d-3jToBhTkA")
				.returnBytes(true)
				.request();
		
		
		if (data != null) {
			System.out.println(data.length);
			IOUtils.write("D://b.zip", data);
		}
	}
	
	
	@Test
	public void testRequestTimeoutWithCallback() {
		byte[] data = null;
		data = HttpUtils.get("http://localhost:8080/downloadFile")
				.bearerAuth("XwGnyZ5TWY1d-3jToBhTkA")
				.returnBytes(true)
				.connectionManagerTimeout(5, SECONDS)
				.connectionTimeout(6, SECONDS)
				.soTimeout(7, SECONDS)
				.timeout(1, SECONDS)
				.onError((e) -> log.error("出错了, 调用回调函数", e))
				.request();
		
		
		if (data != null) {
			System.out.println(data.length);
			IOUtils.write("D://a.zip", data);
		}
		
		data = HttpUtils.get("http://localhost:8080/downloadFile")
				.bearerAuth("XwGnyZ5TWY1d-3jToBhTkA")
				.returnBytes(true)
				.request();
		
		
		if (data != null) {
			System.out.println(data.length);
			IOUtils.write("D://b.zip", data);
		}
	}
	
	@Test
	public void testRetry() {
		byte[] data = null;
		data = HttpUtils.get("http://localhost:8080/downloadFile")
				.bearerAuth("XwGnyZ5TWY1d-3jToBhTkA")
				.returnBytes(true)
				.connectionManagerTimeout(5, SECONDS)
				.connectionTimeout(6, SECONDS)
				.soTimeout(1, SECONDS)
				.onError((e) -> log.error("出错了, 调用回调函数"))
				.retries(6)
				.requestSentRetryEnabled(false)
				.request();
	}
	
	@Test
	public void testHttpsPost() {
		HttpUtils.post("https://10.10.17.31/api/login").request();
	}
	
	@Test
	public void testRibbonRule() {
		for (int i = 0; i < 900; i++) {
			HttpUtils.get("http://localhost:8080/account-port").request();
			log.info("完成调用第【{}】次", i);
			if (i % 100 == 0) {
				HttpUtils.get("http://localhost:8080/account-statistic").request();
				log.info("调用一次统计接口");
			}
		}
		HttpUtils.get("http://localhost:8080/account-statistic").request();
		log.info("调用统计接口完成!");
	}
	
	@SneakyThrows
	@Test
	public void testHelloSentinel() {
		CountDownLatch countDownLatch = new CountDownLatch(100);
		ExecutorService pool = CopilotExecutors.of("sentinel-pool")
				.corePoolSize(8)
				.maxPoolSize(12)
				.prestartAllCoreThreads()
				.build();
		for (int i = 0; i < 100; i++) {
			pool.execute(() -> {
				Object result = HttpUtils.get("http://localhost:8081/hello-sentinel").request();
				System.out.println(result);
				countDownLatch.countDown();
			});
		}
		countDownLatch.await();
		System.out.println("执行完毕!");
	}
	
	@Test
	public void testcallback() throws InterruptedException {
		HttpUtils.get("http://localhost:8080/hello-boot/hello").request((result) -> {
			log.info("拿到结果: {}", result);
		});
		log.info("调用结束");
		SECONDS.sleep(5);
	}

	@Test
	public void testUrlContainerChineseCharacters() {
		byte[] bytes = HttpUtils.get("http://localhost:8070/file/ceph/download/f5e1b1d542f8_qq图片20160718100555.jpg")
				.returnBytes(true)
				.request();

		IOUtils.write("D://a.jpg", bytes);
	}
	
	// =====================================================================================
	// 公网 API 冒烟测试（2026-09-16 新增）
	// 用 httpbingo.org(httpbin 的官方后继, 把请求内容原样回显成 JSON, 适合验证"我们到底
	// 发出去了什么") 和 badssl.com(专设的证书问题站点) 验证 HttpUtils 门面 API 端到端可用。
	// 与上面依赖 localhost/内网的用例不同, 这一组不依赖本机服务, CI 有外网即可跑。
	// 跨境网络有波动: 所有用例放宽 connectionTimeout=10s / soTimeout=20s, 降低误报概率。
	// =====================================================================================
	
	/** GET 回显: 查询参数(含中文)、自定义请求头是否按原样送达, 并用 responseType(Map) 验证 JSON 反序列化 */
	@Test
	public void testPublicApi_getEchoesParamsAndHeaders() {
		String response = HttpUtils.get("https://httpbingo.org/get?name=三少爷&age=25")
				.addHeader("X-Custom-Test", "hello-copilot")
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		// 中文参数值经 URL 编码送达、服务端解码后回显——两头编码约定一致才能看到原字
		assertThat(response).contains("三少爷").contains("25").contains("hello-copilot");
		
		// 同一端点用 responseType 反序列化成 Map, 验证 JacksonUtils 链路
		java.util.Map<?, ?> map = HttpUtils.get("https://httpbingo.org/get?q=1")
				.responseType(java.util.Map.class)
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		assertThat(map.containsKey("args")).isTrue(); // Map<?,?> 通配类型下 AssertJ containsKey(String) 编译不过, 断言布尔值
	}

	/** 表单提交(含中文值)：form() 的 urlencoded + UTF-8 编码是否被服务端正确解出 */
	@Test
	public void testPublicApi_formPostWithChineseField() {
		String response = HttpUtils.form("https://httpbingo.org/post")
				.formData("username", "rico")
				.formData("remark", "中文表单值")
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		// httpbingo 会把表单字段解码后放进 form 节点回显
		assertThat(response).contains("username").contains("rico").contains("中文表单值");
	}

	/** POST/PUT/PATCH 三种方法携带 JSON 请求体, 服务端回显的 method 与 data 都要对 */
	@Test
	public void testPublicApi_threeMethodsCarryJsonBody() {
		for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH}) {
			// post(url) 起步再改 method: 三种都走带 body 的通路(HttpPatch 是 P1-1 修复才支持的)
			String response = HttpUtils.post("https://httpbingo.org/" + method.name().toLowerCase())
					.method(method)
					.contentType("application/json")
					.body("{\"hello\":\"copilot\"}")
					.connectionTimeout(10, SECONDS)
					.soTimeout(20, SECONDS)
					.request();
			assertThat(response)
					.as("%s 请求的响应应回显方法与请求体", method)
					.contains("\"" + method.name() + "\"")
					.contains("copilot");
		}
	}

	/**
	 * DELETE 用例(2026-09-16 公网实测确认的当前行为)：方法本身送达无误, 但请求体拿不到——
	 * HttpClient 4.x 的 HttpDelete 不继承"可携带实体"的请求基类, 框架的 addBody 钩子
	 * 也只在实体类请求上触发, 于是 body 被不报错地丢弃(服务端回显 data 为空)。
	 * 这正是评审报告 P2-5 提到的问题：想按 ES _search 惯例用 DELETE 携带请求体的用户会遇到 body 丢失。
	 * 本用例把现状固化：将来若给 DELETE 加实体支持(如自定义 HttpDeleteWithBody),
	 * 这里对 data 为空的断言会失败, 提醒同步更新。
	 */
	@Test
	public void testPublicApi_deleteMethodArrivesButBodyIsSilentlyDropped() {
		String response = HttpUtils.delete("https://httpbingo.org/delete")
				.body("{\"hello\":\"copilot\"}")
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		assertThat(response).contains("\"method\": \"DELETE\"");
		assertThat(response).contains("\"data\": \"\""); // 请求体丢失被固化断言, 见注释
	}

	/** Basic 认证：密码正确 200 且 authenticated=true; 密码错误 401 走 P0-2 的异常路径 */
	@Test
	public void testPublicApi_basicAuthRightAndWrong() {
		String ok = HttpUtils.get("https://httpbingo.org/basic-auth/user/passwd")
				.basicAuth("user", "passwd")
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		assertThat(ok).contains("\"authenticated\": true");
		
		// 错误密码: 服务端回 401。ErrorUtils(P0-2 修复后)对非2xx抛异常, 401 没有专属
		// ErrorTypes 枚举所以是带状态码的 HttpRequestException, 断言消息里能看到 401
		Throwable wrong = null;
		try {
			HttpUtils.get("https://httpbingo.org/basic-auth/user/passwd")
					.basicAuth("user", "wrong-password")
					.connectionTimeout(10, SECONDS)
					.soTimeout(20, SECONDS)
					.request();
		} catch (Throwable t) {
			wrong = t;
		}
		assertThat(wrong).isNotNull();
		StringBuilder chain = new StringBuilder();
		for (Throwable c = wrong; c != null; c = c.getCause()) {
			chain.append(c.getMessage()).append(' ');
		}
		assertThat(chain.toString()).contains("401");
	}

	/** 下载二进制：returnBytes(true) 与 responseType(byte[].class) 两种写法都拿到准确长度 */
	@Test
	public void testPublicApi_downloadBytesBothModes() {
		byte[] viaFlag = HttpUtils.get("https://httpbingo.org/bytes/128")
				.returnBytes(true)
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		assertThat(viaFlag).hasSize(128);
		
		// P1-7 修复的组合公网版: 只声明 responseType(byte[].class) 也应直接得 byte[]
		byte[] viaType = HttpUtils.get("https://httpbingo.org/bytes/64")
				.responseType(byte[].class)
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		assertThat(viaType).hasSize(64);
	}

	/**
	 * 生命周期超时：/delay/2 至少挂 2 秒(实测跨境 6 秒+), soTimeout 设 1 秒必然失败;
	 * 失败之后紧跟一次正常请求验证连接池与后续流量不受这次超时影响(P0-1/P2-3 公网版)。
	 */
	@Test
	public void testPublicApi_soTimeoutFailsThenNextRequestHealthy() {
		Throwable t = null;
		try {
			HttpUtils.get("https://httpbingo.org/delay/2")
					.connectionTimeout(10, SECONDS)
					.soTimeout(1, SECONDS)
					.request();
		} catch (Throwable e) {
			t = e;
		}
		assertThat(t).isNotNull(); // 1秒内拿不到响应, 读取超时被包成 HttpRequestException
		
		// 超时不该伤及池子: 紧接着的普通请求照常成功
		String healthy = HttpUtils.get("https://httpbingo.org/get?after=timeout")
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		assertThat(healthy).contains("after");
	}

	/**
	 * HTTPS 证书策略（P2-1 公网真实证书版, badssl.com 专测各种证书毛病）：
	 * ① expired.badssl.com 的证书早已过期——默认必须握手失败(修复前"信任一切"会成功);
	 * ② 显式 .trustAllCerts(true) 才放行(内网自签环境的逃生门, 走独立的信任所有池)。
	 */
	@Test
	public void testPublicApi_expiredCertRejectedUnlessTrustAll() {
		Throwable rejected = null;
		try {
			HttpUtils.get("https://expired.badssl.com/")
					.connectionTimeout(10, SECONDS)
					.soTimeout(20, SECONDS)
					.request();
		} catch (Throwable e) {
			rejected = e;
		}
		assertThat(rejected).isNotNull();
		StringBuilder chain = new StringBuilder();
		for (Throwable c = rejected; c != null; c = c.getCause()) {
			chain.append(c.getClass().getName()).append(':').append(c.getMessage()).append(' ');
		}
		assertThat(chain.toString())
				.as("默认路径应因证书过期在SSL握手阶段失败")
				.containsPattern("SSL|certif|PKIX|证书");
		
		String passed = HttpUtils.get("https://expired.badssl.com/")
				.trustAllCerts(true)
				.connectionTimeout(10, SECONDS)
				.soTimeout(20, SECONDS)
				.request();
		assertThat(passed).isNotEmpty();
	}
	
	/**
	 * 连续 10 次公网 GET：每一次都必须成功。P0-1(共享池被第一次请求关闭)如果回退,
	 * 这个用例会从第 2 次开始全红——公网版哨兵。
	 */
	@Test
	public void testPublicApi_tenSequentialRequestsAllSucceed() {
		for (int i = 1; i <= 10; i++) {
			String r = HttpUtils.get("https://httpbingo.org/get?seq=" + i)
					.connectionTimeout(10, SECONDS)
					.soTimeout(20, SECONDS)
					.request();
			assertThat(r).as("第 %d 次请求", i).contains("seq");
		}
	}
}