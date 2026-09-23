package com.awesomecopilot.common.lang.io;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-5 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）：
 * UrlResource 的 Basic 认证头曾使用 URL-safe Base64 字母表（含 -_），
 * 而 RFC 7617 规定 Basic 凭据使用标准字母表（+/）——用户名/密码字节里
 * 恰好出现索引 62/63 时，严格的服务端解码失败，表现为间歇性 401。
 * <p>
 * 用例走本地 HttpServer 看"服务器实际收到的头"：
 * 字节序列 "~:pw"（0x7E 0x3A 0x70 0x77）的 Base64 第二组会产生索引 63
 * （标准表 '/'，url-safe 表 '_'），可区分两套字母表。
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class UrlResourceP2FixTest {

	private static HttpServer server;
	private static volatile String receivedAuth;
	private static int port;

	@BeforeAll
	public static void startServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			receivedAuth = exchange.getRequestHeaders().getFirst("Authorization");
			byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		port = server.getAddress().getPort();
	}

	@AfterAll
	public static void stopServer() {
		server.stop(0);
	}

	@Test
	public void testBasicAuthHeaderUsesStandardBase64Alphabet() throws Exception {
		//userInfo 里的 "~:pw" 编解码后含索引 63 字符: 标准表出 '/', url-safe 表出 '_'
		UrlResource resource = new UrlResource("http://admin~:pw@127.0.0.1:" + port + "/probe");
		try (InputStream in = resource.getInputStream()) {
			assertThat(in.readAllBytes()).hasSize(2); //确认请求真的发出并收到 200
		}
		assertThat(receivedAuth).as("服务器必须收到 Authorization 头").isNotNull();
		String credentials = "admin~:pw";
		String standard = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
		String urlSafe = Base64.getUrlEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
		assertThat(standard).as("该入参必须能区分两套字母表").isNotEqualTo(urlSafe);
		assertThat(receivedAuth)
				.as("修复前实测服务端收到 Basic %s(url-safe 表, 含 '_'), 严格服务端会解码失败给 401", urlSafe)
				.isEqualTo("Basic " + standard);
	}
}
