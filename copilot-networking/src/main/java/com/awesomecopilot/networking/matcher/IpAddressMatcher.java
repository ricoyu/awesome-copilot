package com.awesomecopilot.networking.matcher;

import com.awesomecopilot.networking.utils.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Matches a request based on IP Address or subnet mask matching against the remote
 * address.
 * <p>
 * Both IPv6 and IPv4 addresses are supported, but a matcher which is configured with an
 * IPv4 address will never match a request which returns an IPv6 address, and vice-versa.
 * <p>
 * 2026-09-16 按《copilot-networking/评审报告.md》P1-8/P2-12 修订：
 * ① 查询地址先做 IP 字面量校验再解析, 不再把任意字符串喂给 InetAddress.getByName
 *   ——旧实现会拿 "localhost" 甚至空串去做真 DNS 解析（实测 matches("") 对
 *   127.0.0.1 白名单返回 true）, 白名单可以被"注册一个解析到内网地址的域名"绕过;
 * ② 配置串的掩码校验范围（IPv4 0~32, IPv6 0~128）, 不再让 "/-1" 不声不响地
 *   退化成精确匹配、"192.168.0.1/abc" 到运行期才抛 NumberFormatException;
 * ③ 解析失败拒绝时记 debug 日志, 不再不留痕迹。
 *
 * @author Luke Taylor
 * @since 3.0.2
 */
public final class IpAddressMatcher implements RequestMatcher {

	private static final Logger log = LoggerFactory.getLogger(IpAddressMatcher.class);

	private final int nMaskBits;
	private final InetAddress requiredAddress;

	/**
	 * Takes a specific IP address or a range specified using the IP/Netmask (e.g.
	 * 192.168.1.0/24 or 202.24.0.0/14).
	 * <p>
	 * 配置串本身也必须是 IP 字面量: 白名单写成域名时, 它解析成什么完全取决于 DNS,
	 * 攻击面与查询侧相同, 因此同样在构造期拒绝。
	 *
	 * @param ipAddress the address or range of addresses from which the request must
	 *                  come.
	 */
	public IpAddressMatcher(String ipAddress) {
		Assert.notNull(ipAddress, "ipAddress cannot be null");
		String addressPart = ipAddress;
		int mask = -1;

		if (ipAddress.indexOf('/') > 0) {
			String[] addressAndMask = StringUtils.split(ipAddress, "/");
			// "a/b/c" 这类畸形串 split 出三段; 配置错误要在启动时立即报错说清原因
			Assert.isTrue(addressAndMask != null && addressAndMask.length == 2,
					"非法的IP段配置(应形如 192.168.1.0/24): " + ipAddress);
			addressPart = addressAndMask[0];
			String maskText = addressAndMask[1];
			Assert.isTrue(!maskText.isEmpty() && maskText.chars().allMatch(Character::isDigit),
					"掩码必须是非负数字: " + ipAddress);
			mask = Integer.parseInt(maskText);
			// IPv4 掩码上限 32, IPv6 上限 128; 按地址形态判断上限
			boolean looksIpv6 = addressPart.indexOf(':') >= 0;
			int max = looksIpv6 ? 128 : 32;
			Assert.isTrue(mask <= max, "掩码应在 0~" + max + " 之间: " + ipAddress);
		}
		Assert.isTrue(isIpLiteral(addressPart),
				"白名单必须是IP字面量(不允许域名等需要DNS解析的形式): " + ipAddress);
		requiredAddress = parseAddress(addressPart);
		nMaskBits = mask;
		Assert.isTrue(requiredAddress.getAddress().length * 8 >= nMaskBits,
				"IP 地址长度太短");
	}

	public boolean matches(HttpServletRequest request) {
		return matches(request.getRemoteAddr());
	}

	public boolean matches(String address) {
		/*
		 * P1-8 核心修复：先验证是 IP 字面量。IPUtils 的正则只接受点分四段/标准 IPv6,
		 * 因此拒绝了 ""(旧实现把它解析为回环地址)、"localhost"(走DNS)、
		 * "127.1"(JDK 接受缩写十进制, 而 IPUtils.isValidIpV4 判定非法——两处标准
		 * 不一致, 现在统一到 IPUtils 的标准)。
		 */
		if (address == null || !isIpLiteral(address)) {
			log.debug("拒绝非IP字面量的查询地址: {}", address);
			return false;
		}
		InetAddress remoteAddress;
		try {
			remoteAddress = parseAddress(address);
		} catch (IllegalArgumentException e) {
			// P2-12: 不再不留痕迹, 记录被拒地址
			log.debug("IP地址解析失败: {}", address, e);
			return false;
		}

		if (!requiredAddress.getClass().equals(remoteAddress.getClass())) {
			return false;
		}

		if (nMaskBits < 0) {
			return remoteAddress.equals(requiredAddress);
		}

		byte[] remAddr = remoteAddress.getAddress();
		byte[] reqAddr = requiredAddress.getAddress();

		int nMaskFullBytes = nMaskBits / 8;
		byte finalByte = (byte) (0xFF00 >> (nMaskBits & 0x07));

		for (int i = 0; i < nMaskFullBytes; i++) {
			if (remAddr[i] != reqAddr[i]) {
				return false;
			}
		}

		if (finalByte != 0) {
			return (remAddr[nMaskFullBytes] & finalByte) == (reqAddr[nMaskFullBytes] & finalByte);
		}

		return true;
	}

	/**
	 * 是纯 IPv4 或 IPv6 字面量吗(不含 /掩码 后缀)。
	 */
	private static boolean isIpLiteral(String address) {
		return IPUtils.isValidIpV4(address) || IPUtils.isValidIpV6(address);
	}

	private InetAddress parseAddress(String address) {
		try {
			return InetAddress.getByName(address);
		}
		catch (UnknownHostException e) {
			throw new IllegalArgumentException("Failed to parse address " + address, e);
		}
	}
}
