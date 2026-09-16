package com.awesomecopilot.networking;

import com.awesomecopilot.common.lang.exception.BusinessException;
import com.awesomecopilot.networking.enums.HttpMethod;
import com.awesomecopilot.networking.exception.HttpRequestException;
import com.awesomecopilot.networking.utils.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 2026-09-16 一批修复（评审报告 P0-2/P0-3、P1-1/P1-2/P1-3/P1-4/P1-5/P1-7）的行为验证测试。
 * <p>
 * 测试方式：本机随机端口起一个 com.sun.net.httpserver 服务，用 HttpUtils 真实发请求，
 * 在 Handler 里抓下"服务端实际收到的" 方法名 / 原始查询串 / 请求体 / Cookie 头，
 * 然后断言这些送达结果——验证的是行为后果，不是实现细节。全程不依赖外网与任何存量服务。
 * <p>
 * 各用例与修复条目的对应关系写在每个方法的注释里。
 * <p>
 * Copyright: (C), 2026-09-16
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class HttpUtilsFixesTest {

	private static HttpServer server;
	private static String base;

	// 服务端视角的捕获槽位
	private static volatile String capturedMethod;
	private static volatile String capturedRawQuery;
	private static volatile String capturedBody;
	private static volatile String capturedCookieHeader;

	@BeforeAll
	public static void startServer() throws Exception {
		// 通配地址绑定, IPv4(127.0.0.1) 和 IPv6(::1) 的请求都能进来
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/capture", HttpUtilsFixesTest::recordThenRespond200);
		server.createContext("/404", ex -> respond(ex, 404, "{\"error\":\"not found\"}"));
		server.createContext("/401", ex -> respond(ex, 401, "{\"error\":\"unauthorized\"}"));
		server.createContext("/204", ex -> respond(ex, 204, null));
		server.start();
		base = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	public static void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	private static void recordThenRespond200(HttpExchange ex) throws java.io.IOException {
		capturedMethod = ex.getRequestMethod();
		capturedRawQuery = ex.getRequestURI().getRawQuery();
		capturedCookieHeader = ex.getRequestHeaders().getFirst("Cookie");
		capturedBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		respond(ex, 200, "captured");
	}

	private static void respond(HttpExchange ex, int status, String body) throws java.io.IOException {
		byte[] b = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
		if (status == 204) {
			ex.sendResponseHeaders(204, -1);
		} else {
			ex.sendResponseHeaders(status, b.length);
			try (OutputStream os = ex.getResponseBody()) {
				os.write(b);
			}
		}
		ex.close();
	}

	private static void resetCaptured() {
		capturedMethod = null;
		capturedRawQuery = null;
		capturedBody = null;
		capturedCookieHeader = null;
	}

	// ==================== P0-2 状态码检查 ====================

	/**
	 * 评审报告 P0-2：修复前 404 落到 ErrorUtils.checkError 的 switch 结尾什么都不做，
	 * 错误响应体被当成正常结果返回。修复后：404 必须抛 BusinessException，
	 * 错误码是 NOT_FOUND 对应的 "4041"。
	 */
	@Test
	public void test404ThrowsBusinessExceptionInsteadOfReturningBody() {
		assertThatThrownBy(() -> HttpUtils.get(base + "/404").request())
				.isInstanceOf(BusinessException.class)
				// 异常允许被 HttpRequestException 包一层? 不允许——P1-6 修复点：BusinessException 原样抛出
				.satisfies(t -> assertThat(((BusinessException) t).getCode()).isEqualTo("4041"));
	}

	/**
	 * 评审报告 P0-2：401 这类 ErrorTypes 没有一一对应枚举的状态码，
	 * 也要抛异常且异常消息带状态码，调用方能直接看出是 HTTP 语义错误。
	 */
	@Test
	public void test401ThrowsWithStatusCodeInMessage() {
		assertThatThrownBy(() -> HttpUtils.get(base + "/401").request())
				.isInstanceOfAny(BusinessException.class, HttpRequestException.class)
				.hasMessageContaining("401");
	}

	/**
	 * 评审报告 P0-2 的顺带修正：204 No Content 是合法成功。
	 * 旧实现 `if (statucCode != 200)` 会把 204 送进 checkError 且旧 switch 里没有 204 分支,
	 * 等于把成功响应当错误路径处理; 修复后 2xx 整段放行, 无响应体时返回 null 而不是抛异常。
	 */
	@Test
	public void test204IsTreatedAsSuccessReturningNull() {
		String result = HttpUtils.get(base + "/204").request();
		assertThat(result).isNull(); // 没有抛异常就是 P0-2 修复生效的一半, 返回 null 对应"确实没有内容"
	}

	// ==================== P1-7 响应类型 ====================

	/**
	 * 评审报告 P1-7：声明 responseType(byte[].class) 时, 修复前会走 Jackson 分支
	 * 抛 ClassCastException（被包成看不出根因的 HttpRequestException）;
	 * 修复后与 returnBytes(true) 联动, 直接拿到 byte[]。
	 */
	@Test
	public void testResponseTypeByteArrayReturnsBytesWithoutReturnBytesFlag() {
		byte[] data = HttpUtils.get(base + "/capture").responseType(byte[].class).request();
		assertThat(data).isNotNull();
		assertThat(new String(data, StandardCharsets.UTF_8)).isEqualTo("captured");
	}

	// ==================== P0-3 / P1-3 URL 查询参数 ====================

	/**
	 * 评审报告 P0-3 + P1-3：URL 里自带、已经做过 URL 编码的查询参数——
	 * 修复前：参数被重新按原始文本编码一次（%20 变 %2520）, 值带 = 的参数（token=ab%3Dcd%3D）
	 * 和没有 = 的裸参数（flag）被不报错地丢弃。
	 * 修复后（服务端抓包断言）：type 的编码保持单层、token 完整保留、flag 保留。
	 */
	@Test
	public void testUrlQueryParamsKeepSingleEncodingAndNotDropped() {
		resetCaptured();
		HttpUtils.get(base + "/capture?type=doc%20x&token=ab%3Dcd%3D&flag").request();

		assertThat(capturedRawQuery)
				.contains("type=doc%20x")        // 单层编码: 修复前是 doc%2520x
				.contains("token=ab%3Dcd%3D")     // 值里带 = 的参数完整送达: 修复前整个参数消失
				.doesNotContain("%25")            // 全文不含二次编码特征
				.matches(".*(^|&)flag(&|$).*");   // 无值裸参数保留: 修复前被丢
	}

	/**
	 * 评审报告 P0-3：用户显式 addParam 的原始文本值（含空格）应被恰好编码一次。
	 * 服务端收到 q=hello+world（application/x-www-form-urlencoded 风格的空格加号编码,
	 * URIBuilder 对 UTF-8 的规范行为）, 不能出现 %2520。
	 */
	@Test
	public void testExplicitParamEncodedExactlyOnce() {
		resetCaptured();
		HttpUtils.get(base + "/capture").addParam("q", "hello world").request();

		assertThat(capturedRawQuery)
				.contains("q=hello+world")
				.doesNotContain("%25");
	}

	/**
	 * 评审报告 P1-2（路径 A）：同名参数 addParam 两次, 服务端应收到标准的重复参数
	 * tag=java 与 tag=go 各一次（pairs 列表分支的语义）。
	 */
	@Test
	public void testRepeatedAddParamProducesDuplicateQueryParams() {
		resetCaptured();
		HttpUtils.get(base + "/capture").addParam("tag", "java").addParam("tag", "go").request();

		assertThat(capturedRawQuery)
				.contains("tag=java")
				.matches(".*tag=go.*");
	}

	/**
	 * 评审报告 P1-2（路径 B）：参数值本身是一个 List（form.param 直接塞集合的场景,
	 * MultiValueMap 收进后呈 [[java, go]] 嵌套形态）。
	 * 修复前：整个 List 被当单个元素调 toString, 服务端收到 tag=[java, go]（带方括号）;
	 * 修复后：逐层摊平逗号连接, 服务端收到 tag=java%2Cgo。
	 */
	@Test
	public void testListParamValueFlattenedToCommaJoined() {
		resetCaptured();
		HttpUtils.get(base + "/capture")
				.addParam("tag", Arrays.asList("java", "go"))
				.addParam("other", 1)
				.request();

		assertThat(capturedRawQuery)
				.contains("tag=java%2Cgo") // 逗号编码为 %2C, 值干净
				.doesNotContain("%5B")      // 没有左方括号 '['
				.doesNotContain("[");
	}

	// ==================== P1-1 PATCH ====================

	/**
	 * 评审报告 P1-1：HttpMethod 枚举里有 PATCH 但 switch 没这个分支,
	 * 修复前 request 保持 null, 走到 addHeader 抛 NullPointerException。
	 * 修复后：PATCH 方法与请求体都要真实送达服务端（addBody 挂载判断同步放宽才有 body）。
	 */
	@Test
	public void testPatchMethodAndBodyDelivered() {
		resetCaptured();
		HttpUtils.post(base + "/capture")
				.method(HttpMethod.PATCH)
				.body("{\"p\":1}")
				.request();

		assertThat(capturedMethod).isEqualTo("PATCH");
		assertThat(capturedBody).isEqualTo("{\"p\":1}");
	}

	// ==================== P1-4 URL 解析 ====================

	/**
	 * 评审报告 P1-4：旧自研正则要求端口至少两位、host 段排除冒号,
	 * 导致 IPv6 地址解析错位。修复后走 JDK URI 解析器, http://[::1]:port 能正常请求。
	 */
	@Test
	public void testIpv6UrlParsesAndConnects() {
		int port = server.getAddress().getPort();
		String response = HttpUtils.get("http://[::1]:" + port + "/capture").request();
		assertThat(response).isEqualTo("captured");
	}

	/**
	 * 评审报告 P1-4：非法 URL 要当场报出原因并带原始文本。
	 * 修复前：RegexUtils.teardown 返回 null, request() 拿 null 调 getScheme() 抛
	 * NullPointerException, 报错位置与真实原因（URL 写错）完全对不上。
	 */
	@Test
	public void testInvalidUrlThrowsWithOriginalText() {
		assertThatThrownBy(() -> HttpUtils.get("http://%zz/").request())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不合法的URL");
	}

	// ==================== P1-5 cookie ====================

	/**
	 * 评审报告 P1-5：FormRequestBuilder.addCookie(name,value) 修复前把域写死成
	 * "sexy-uncle.com", 请求本机时因域不匹配根本不会携带该 cookie;
	 * 只删写死值也不行（RFC6265 拒收无域手工 cookie, 实测 Cookie 头为空）,
	 * 需配套 assignDomainlessCookies 回填请求主机域。
	 * 本用例断言服务端真实收到 Cookie: sid=abc123。
	 */
	@Test
	public void testFormAddCookieReachesServer() {
		resetCaptured();
		HttpUtils.form(base + "/capture")
				.formData("u", "1")
				.addCookie("sid", "abc123")
				.request();

		assertThat(capturedCookieHeader).isNotNull();
		assertThat(capturedCookieHeader).contains("sid=abc123");
	}

	/**
	 * 对照组：JsonRequestBuilder 的 addCookie 同样要送达（两参版没写死过域,
	 * 但 assignDomainlessCookies 是加在公共链路上的, 两个构建器行为从此一致）。
	 */
	@Test
	public void testJsonAddCookieReachesServer() {
		resetCaptured();
		HttpUtils.get(base + "/capture")
				.addCookie("lang", "zh")
				.request();

		assertThat(capturedCookieHeader).isNotNull();
		assertThat(capturedCookieHeader).contains("lang=zh");
	}
}
