package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * randomStr 行为契约测试（2026-09-27 重扫发现: 旧实现每次调用 new java.util.Random()——
 * 线性同余发生器, 观测到足够输出可反推内部状态预测后续值, 不适合验证码/Token 这类凭证;
 * 且 nextFloat 缩放乘法有上界取整偏差）。
 * <p>
 * 修复后复用类内已有的静态 SecureRandom 共享实例。随机源强度无法用单测统计区分,
 * 验证方式 = 本契约测试(长度/字符集/不重复, 保证调用方兼容) + javap 反编译确认
 * randomStr 字节码不再引用 java/util/Random(修复记录附输出)。
 *
 * @author Rico Yu  ricoyu520@gmail.com
 */
class StringUtilsRandomStrTest {

	@Test
	void lengthAndAlphabetPreserved() {
		for (int len : new int[]{1, 8, 32, 128}) {
			String s = StringUtils.randomStr(len);
			assertEquals(len, s.length(), "长度必须等于入参");
			assertTrue(s.matches("[a-z]+"), "字符集必须是 a-z(与旧行为一致), 实际=" + s);
		}
	}

	@Test
	void consecutiveStringsAreDistinct() {
		Set<String> set = new HashSet<>();
		for (int i = 0; i < 2000; i++) {
			set.add(StringUtils.randomStr(12));
		}
		//12位26字符空间, 2000次出现一对重复的概率约百万分之几; 全同说明随机源退化成定值
		assertTrue(set.size() >= 1999, "生成值几乎全部相同说明随机源退化, 去重后仅 " + set.size());
	}
}
