package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-32 UrlUtils.encodeUrl 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <p>
 * 修复前（git HEAD 旧实现，现象由对照探针复现）：
 * <ul>
 * <li>重建 URL 只拼 协议+主机+端口+路径+查询——
 * "https://ex.com/a?q=hello world#frag" 编码后 #frag 整段消失；
 * "https://u:p@ex.com/p" 的 userInfo 被整段丢弃（后续请求以未认证身份发出）；</li>
 * <li>查询串整体 encode 后把 %3D/%26 无条件换回 =/&——原值里已编码好的 %20
 * 被二次编码成 %2520（"q=a%20b" 实测变 "q=a%2520b"）。</li>
 * </ul>
 * 修复后按组件重建并透传合法 %XX 序列。行为变化：单独查询串入参 "q=a=b" 的值内
 * 字面 = 编码为 %3D（修复前换回 = 导致值与分隔符混淆，见 testLiteralEqualsEncoded）。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class UrlUtilsP2FixTest {

	@Test
	public void testFragmentPreserved() {
		String out = UrlUtils.encodeUrl("https://ex.com/a?q=hello world#frag");
		assertThat(out)
				.as("#fragment 不得丢失(修复前实测整段消失)")
				.endsWith("#frag");
		assertThat(out).contains("q=hello%20world");
	}

	@Test
	public void testUserInfoPreserved() {
		String out = UrlUtils.encodeUrl("https://u:p@ex.com/p");
		assertThat(out)
				.as("用户名密码不得丢弃(修复前实测 https://ex.com/p)")
				.startsWith("https://u:p@ex.com");
	}

	@Test
	public void testNoDoubleEncodingOfPercentEscapes() {
		//修复前实测: 原查询串里已编码的 %20 被二次编码成 %2520
		String out = UrlUtils.encodeUrl("https://ex.com/s?q=a%20b");
		assertThat(out).contains("q=a%20b");
		assertThat(out).doesNotContain("%2520");
	}

	@Test
	public void testLiteralEqualsEncodedInValue() {
		//行为变化: 值内部的字面 = 编码为 %3D(修复前 %3D 被无条件换回 =, 分隔结构混淆)
		String out = UrlUtils.encodeUrl("q=a=b");
		assertThat(out).isEqualTo("q=a%3Db");
	}

	@Test
	public void testElasticsearchStyleQueryUnchangedSemantics() {
		//项目内典型用法(ES 查询串): 键值结构与 :>= 的转义结果保持既有形态
		String out = UrlUtils.encodeUrl("q=year:>=1980&sort=year:asc");
		assertThat(out).isEqualTo("q=year%3A%3E%3D1980&sort=year%3Aasc");
	}

	@Test
	public void testFullUrlComponentsAllKept() {
		String out = UrlUtils.encodeUrl(
				"https://user:pass@www.example.com:8443/api/data/search?q=hello world&lang=en#a b");
		assertThat(out).isEqualTo(
				"https://user:pass@www.example.com:8443/api/data/search?q=hello%20world&lang=en#a%20b");
	}
}
