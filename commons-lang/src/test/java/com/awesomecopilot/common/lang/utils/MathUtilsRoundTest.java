package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-18 回归测试（CODE_REVIEW_REPORT 2026-09-22）：MathUtils 用 new BigDecimal(double)
 * 做"精确运算"。double 字面量存的是二进制近似值（0.1 实际是 0.1000000000000000055...），
 * new BigDecimal(2.675) 取到的是 2.67499999...，四舍五入后恒比期望少一分。
 * <p>
 * 修复方式：double 入口一律 BigDecimal.valueOf(v)（先走 Double.toString 的十进制转换）。
 * 同文件 roundUpTwo(new BigDecimal("2.675"))=2.68 与 round(2.675,2)=2.67 互相矛盾，
 * 修复后两者应一致。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class MathUtilsRoundTest {

	@Test
	public void testRoundHalfUpOnDecimalLiteral() {
		//修复前实测: round(2.675, 2) = 2.67(少一分), 与 roundUpTwo(new BigDecimal("2.675"))=2.68 矛盾
		assertThat(MathUtils.round(2.675, 2)).isEqualTo(2.68d);
		assertThat(MathUtils.format(2.675, 2)).isEqualTo("2.68");
		assertThat(MathUtils.formatDouble(2.675, 2)).isEqualTo(2.68d);
	}

	@Test
	public void testMulPrecision() {
		//修复前实测: mul(1.1, 1.1, 20) = 1.2100000000000002
		assertThat(MathUtils.mul(1.1, 1.1, 20)).isEqualTo(1.21d);
	}

	@Test
	public void testAddSubDivConsistentWithDecimalView() {
		assertThat(MathUtils.add(0.1, 0.2, 2)).isEqualTo(0.3d);
		assertThat(MathUtils.sub(0.3, 0.1, 2)).isEqualTo(0.2d);
		assertThat(MathUtils.div(1.0, 3.0, 2)).isEqualTo(0.33d);
	}

	@Test
	public void testNormalValuesUnchanged() {
		//普通取值不能因修复而漂移
		assertThat(MathUtils.round(2.567, 2)).isEqualTo(2.57d);
		assertThat(MathUtils.round(2.0, 0)).isEqualTo(2.0d);
		assertThat(MathUtils.mul(2.0, 3.0, 2)).isEqualTo(6.0d);
	}
}
