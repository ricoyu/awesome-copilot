package com.awesomecopilot.networking;

import com.awesomecopilot.networking.builder.JsonRequestBuilder;
import com.awesomecopilot.networking.constants.MediaType;
import com.awesomecopilot.networking.enums.HttpMethod;
import com.awesomecopilot.networking.enums.Scheme;
import com.awesomecopilot.networking.exception.HttpRequestException;
import com.awesomecopilot.networking.utils.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsServer;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 2026-09-16 P2 批次修复（评审报告 P2-1/P2-2/P2-3/P2-5/P2-6/P2-7/P2-8/P2-9/P2-13）的行为验证测试。
 * <p>
 * 方式与 HttpUtilsFixesTest 一致：本机起 HTTP/HTTPS 服务真实发请求，在 Handler 里抓
 * "服务端实际收到的" 请求头 / body，断言行为后果而非实现细节。
 * HTTPS 用例用运行时生成的自签证书 keystore（不入库任何密钥文件）。
 * <p>
 * Copyright: (C), 2026-09-16
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class HttpUtilsP2FixesTest {

	private static com.sun.net.httpserver.HttpServer http;
	private static HttpsServer https;
	private static String httpBase;
	private static int httpsPort;

	private static volatile String capturedContentType;
	private static volatile String capturedCookieHeader;
	private static volatile String capturedBody;
	private static volatile boolean setCookieOnNext;

	@BeforeAll
	public static void startServers() throws Exception {
		http = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		http.createContext("/capture", HttpUtilsP2FixesTest::handleCapture);
		http.createContext("/slow", HttpUtilsP2FixesTest::handleSlow);
		http.start();
		httpBase = "http://127.0.0.1:" + http.getAddress().getPort();

		// 自签证书 HTTPS 服务: 运行时用 keytool 生成临时 keystore(不落仓库, 不存密钥文件)
		KeyStore ks = selfSignedKeyStore();
		javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance(
				javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, "changeit".toCharArray());
		SSLContext ssl = SSLContext.getInstance("TLS");
		ssl.init(kmf.getKeyManagers(), null, null);
		https = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		https.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(ssl));
		https.createContext("/secure", HttpUtilsP2FixesTest::handleCapture);
		https.start();
		httpsPort = https.getAddress().getPort();
	}

	@AfterAll
	public static void stopServers() {
		if (http != null) {
			http.stop(0);
		}
		if (https != null) {
			https.stop(0);
		}
	}

	private static void handleCapture(HttpExchange ex) throws java.io.IOException {
		capturedContentType = ex.getRequestHeaders().getFirst("Content-type");
		capturedCookieHeader = ex.getRequestHeaders().getFirst("Cookie");
		capturedBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		byte[] b = "captured".getBytes(StandardCharsets.UTF_8);
		if (setCookieOnNext) {
			// 模拟登录成功下发会话 cookie; HttpOnly 不设, 让默认 cookie 规范接受
			ex.getResponseHeaders().add("Set-Cookie", "sess=xyz789; Path=/");
		}
		ex.sendResponseHeaders(200, b.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(b);
		}
		ex.close();
	}

	private static void handleSlow(HttpExchange ex) throws java.io.IOException {
		try {
			TimeUnit.SECONDS.sleep(3); // 比任何用例的 timeout 都长, 保证超时路径被触发
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		byte[] b = "late".getBytes(StandardCharsets.UTF_8);
		ex.sendResponseHeaders(200, b.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(b);
		}
	}

	/** 运行时生成一张 127.0.0.1 自签证书, 存进内存 keystore（不落仓库任何密钥文件） */
	private static KeyStore selfSignedKeyStore() throws Exception {
		java.io.File tmp = java.io.File.createTempFile("probe-ks", ".p12");
		tmp.deleteOnExit();
		// keytool 要求自己创建文件, 删掉 createTempFile 留下的 0 字节空文件
		java.nio.file.Files.delete(tmp.toPath());
		String keytool = System.getProperty("java.home")
				+ java.io.File.separator + "bin" + java.io.File.separator + "keytool.exe";
		Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", "localhost",
				"-keyalg", "RSA", "-keysize", "2048", "-validity", "30",
				"-storetype", "PKCS12", "-keystore", tmp.getAbsolutePath(),
				"-storepass", "changeit", "-dname", "CN=localhost",
				"-ext", "san=ip:127.0.0.1")
				.redirectErrorStream(true).start();
		String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int code = p.waitFor();
		if (code != 0) {
			throw new IllegalStateException("keytool 生成自签证书失败 exit=" + code + " 输出: " + out);
		}
		KeyStore ks = KeyStore.getInstance("PKCS12");
		try (java.io.FileInputStream in = new java.io.FileInputStream(tmp)) {
			ks.load(in, "changeit".toCharArray());
		}
		return ks;
	}

	// ==================== P2-5 GET/DELETE 不带 Content-Type ====================

	/**
	 * 评审报告 P2-5：HttpUtils.get 修复前预置 Content-Type: application/json。
	 * Content-Type 描述请求体的类型, GET 没有请求体, 不该带。服务端断言收不到该头。
	 */
	@Test
	public void testGetDoesNotSendContentTypeHeader() {
		capturedContentType = "unset";
		HttpUtils.get(httpBase + "/capture").request();
		assertThat(capturedContentType).isNull();
	}

	/**
	 * 对照：POST(JSON) 仍然默认携带 application/json——修复只摘掉"无请求体方法"的头,
	 * 没有改变有请求体场景的默认行为。
	 */
	@Test
	public void testPostStillSendsJsonContentType() {
		capturedContentType = "unset";
		HttpUtils.post(httpBase + "/capture").body("{\"a\":1}").request();
		assertThat(capturedContentType).startsWith(MediaType.APPLICATION_JSON);
	}

	// ==================== P2-6 https 分段构建默认端口 443 ====================

	/**
	 * 评审报告 P2-6：scheme(HTTPS)+host 但没调过 port() 时, 旧实现把字段初始值 80
	 * 写进请求端口。修复后未显式设端口则 https 用 443。
	 * 断言方式：对本机 127.0.0.1 发 https 请求(本机 443 端口没有服务, 连接被立刻拒绝,
	 * 不依赖外网也不依赖 DNS), 异常信息里的目标端口应为 :443——旧实现会是 :80。
	 */
	@Test
	public void testHttpsDefaultPortIs443WhenPortNotSet() {
		Throwable t = catchThrowableOfType(
				() -> HttpUtils.get().scheme(Scheme.HTTPS).host("127.0.0.1")
						.path("/x").connectionTimeout(3, TimeUnit.SECONDS).request(),
				HttpRequestException.class);
		assertThat(t).isNotNull();
		assertThat(rootMessage(t)).contains("127.0.0.1:443");
	}

	/**
	 * 显式 port() 必须被尊重(即使是 80/443 之外的端口)——P2-6 修复的对照面。
	 */
	@Test
	public void testExplicitPortStillRespected() {
		capturedContentType = "unset";
		// 对本机 HTTP 服务用 https scheme + 真实端口: TLS 握手对明文端口必然失败,
		// 但异常信息里会带上我们设定的端口, 证明显式值生效
		final int port = http.getAddress().getPort();
		Throwable t = catchThrowableOfType(
				() -> HttpUtils.get().scheme(Scheme.HTTPS).host("127.0.0.1").port(port)
						.path("/capture").connectionTimeout(3, TimeUnit.SECONDS).soTimeout(3, TimeUnit.SECONDS).request(),
				HttpRequestException.class);
		assertThat(t).isNotNull();
		assertThat(rootMessage(t)).contains("127.0.0.1:" + port);
	}

	// ==================== P2-7 表单 Content-Type 带 charset ====================

	/**
	 * 评审报告 P2-7：form(url) 预置的 Content-Type 现在必须声明 charset=UTF-8。
	 * 请求体实际按 UTF-8 编码, 头里不声明字符集时, 严格按规范解析的服务端会按
	 * ISO-8859-1 解中文表单字段导致乱码。
	 */
	@Test
	public void testFormSendsContentTypeWithCharset() {
		capturedContentType = "unset";
		HttpUtils.form(httpBase + "/capture").formData("name", "三少爷").request();
		assertThat(capturedContentType)
				.startsWith("application/x-www-form-urlencoded")
				.containsIgnoringCase("charset=UTF-8");
	}

	// ==================== P2-3 timeout 用共享调度器, 请求可正常收敛 ====================

	/**
	 * 评审报告 P2-3：生命周期超时行为保持不变——慢响应在 timeout 到点时被中断、
	 * 调用方收到异常。实现从"每请求 new Timer 且任务不取消"换成共享调度线程 +
	 * 请求结束即 cancel 任务, 本用例守护换实现后的对外行为等价。
	 */
	@Test
	public void testRequestLevelTimeoutStillAbortsSlowRequest() {
		assertThatThrownBy(() -> HttpUtils.get(httpBase + "/slow")
				.timeout(300, TimeUnit.MILLISECONDS)
				.request())
				.isInstanceOf(HttpRequestException.class);
	}

	/**
	 * timeout 设了但请求提前正常结束时, 定时任务被取消、不会去中断后续请求:
	 * 旧实现的 Timer 任务不取消, 同一 httpRequest 对象不复用碰不到, 但换共享调度器后
	 * 若忘记 cancel, 残留任务会累积——用"快请求 + 大 timeout 连续 5 次全成功"守住
	 * 取消路径不产生误伤。
	 */
	@Test
	public void testFastRequestsWithTimeoutAllSucceed() {
		for (int i = 0; i < 5; i++) {
			String r = HttpUtils.get(httpBase + "/capture")
					.timeout(10, TimeUnit.SECONDS)
					.request();
			assertThat(r).isEqualTo("captured");
		}
	}

	// ==================== P2-8 跨请求共享 cookieStore ====================

	/**
	 * 评审报告 P2-8：修复前每个 builder 自带一次性 BasicCookieStore, 服务端
	 * Set-Cookie 下发的会话 cookie 随 builder 一起丢弃, 下一个请求什么都不带。
	 * 修复后调用方可显式注入共享 store：请求1(登录)拿到 Set-Cookie 存进 store,
	 * 请求2(带同一 store)自动携带 sess=xyz789。
	 */
	@Test
	public void testSharedCookieStoreCarriesSessionCookieAcrossRequests() {
		BasicCookieStore shared = new BasicCookieStore();
		setCookieOnNext = true;
		try {
			HttpUtils.get(httpBase + "/capture").cookieStore(shared).request();
			// 登录响应里的 Set-Cookie 应已进 store
			assertThat(shared.getCookies()).isNotEmpty();

			capturedCookieHeader = null;
			String r = HttpUtils.get(httpBase + "/capture").cookieStore(shared).request();
			assertThat(r).isEqualTo("captured");
			assertThat(capturedCookieHeader).contains("sess=xyz789");
		} finally {
			setCookieOnNext = false;
		}
	}

	// ==================== P2-9 retries 配置与否走同一个 handler 类型 ====================

	/**
	 * 评审报告 P2-9：旧代码不指定 retries 走 HttpClientBuilder 内部的
	 * DefaultHttpRequestRetryHandler, 指定后换 StandardHttpRequestRetryHandler,
	 * 两者的可重试异常名单不同——配个次数顺带换了重试策略。
	 * 修复后两条路都是 StandardHttpRequestRetryHandler, 只是次数不同。
	 */
	@Test
	public void testRetryHandlerTypeIsUniformWithAndWithoutRetries() {
		HttpRequestRetryHandler without =
				JsonRequestBuilder.resolveRetryHandler(null, false);
		HttpRequestRetryHandler with =
				JsonRequestBuilder.resolveRetryHandler(6, true);

		assertThat(without).isInstanceOf(StandardHttpRequestRetryHandler.class);
		assertThat(with).isInstanceOf(StandardHttpRequestRetryHandler.class);
		assertThat(without).isNotSameAs(with);
	}

	// ==================== P2-1 HTTPS 默认证书校验; 显式开关可回到旧行为 ====================

	/**
	 * 评审报告 P2-1：本地自签证书 HTTPS 服务, 默认(不声明 trustAllCerts)必须拒绝连接——
	 * 证书链不受系统信任, 握手失败。修复前信任所有证书, 这条会"成功"。
	 */
	@Test
	public void testSelfSignedHttpsRejectedByDefault() {
		assertThatThrownBy(() -> HttpUtils.get("https://127.0.0.1:" + httpsPort + "/secure")
				.request())
				.isInstanceOf(HttpRequestException.class)
				.satisfies(t -> assertThat(sslRelatedCause((Throwable) t)).isTrue());
	}

	/**
	 * 评审报告 P2-1 的开关面：显式 trustAllCerts(true) 时保持旧行为(内网自签环境),
	 * 对同一个自签服务请求成功。
	 */
	@Test
	public void testSelfSignedHttpsSucceedsWithTrustAllCertsFlag() {
		capturedContentType = "unset";
		String r = HttpUtils.get("https://127.0.0.1:" + httpsPort + "/secure")
				.trustAllCerts(true)
				.request();
		assertThat(r).isEqualTo("captured");
	}

	/**
	 * 评审报告 P2-1/P2-13 的接线正确性：两种模式路由到两个不同的 socket factory,
	 * 且同一参数两次取得的是同一个实例(工厂复用, 不会每请求新建)。
	 * 主机名校验是否真正生效不在这里断言(SDK 类无公开getter可查)——
	 * 由 testSelfSignedHttpsRejectedByDefault / SucceedsWithTrustAllCertsFlag
	 * 两个端到端用例从行为上覆盖。
	 */
	@Test
	public void testSslSocketFactorySelection() {
		assertThat(JsonRequestBuilder.sslSocketFactoryFor(false))
				.isSameAs(JsonRequestBuilder.sslSocketFactoryFor(false));
		assertThat(JsonRequestBuilder.sslSocketFactoryFor(true))
				.isNotSameAs(JsonRequestBuilder.sslSocketFactoryFor(false));
	}

	// ==================== P2-2 空闲连接回收线程已接线 ====================

	/**
	 * 评审报告 P2-2：IdleConnectionEvictor 此前定义了却全仓库无人启动。
	 * 修复后 AbstractRequestBuilder 静态初始化时启动它——触发类加载后可观测。
	 */
	@Test
	public void testIdleConnectionEvictorIsRunning() {
		// 引用一次静态方法即完成静态块初始化
		JsonRequestBuilder.isIdleConnectionEvictorRunning();
		assertThat(JsonRequestBuilder.isIdleConnectionEvictorRunning()).isTrue();
	}

	// ==================== P2-10 HttpMethods 常量值 ====================

	/**
	 * 评审报告 P2-10：HttpMethods 六个常量此前全是 "GET"(复制粘贴未改值),
	 * 已逐个改正; 一旦回退, 本测试变红。
	 */
	@Test
	public void testHttpMethodsConstantsHaveCorrectValues() {
		assertThat(com.awesomecopilot.networking.constants.HttpMethods.GET).isEqualTo("GET");
		assertThat(com.awesomecopilot.networking.constants.HttpMethods.POST).isEqualTo("POST");
		assertThat(com.awesomecopilot.networking.constants.HttpMethods.PUT).isEqualTo("PUT");
		assertThat(com.awesomecopilot.networking.constants.HttpMethods.DELETE).isEqualTo("DELETE");
		assertThat(com.awesomecopilot.networking.constants.HttpMethods.OPTIONS).isEqualTo("OPTIONS");
		assertThat(com.awesomecopilot.networking.constants.HttpMethods.TRACE).isEqualTo("TRACE");
		assertThat(com.awesomecopilot.networking.constants.HttpMethods.HEAD).isEqualTo("HEAD");
		assertThat(com.awesomecopilot.networking.constants.HttpMethods.PATCH).isEqualTo("PATCH");
	}

	// ==================== 辅助 ====================

	private static String rootMessage(Throwable t) {
		StringBuilder sb = new StringBuilder();
		for (Throwable c = t; c != null; c = c.getCause()) {
			sb.append(c.getMessage()).append(' ');
		}
		return sb.toString();
	}

	private static boolean sslRelatedCause(Throwable t) {
		for (Throwable c = t; c != null; c = c.getCause()) {
			if (c instanceof javax.net.ssl.SSLException
					|| c.getClass().getName().startsWith("sun.security.")
					|| String.valueOf(c.getMessage()).contains("SSL")
					|| String.valueOf(c.getMessage()).contains("证书")
					|| String.valueOf(c.getMessage()).contains("unable to find valid certification")) {
				return true;
			}
		}
		return false;
	}
}
