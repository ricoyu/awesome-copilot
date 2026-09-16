package com.awesomecopilot.networking;

import com.awesomecopilot.networking.enums.Scheme;
import com.awesomecopilot.networking.matcher.IpAddressMatcher;
import com.awesomecopilot.networking.utils.DomainUtils;
import com.awesomecopilot.networking.utils.IPUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 2026-09-16 外围工具类修复（评审报告 P1-8/P1-9/P2-11/P2-12）的行为验证测试。
 * 每个用例注释标明修复前的错误行为, 保证它们真能因回退而变红。
 * <p>
 * Copyright: (C), 2026-09-16
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class PeripheralFixesTest {

	// ==================== P1-8 IpAddressMatcher 不再被 DNS 解析结果骗过 ====================

	/**
	 * 修复前：parseAddress 直接把入参喂给 InetAddress.getByName——空串被 JDK 解析为
	 * 回环地址, 实测 matches("") 对 "127.0.0.1" 白名单返回 true。修复后必须先过
	 * IP 字面量校验, 空串/域名一律不匹配。
	 */
	@Test
	public void testEmptyAndHostnameNoLongerHitLocalhostWhitelist() {
		IpAddressMatcher matcher = new IpAddressMatcher("127.0.0.1");
		assertThat(matcher.matches("")).isFalse();
		assertThat(matcher.matches("localhost")).isFalse();
		// 真实字面量仍然正常匹配(别把功能修丢)
		assertThat(matcher.matches("127.0.0.1")).isTrue();
	}

	/**
	 * 修复前 "127.1"(JDK 接受的缩写十进制)能通过, 而 IPUtils.isValidIpV4("127.1")
	 * 判非法——两处标准矛盾。修复后统一到 IPUtils 标准: 缩写形式拒绝。
	 */
	@Test
	public void testAbbreviatedDecimalRejectedConsistently() {
		IpAddressMatcher subnet = new IpAddressMatcher("127.0.0.1/24");
		assertThat(IPUtils.isValidIpV4("127.1")).isFalse();
		assertThat(subnet.matches("127.1")).isFalse();
		// 同网段的规范写法仍然命中
		assertThat(subnet.matches("127.0.0.66")).isTrue();
	}

	/**
	 * 白名单配置串本身也拒绝域名：修复前 new IpAddressMatcher("evil.example.com")
	 * 会当场做 DNS 解析, 解析到哪个 IP 白名单就算配置成功——配置正确性交给了解析器。
	 */
	@Test
	public void testDomainConfigRejectedAtConstruction() {
		assertThatThrownBy(() -> new IpAddressMatcher("localhost"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// ==================== P2-12 掩码配置校验 ====================

	/**
	 * 修复前 "192.168.0.1/abc" 构造时抛 NumberFormatException(消息与"掩码非法"无关),
	 * "192.168.0.1/-1" 则不报错地退化成精确匹配。现在构造期给出明确原因。
	 */
	@Test
	public void testInvalidMaskRejectedWithClearMessage() {
		assertThatThrownBy(() -> new IpAddressMatcher("192.168.0.1/abc"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("掩码");
		assertThatThrownBy(() -> new IpAddressMatcher("192.168.0.1/-1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("掩码");
		assertThatThrownBy(() -> new IpAddressMatcher("192.168.0.1/33"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("0~32");
		// 合法值照常工作
		assertThat(new IpAddressMatcher("192.168.0.1/24").matches("192.168.0.99")).isTrue();
	}

	// ==================== P1-9 DomainUtils 重写后的行为 ====================

	/**
	 * 修复①：\w 不吃连字符, getDomain("my-site.com", 1) 修复前返回 "site.com"。
	 */
	@Test
	public void testHyphenatedDomainExtractedCorrectly() {
		assertThat(DomainUtils.getDomain("my-site.com", 1)).isEqualTo("my-site.com");
		assertThat(DomainUtils.getDomain("www.my-site.com.cn", 1)).isEqualTo("my-site.com.cn");
	}

	/**
	 * 修复②：嵌套量词导致的指数回溯——修复前 401 字符要 6 秒+。
	 * 重写为纯字符串切分后, 800 字符的恶意输入必须在毫秒级完成。
	 * 给 2 秒上限：正常实现 <10ms, 回退成旧正则则远超 2s。
	 */
	@Test
	public void testLongAdversarialInputCompletesFast() {
		String evil = "a".repeat(800) + "!";
		long t0 = System.nanoTime();
		String r = DomainUtils.getDomain(evil, 3);
		long ms = (System.nanoTime() - t0) / 1_000_000;
		assertThat(r).isEqualTo("");
		assertThat(ms).isLessThan(2000L);
	}

	/**
	 * 原有语义不回退：常见层级与组合后缀(com.cn)的一/二/三级提取结果与旧正则一致,
	 * 完整 URL 输入也能取到域名(新增能力, 修复前只接受纯主机名)。
	 */
	@Test
	public void testNormalDomainLevelsUnchanged() {
		assertThat(DomainUtils.getDomain("www.baidu.com", 1)).isEqualTo("baidu.com");
		// 旧正则 level=2 取"后缀左侧再2段": dnssec.example.com
		assertThat(DomainUtils.getDomain("xcc.stage.710162.server.dnssec.example.com", 2))
				.isEqualTo("dnssec.example.com");
	}

	/**
	 * 层级语义明确写死：level=1 取"主域名+后缀", level=2 再多带一段子域。
	 */
	@Test
	public void testLevelSemanticsExplicit() {
		String host = "a.b.example.com";
		assertThat(DomainUtils.getDomain(host, 1)).isEqualTo("example.com");
		assertThat(DomainUtils.getDomain(host, 2)).isEqualTo("b.example.com");
		assertThat(DomainUtils.getDomain(host, 3)).isEqualTo("a.b.example.com");
		assertThat(DomainUtils.getDomain("http://" + host + ":8080/x?q=1", 2))
				.isEqualTo("b.example.com");
	}

	// ==================== P2-11 Scheme.of / 掩码前导零 ====================

	/**
	 * Scheme.of 修复前是实例方法(无法静态调用)且解析失败返回 null。
	 * 现在 Scheme.of("https") 直接可用, 坏输入抛 IllegalArgumentException。
	 */
	@Test
	public void testSchemeOfIsStaticAndFailsLoud() {
		assertThat(Scheme.of("https")).isEqualTo(Scheme.HTTPS);
		assertThat(Scheme.of("HTTP")).isEqualTo(Scheme.HTTP);
		assertThatThrownBy(() -> Scheme.of("ftp"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	/**
	 * 掩码前导零双标修复：192.168.0.1/032 此前被 Integer.parseInt 放行,
	 * 而 v6 侧 /064 被正则拒绝。现在两侧都不接受前导零。
	 */
	@Test
	public void testLeadingZeroMaskRejectedBothSides() {
		assertThat(IPUtils.isValidIpV4("192.168.0.1/032")).isFalse();
		assertThat(IPUtils.isValidIpV6("fe80:0:0:0:0:0:c0a8:1/064")).isFalse();
		// 规范写法不受影响
		assertThat(IPUtils.isValidIpV4("192.168.0.1/32")).isTrue();
	}
}
