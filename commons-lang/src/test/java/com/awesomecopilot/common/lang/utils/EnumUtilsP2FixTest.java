package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * P2-25 EnumUtils 回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <ul>
 * <li>lookup(Class,String): 修复前空串/null 抛 IllegalArgumentException, 而按属性匹配的
 * 姊妹重载 lookup(Class,String,String) 对空串返回 null——表单没填时一个给 null 一个给
 * 异常, 调用方无所适从。统一返回 null;</li>
 * <li>lookupEnum 的 Long/BigInteger 属性匹配分支: 修复前 value.intValue() ==
 * propertyValue.intValue() 比较——实测 4294967297L(=2^32+1) 与 1L 的 intValue 都是 1,
 * 会命中错误枚举。现 longValue()/equals 比较。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class EnumUtilsP2FixTest {

	public enum LongCode {
		ONE(1L),
		HUGE(4294967297L); // 2^32 + 1, intValue() 截断后与 1 相同

		private final long code;

		LongCode(long code) {
			this.code = code;
		}
	}

	public enum BigIntegerCode {
		ONE(1L),
		HUGE(4294967297L);

		private final BigInteger code;

		BigIntegerCode(long code) {
			this.code = BigInteger.valueOf(code);
		}
	}

	// ---------------- lookup(Class,String) 空串契约 ----------------

	@Test
	public void testLookupEnumEmptyNameReturnsNullNotThrow() {
		//修复前实测: lookupEnum(DayOfWeek.class, "") 抛 IllegalArgumentException
		assertThatCode(() -> EnumUtils.lookupEnum(DayOfWeek.class, ""))
				.doesNotThrowAnyException();
		DayOfWeek empty = EnumUtils.lookupEnum(DayOfWeek.class, "");
		assertThat(empty)
				.as("空串与姊妹重载(按属性匹配)行为一致: 返回 null")
				.isNull();
		//正常查找不受影响
		DayOfWeek monday = EnumUtils.lookupEnum(DayOfWeek.class, "MONDAY");
		assertThat(monday).isEqualTo(DayOfWeek.MONDAY);
	}

	// ---------------- Long 属性比较不再 intValue 截断 ----------------

	@Test
	public void testLookupEnumByLongPropertyNoIntTruncation() {
		//修复前实测: lookupEnum(LongCode.class, 4294967297L, "code") 命中 ONE
		//(4294967297 与 1 的 intValue 都是 1, 且 ONE 声明在前)
		LongCode huge = EnumUtils.lookupEnum(LongCode.class, 4294967297L, "code");
		assertThat(huge)
				.as("4294967297L 必须命中 HUGE(修复前误命中 ONE)")
				.isEqualTo(LongCode.HUGE);
		LongCode one = EnumUtils.lookupEnum(LongCode.class, 1L, "code");
		assertThat(one).isEqualTo(LongCode.ONE);
	}

	// ---------------- BigInteger 属性比较不再 intValue 截断 ----------------

	@Test
	public void testLookupEnumByBigIntegerPropertyNoIntTruncation() {
		BigIntegerCode huge = EnumUtils.lookupEnum(BigIntegerCode.class, BigInteger.valueOf(4294967297L), "code");
		assertThat(huge)
				.as("BigInteger 4294967297 必须命中 HUGE(修复前误命中 ONE)")
				.isEqualTo(BigIntegerCode.HUGE);
		BigIntegerCode one = EnumUtils.lookupEnum(BigIntegerCode.class, BigInteger.ONE, "code");
		assertThat(one).isEqualTo(BigIntegerCode.ONE);
	}
}
