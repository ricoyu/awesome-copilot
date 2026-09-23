package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-23 MathUtils 数值簇回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <ul>
 * <li>toDouble("abc", true): 修复前实测抛 NullPointerException——内层 toDouble 对
 * 不可转换值返回 null, 0 - null 拆箱直接 NPE。现返回 null;</li>
 * <li>format2Currency: 修复前模式 ",000" 把整数部分补足 3 位, 实测 (5,2)="005.00"、
 * (99,2)="099.00"。现模式 ",##0" 组千分位但不补前导零: (5,2)="5.00";</li>
 * <li>div(v1,v2,precision): 修复前除数为 0 抛 ArithmeticException 但 javadoc 未写。
 * 现 javadoc 写明契约, 行为保持(不改成返回 null——除零是调用错误, 返回 null 会让脏数据继续传播);</li>
 * <li>round(v, precision): 修复前缺 precision&lt;0 校验(同文件 formatDouble 有),
 * round(1.55,-1) 返回 0.0 且不报错。现抛 IllegalArgumentException;</li>
 * <li>toInteger(3000000000L): 修复前 intValue() 截断返回 -1294967296 不报错。
 * 现溢出抛 ArithmeticException(intValueExact);</li>
 * <li>equals(Long,Long): javadoc 表写"都为 null 返回 false", 实现返回 true——改注释
 * (longEqual 的注释与实现一致, 说明这份是抄错的)。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class MathUtilsP2FixTest {

	// ---------------- toDouble(Object, boolean) NPE ----------------

	@Test
	public void testToDoubleNegativeOfUnconvertibleReturnsNull() {
		//修复前实测: toDouble("abc", true) 抛 NPE(0 - null 拆箱)
		assertThat(MathUtils.toDouble("abc", true))
				.as("不可转换值 toNegative=true 应返回 null(修复前实测 NPE)")
				.isNull();
		assertThat(MathUtils.toDouble("abc")).isNull();
	}

	@Test
	public void testToDoubleNegativeStillNegates() {
		assertThat(MathUtils.toDouble("3.5", true)).isEqualTo(-3.5d);
		assertThat(MathUtils.toDouble(2.0, false)).isEqualTo(2.0d);
		assertThat(MathUtils.toDouble(null, true)).isNull();
	}

	// ---------------- format2Currency 前导零 ----------------

	@Test
	public void testFormat2CurrencyNoLeadingZeroPadding() {
		//修复前实测: (5,2)="005.00", (99,2)="099.00"(模式 ",000" 补足 3 位整数)
		assertThat(MathUtils.format2Currency(new BigDecimal("5"), 2))
				.as("5 元应输出 5.00(修复前实测 005.00)")
				.isEqualTo("5.00");
		assertThat(MathUtils.format2Currency(new BigDecimal("99"), 2)).isEqualTo("99.00");
		//正常量级与千分位分组不受影响
		assertThat(MathUtils.format2Currency(new BigDecimal("12345.5"), 2)).isEqualTo("12,345.50");
	}

	@Test
	public void testFormat2CurrencyZeroAndNull() {
		assertThat(MathUtils.format2Currency(BigDecimal.ZERO, 2)).isEqualTo("0.00");
		assertThat(MathUtils.format2Currency(null, 2)).isEqualTo("0.00"); //null 归零契约不变
	}

	// ---------------- div 除零契约 ----------------

	@Test
	public void testDivByZeroThrowsAndIsDocumented() {
		//行为保持: 除数为 0 抛 ArithmeticException(javadoc 补契约说明)
		assertThatThrownBy(() -> MathUtils.div(1.0, 0.0, 2))
				.isInstanceOf(ArithmeticException.class);
		//非零除数正常
		assertThat(MathUtils.div(1.0, 3.0, 2)).isEqualTo(0.33d);
	}

	// ---------------- round precision 校验 ----------------

	@Test
	public void testRoundRejectsNegativePrecision() {
		//修复前实测: round(1.55,-1) 返回 0.0 不报错; 同文件 formatDouble 却有校验
		assertThatThrownBy(() -> MathUtils.round(1.55, -1))
				.as("precision<0 应抛 IllegalArgumentException(与 formatDouble 风格一致)")
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(MathUtils.round(1.55, 2)).isEqualTo(1.55d);
	}

	// ---------------- toInteger 溢出 ----------------

	@Test
	public void testToIntegerLongOverflowThrows() {
		//修复前实测: toInteger(3000000000L) 返回 -1294967296, 截断后不报错
		assertThatThrownBy(() -> MathUtils.toInteger(3000000000L))
				.as("超出 int 范围的 Long 必须报错(修复前实测截断成负数且不报错)")
				.isInstanceOf(ArithmeticException.class);
		//正常范围不受影响
		assertThat(MathUtils.toInteger(300L)).isEqualTo(300);
		assertThat(MathUtils.toInteger("42")).isEqualTo(42);
		assertThat(MathUtils.toInteger(null)).isNull();
		assertThat(MathUtils.toInteger("abc")).isNull(); //字符串解析失败仍返回 null(原契约)
	}

	// ---------------- equals(Long,Long) 注释与实现一致 ----------------

	@Test
	public void testEqualsBothNullReturnsTrue() {
		//实现返回 true; 修复前 javadoc 表写 false(抄错), 现注释已改为与实现一致——
		//这里锁定实现行为, 注释正确性人工核对
		assertThat(MathUtils.equals((Long) null, (Long) null)).isTrue();
		assertThat(MathUtils.equals(Long.valueOf(1L), Long.valueOf(1L))).isTrue();
		assertThat(MathUtils.equals((Long) null, Long.valueOf(1L))).isFalse();
	}
}
