package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * P2-22 StringUtils 四方法回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <ul>
 * <li>format(template, Object...): 修复前只有 Long 被转字符串绕开 MessageFormat 本地化,
 * 实测 format("计数:{0}", 1234) 输出 "计数:1,234"(Integer、BigDecimal 被加千分位)——
 * 拼订单号/ID 时数字被改写。现 Number 统一 toString();</li>
 * <li>lastN: 修复前实测 lastN("abc",5) 抛 StringIndexOutOfBoundsException: Range [-2, 3),
 * n 为负同理。现 n&lt;=0 返回空串、n&gt;=长度返回原串(与同类 subStr 的宽容约定一致);</li>
 * <li>padStringWithZeros: 名字叫"补零"实测却是右补零(padStringWithZeros("123",6)
 * 返回 123000, 数值放大 1000 倍), 常见需求是左补。仓库内零调用, 改为左补(数值不变);</li>
 * <li>removeAllQuotes: 修复前字符类同时匹配双引号和反斜杠,
 * 实测 removeAllQuotes("C:\\data\\x") 返回 "C:datax"(Windows 路径分隔符全丢)。
 * 现只删引号(转义引号与未转义引号), 不动其余反斜杠。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class StringUtilsP2FixTest {

	// ---------------- format: Number 不再被加千分位 ----------------

	@Test
	public void testFormatIntegerHasNoThousandsSeparator() {
		//修复前实测: "计数:1,234"
		assertThat(StringUtils.format("计数:{0}", 1234))
				.as("Integer 不得被 MessageFormat 加千分位(修复前实测 计数:1,234)")
				.isEqualTo("计数:1234");
	}

	@Test
	public void testFormatBigDecimalHasNoThousandsSeparator() {
		assertThat(StringUtils.format("金额:{0}", new BigDecimal("1234.56")))
				.as("BigDecimal 同样走 toString(修复前实测 1,234.56)")
				.isEqualTo("金额:1234.56");
	}

	@Test
	public void testFormatLongStillExact() {
		//原有的 Long 特判行为要保持(订单号/ID 场景)
		assertThat(StringUtils.format("id:{0}", 1234567890123L)).isEqualTo("id:1234567890123");
	}

	@Test
	public void testFormatStringAndNullValues() {
		//MessageFormat 对 null 参数输出字面 "null"(JDK 行为, 本条不改, 仅记录)
		assertThat(StringUtils.format("{0}={1}", "a", null)).isEqualTo("a=null");
	}

	// ---------------- lastN: 越界不抛 ----------------

	@Test
	public void testLastNOutOfRangeReturnsWholeString() {
		//修复前实测抛 StringIndexOutOfBoundsException: Range [-2, 3)
		assertThatCode(() -> StringUtils.lastN("abc", 5)).doesNotThrowAnyException();
		assertThat(StringUtils.lastN("abc", 5)).isEqualTo("abc");
		assertThat(StringUtils.lastN("abc", 3)).isEqualTo("abc");
	}

	@Test
	public void testLastNNonPositiveReturnsEmpty() {
		assertThat(StringUtils.lastN("abc", 0)).isEqualTo("");
		assertThat(StringUtils.lastN("abc", -2))
				.as("n 为负返回空串(修复前同样抛越界异常)")
				.isEqualTo("");
	}

	@Test
	public void testLastNNormalCase() {
		assertThat(StringUtils.lastN("abcdef", 2)).isEqualTo("ef");
		assertThat(StringUtils.lastN(null, 2)).isNull();
	}

	// ---------------- padStringWithZeros: 左补零 ----------------

	@Test
	public void testPadStringWithZerosPadsOnLeft() {
		//修复前实测 "123"→"123000"(右补, 数值放大 1000 倍); 现左补, 数值不变
		assertThat(StringUtils.padStringWithZeros("123", 6))
				.as("补零应在左侧(修复前实测右补得到 123000)")
				.isEqualTo("000123");
	}

	@Test
	public void testPadStringWithZerosNoOpWhenLongEnough() {
		assertThat(StringUtils.padStringWithZeros("1234567", 3)).isEqualTo("1234567");
		assertThat(StringUtils.padStringWithZeros("123", 3)).isEqualTo("123");
	}

	// ---------------- removeAllQuotes: 不删反斜杠 ----------------

	@Test
	public void testRemoveAllQuotesKeepsBackslashes() {
		//修复前实测: "C:\data\x" → "C:datax"(反斜杠被一起删了)
		assertThat(StringUtils.removeAllQuotes("C:\\data\\x"))
				.as("Windows 路径分隔符不得丢失(修复前实测返回 C:datax)")
				.isEqualTo("C:\\data\\x");
	}

	@Test
	public void testRemoveAllQuotesRemovesBothQuoteForms() {
		assertThat(StringUtils.removeAllQuotes("\"quoted\"")).isEqualTo("quoted");
		assertThat(StringUtils.removeAllQuotes("he said \\\"hi\\\"")).isEqualTo("he said hi");
		assertThat(StringUtils.removeAllQuotes("路径 \"C:\\x\" 结束"))
				.as("引号删除、反斜杠保留")
				.isEqualTo("路径 C:\\x 结束");
	}

	@Test
	public void testRemoveAllQuotesNullSafe() {
		assertThat(StringUtils.removeAllQuotes(null)).isNull();
	}
}
