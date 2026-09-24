package com.awesomecopilot.networking;

import com.awesomecopilot.networking.builder.AbstractRequestBuilder;
import com.awesomecopilot.networking.builder.JsonRequestBuilder;
import com.awesomecopilot.networking.exception.HttpRequestException;
import com.awesomecopilot.networking.utils.HttpUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-6 回归测试：三个 HTTP 超时（建连/传输/借连接）此前不设即无限等待，
 * 慢下游会把调用线程和共享连接池一起拖到排队。修复后未显式配置的请求
 * 生效保守默认值（5s/10s/2s），http.properties 可按键覆盖（仅正整数生效），显式设置优先。
 * <p>
 * 注意 test/resources/http.properties 对整个 copilot-networking 测试类路径生效。
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class TimeoutDefaultsTest {
	
	/**
	 * 回落链校验：连接管理器键配了合法正整数 1500 → 覆盖硬默认 2000；
	 * connect 键配了非法值 abc、socket 键配了 -1 → 都不采纳, 落硬默认 5000/10000。
	 * （硬默认 2000 本身被 1500 盖住, 在带测试资源的场景不可达, 属预期。）修复前三者都是 -1。
	 */
	@Test
	public void testDefaultsAndPropertiesApplyToRequestConfig() throws Exception {
		JsonRequestBuilder builder = HttpUtils.get("http://127.0.0.1:1/");
		try (CloseableHttpClient client = buildClientOf(builder)) {
			RequestConfig cfg = requestConfigOf(client);
			assertEquals(5000, cfg.getConnectTimeout(), "非法值属性应落硬默认 5s");
			assertEquals(10000, cfg.getSocketTimeout(), "负值属性应落硬默认 10s");
			assertEquals(1500, cfg.getConnectionRequestTimeout(),
					"http.properties 的合法正整数 1500 应覆盖硬默认 2000");
		}
		// 未显式设置时三个字段保持 null, 回落发生在装配时, 不污染 builder 字段
		assertNullField(builder, "connectionTimeout");
		assertNullField(builder, "soTimeout");
		assertNullField(builder, "connectionManagerTimeout");
	}
	
	/**
	 * FormRequestBuilder（另一个子类, 未覆盖 buildHttpClient）走同一回落。
	 */
	@Test
	public void testFormRequestBuilderGetsSameDefaults() throws Exception {
		RequestConfig cfg;
		try (CloseableHttpClient client = buildClientOf(HttpUtils.form("http://127.0.0.1:1/"))) {
			cfg = requestConfigOf(client);
		}
		assertEquals(5000, cfg.getConnectTimeout());
		assertEquals(10000, cfg.getSocketTimeout());
		assertEquals(1500, cfg.getConnectionRequestTimeout());
	}
	
	/**
	 * 显式设置优先于默认值：soTimeout(1s) 要压过默认 10s。
	 */
	@Test
	public void testExplicitTimeoutOverridesDefault() throws Exception {
		JsonRequestBuilder builder = HttpUtils.get("http://127.0.0.1:1/")
				.soTimeout(1, TimeUnit.SECONDS);
		RequestConfig cfg;
		try (CloseableHttpClient client = buildClientOf(builder)) {
			cfg = requestConfigOf(client);
		}
		assertEquals(1000, cfg.getSocketTimeout(), "显式 1s 应压过默认 10s");
	}
	
	/**
	 * 显式传 -1 按调用方原意透传(=无限等待, javadoc 已提示风险), 不被默认值拦截。
	 */
	@Test
	public void testExplicitNegativeOnePassesThrough() throws Exception {
		JsonRequestBuilder builder = HttpUtils.get("http://127.0.0.1:1/")
				.connectionTimeout(-1, TimeUnit.MILLISECONDS);
		RequestConfig cfg;
		try (CloseableHttpClient client = buildClientOf(builder)) {
			cfg = requestConfigOf(client);
		}
		assertEquals(-1, cfg.getConnectTimeout(), "显式 -1 应原样透传");
	}
	
	/**
	 * 行为验证：下游收完请求头后不回包也不断开，默认 socket 超时(10s)内必须抛错收场。
	 * 修复前这里读阻塞, 直到服务端自行关闭才收场。
	 * 断言预算 = 默认 soTimeout(10s) + 5s 余量；服务端循环 accept，
	 * 即使重试策略将来变化(当前 SocketTimeoutException 属不可重试异常)也不会二次建连后一直等待。
	 */
	@Test
	public void testSlowDownstreamFailsWithDefaultSocketTimeout() throws Exception {
		try (ServerSocket server = new ServerSocket(0)) {
			Thread sink = new Thread(() -> {
				while (!server.isClosed()) {
					try (Socket s = server.accept()) {
						BufferedReader r = new BufferedReader(
								new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
						String line;
						while ((line = r.readLine()) != null && !line.isEmpty()) {
							// 读完请求头
						}
						Thread.sleep(20_000); // 不回包不断开
					} catch (Exception ignored) {
						// 客户端超时断开属预期
					}
				}
			});
			sink.setDaemon(true);
			sink.start();
			
			long start = System.currentTimeMillis();
			HttpRequestException e = assertThrows(HttpRequestException.class, () ->
					HttpUtils.get("http://127.0.0.1:" + server.getLocalPort() + "/slow").request());
			long cost = System.currentTimeMillis() - start;
			assertTrue(cost < 15_000, "应在默认 socket 超时(10s)附近收场, 实际耗时 " + cost + "ms");
			assertInstanceOf(SocketTimeoutException.class, e.getCause(),
					"根因应是 socket 读超时, 实际: " + e.getCause());
		}
	}
	
	/**
	 * 行为验证：连不上的地址在默认 connect 超时(5s)内收场, 不等操作系统耗尽重传。
	 * 若网络环境会快速拒绝(RST)则更快, 断言只给上限不给下限。
	 */
	@Test
	public void testUnreachableHostFailsFast() {
		long start = System.currentTimeMillis();
		assertThrows(HttpRequestException.class, () ->
				HttpUtils.get("http://10.255.255.1:8080/").request());
		long cost = System.currentTimeMillis() - start;
		assertTrue(cost < 12_000, "应在默认 connect 超时(5s)附近收场, 实际耗时 " + cost + "ms");
	}
	
	/** buildHttpClient 是 protected(跨包不可见), 反射调用; 返回的 client 由调用方 try-with 关闭 */
	private static CloseableHttpClient buildClientOf(AbstractRequestBuilder builder) throws Exception {
		Method m = AbstractRequestBuilder.class.getDeclaredMethod("buildHttpClient");
		m.setAccessible(true);
		return (CloseableHttpClient) m.invoke(builder);
	}
	
	/**
	 * 优先走公开方法 InternalHttpClient.getConfig()(比反射私有字段稳定);
	 * 方法不存在时退回沿类层级找 defaultConfig 字段, 找不到就响亮抛错, 不会取错值。
	 */
	private static RequestConfig requestConfigOf(CloseableHttpClient client) throws Exception {
		try {
			Method getConfig = client.getClass().getMethod("getConfig");
			// InternalHttpClient 类本身包私有, 方法虽 public 反射直调仍被拒, 需 setAccessible
			getConfig.setAccessible(true);
			Object cfg = getConfig.invoke(client);
			if (cfg instanceof RequestConfig) {
				return (RequestConfig) cfg;
			}
		} catch (NoSuchMethodException ignored) {
			// 退到字段反射
		}
		for (Class<?> c = client.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField("defaultConfig");
				f.setAccessible(true);
				Object cfg = f.get(client);
				assertInstanceOf(RequestConfig.class, cfg);
				return (RequestConfig) cfg;
			} catch (NoSuchFieldException ignored) {
				// 继续向父类找
			}
		}
		throw new NoSuchFieldException("在 " + client.getClass() + " 上既没有 getConfig() 也没有 defaultConfig 字段");
	}
	
	private static void assertNullField(Object target, String name) throws Exception {
		Field f = AbstractRequestBuilder.class.getDeclaredField(name);
		f.setAccessible(true);
		assertNull(f.get(target), name + " 未显式设置应保持 null");
	}
}
