package com.awesomecopilot.networking;

import com.awesomecopilot.networking.utils.HttpUtils;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 《copilot-networking/评审报告.md》P0-1 的行为回归测试。
 * <p>
 * 原缺陷：AbstractRequestBuilder.request() 用 try-with-resources 关闭 CloseableHttpClient,
 * 而这个 client 背后挂的是 static 全局共享的 PoolingHttpClientConnectionManager。
 * HttpClientBuilder 对"构造时传入的外部连接管理器"默认注册一条关池动作
 * （httpclient 4.5.13 源码 HttpClientBuilder.java:1244 的 cm.shutdown()）,
 * 于是第一个请求结束时把整个进程的共享连接池关掉了, 之后的每个请求都抛
 * IllegalStateException: Connection pool shut down。
 * <p>
 * 修复（2026-09-16）：buildHttpClient() 增加 .setConnectionManagerShared(true),
 * 声明"连接管理器不归这个客户端所有", 客户端 close() 不再连带关闭共享池。
 * <p>
 * 本测试锁定修复后的行为，防止问题重新出现：同一个进程内连续多次 HttpUtils 请求都必须成功。
 * 如果将来有人删掉 setConnectionManagerShared(true) 或改造客户端生命周期时
 * 重新引入"close 客户端 = 关共享池", 本测试会红。
 * <p>
 * Copyright: (C), 2026-09-16
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class HttpUtilsConnectionPoolRegressionTest {

	private static HttpServer server;
	private static String url;

	@BeforeAll
	public static void startLocalServer() throws Exception {
		// 本机随机端口起一个最小 HTTP 服务, 测试不依赖任何外部服务
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/ok", exchange -> {
			byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(body);
			}
		});
		server.start();
		url = "http://127.0.0.1:" + server.getAddress().getPort() + "/ok";
	}

	@AfterAll
	public static void stopLocalServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	/**
	 * 同一进程内连续发起 3 次 GET：P0-1 修复前第 2 次起必抛 Connection pool shut down,
	 * 修复后三次全部成功且响应体一致。
	 * <p>
	 * 用循环而不是三次平铺代码：次数越多越能同时覆盖"池被误关"（第 2 次即失败）和
	 * "keep-alive 连接复用路径上的状态污染"（第 3 次起才暴露）两类问题;
	 * 同时连接池复用要求请求间隔内连接归还顺畅, 任何一次失败都会在这里现形。
	 */
	@Test
	public void testRepeatedRequestsAllSucceedWithSharedPool() {
		for (int attempt = 1; attempt <= 3; attempt++) {
			String result = null;
			Throwable failure = null;
			try {
				result = HttpUtils.get(url).request();
			} catch (Throwable t) {
				failure = t;
			}
			assertThat(failure)
					.as("第 %d 次 HttpUtils 请求不应抛异常(P0-1 回归)", attempt)
					.isNull();
			assertThat(result)
					.as("第 %d 次 HttpUtils 请求应拿到正确响应体", attempt)
					.isEqualTo("{\"ok\":true}");
		}
	}
}
