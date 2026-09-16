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
 * 复现《copilot-networking/评审报告.md》P0-1 的回归测试：
 * AbstractRequestBuilder.request() 用 try-with-resources 关闭 CloseableHttpClient,
 * 而这个 client 背后挂的是 static 全局共享的 PoolingHttpClientConnectionManager。
 * HttpClientBuilder 对"构造时传入的外部连接管理器"默认会注册一条关池动作
 * （httpclient 4.5.13 源码 HttpClientBuilder.java:1244 的 cm.shutdown()）,
 * 所以第一个请求结束时把整个进程的共享连接池关掉了, 之后的每个请求都抛
 * IllegalStateException: Connection pool shut down。
 * <p>
 * ⚠️ 这个测试断言的是【当前的错误行为】——它通过, 恰恰说明 bug 还在。
 * 修复方式（评审报告 P0-1 方案②, buildHttpClient() 加 .setConnectionManagerShared(true),
 * 或方案① 客户端进程级单例）落地之后, 请把断言翻转为"两次请求都成功",
 * 并把类名注释改写为"防止连接池被关闭的行为回归"。
 * <p>
 * 探针版结论升级为可重跑的单元测试依据：报告中该条标注【实测】时用的是临时目录里
 * 一次性探针, 不在仓库内; 本类是可进 CI 的等价复现。
 * <p>
 * Copyright: (C), 2026-09-16
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class HttpUtilsConnectionPoolBugTest {

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
	 * 连续两次通过 HttpUtils 请求同一个本地服务, 断言第二次一定失败。
	 * <p>
	 * 为什么不断言"第一次成功、第二次失败"：surefire 默认同一个 JVM 里跑完整个测试包,
	 * 如果先跑的别的测试已经用过 HttpUtils（哪怕那次请求自己失败了）, 共享池在本测试
	 * 开始前就已经被关闭, "第一次成功"就不再成立。而"池一旦关掉就再也回不来"正是
	 * P0-1 的核心事实——所以无论前序谁动过池, 本类最后一次调用必然以
	 * Connection pool shut down 收场, 这个断言是确定性的。
	 * <p>
	 * 修复后预期行为：两次都成功, 断言相应翻转（见类注释的 ⚠️ 说明）。
	 */
	@Test
	public void testSecondRequestFailsBecauseSharedPoolWasShutDownByFirstClient() {
		String first = tryRequest();
		String second = tryRequest();

		// 第二次必然抛异常, 且异常链里必须有"池已关闭"这个根因
		assertThat(second)
				.as("当前实现下第二次请求必然失败(评审报告P0-1); 若此断言因修复而失败, 请翻转本测试断言")
				.contains("Connection pool shut down");
		// 第一次是否成功取决于运行顺序(池可能已被前序测试关闭), 只记录不强制
		System.out.println("HttpUtils P0-1 复现: first=" + (first == null ? "success" : "failed(" + first + ")")
				+ ", second=" + second);
	}

	/**
	 * 发一次 HttpUtils GET 请求。
	 *
	 * @return null 表示请求成功; 否则返回异常链根因的"类名: 消息"文本
	 */
	private String tryRequest() {
		try {
			String result = HttpUtils.get(url).request();
			return "{\"ok\":true}".equals(result) ? null : "unexpected-body: " + result;
		} catch (Throwable t) {
			Throwable root = t;
			while (root.getCause() != null) {
				root = root.getCause();
			}
			return root.getClass().getSimpleName() + ": " + root.getMessage();
		}
	}
}
