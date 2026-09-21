package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * uniqueKey 行为契约测试（评审发现: 旧实现走 RandomStringUtils.randomAlphanumeric,
 * 其随机源是 ThreadLocalRandom——可并发但不属密码学安全的随机数发生器, 不适合用于
 * 会话令牌/验证码这类安全敏感的取值）.
 * <p>
 * 随机源强度无法用单测统计区分(ThreadLocalRandom 与 SecureRandom 都均匀分布),
 * 修复的落地验证 = 本契约测试(长度/字符集/不重复, 保证调用方兼容) + javap 反编译确认
 * uniqueKey 字节码不再引用 RandomStringUtils(见评审报告修复记录).
 *
 * @author Rico Yu  ricoyu520@gmail.com
 */
class StringUtilsUniqueKeyTest {
	
	@Test
	void lengthAndAlphabetPreserved() {
		for (int len : new int[]{4, 12, 36, 66, 99}) {
			String key = StringUtils.uniqueKey(len);
			assertEquals(len, key.length(), "长度必须等于入参");
			assertTrue(key.matches("[0-9A-Za-z]+"), "字符集必须是大小写字母+数字(与旧行为一致), 实际=" + key);
		}
		assertEquals(99, StringUtils.uniqueKey().length(), "无参版本长度 99 不变");
	}
	
	@Test
	void consecutiveKeysAreDistinct() {
		Set<String> keys = new HashSet<>();
		for (int i = 0; i < 2000; i++) {
			keys.add(StringUtils.uniqueKey(16));
		}
		//16位62字符空间, 2000次两两相同是巧合级别(概率约千万分之三), 全同则随机源退化成定值
		assertTrue(keys.size() >= 1999, "生成值几乎全部相同说明随机源异常, distinct=" + keys.size());
	}
}
