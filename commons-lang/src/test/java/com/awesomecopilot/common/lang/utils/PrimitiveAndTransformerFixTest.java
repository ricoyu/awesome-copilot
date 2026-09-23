package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.bean.UrlParts;
import com.awesomecopilot.common.lang.transformer.Transformers;
import com.awesomecopilot.common.lang.transformer.ValueHandlerFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基础类型/转换器组回归测试（CODE_REVIEW_REPORT P1-4 / P1-5 / P1-6 / P1-7 / P1-15 / P1-17）。
 * <p>
 * P1-4  DoubleValueHandler 判了 Float 却按 Double 强转 → ClassCastException
 * P1-5  UrlParts.paramMap 用 split("=") 且要求长度==2 → 值含 = 或空值参数被整条丢弃
 * P1-6  RegexUtils URL 正则端口 \d{2,} → 个位数端口(http://localhost:8)解析错乱
 * P1-7  PrimitiveUtils 漏 Double.TYPE；Character 分支把 Character.TYPE 抄成 Short.TYPE（三处）
 * P1-15 toBigDecimal：BigInteger 走 intValue 溢出、double/float 返回 0、小数文本抛异常
 * P1-17 BooleanValueHandler 用 Boolean.getBoolean(系统属性查询) 解析字符串 "true" → false 且不报错
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class PrimitiveAndTransformerFixTest {

	// ---------- P1-4 ----------

	@Test
	public void testConvertFloatToDouble() {
		//修复前实测: ClassCastException: Float cannot be cast to Double
		assertThat(ValueHandlerFactory.convert(1.5f, Double.class)).isEqualTo(1.5d);
		assertThat(Transformers.convert(2.25f, Double.class)).isEqualTo(2.25d);
	}

	// ---------- P1-5 ----------

	@Test
	public void testParamMapKeepsValuesContainingEqualsSign() {
		UrlParts parts = new UrlParts();
		//修复前实测: 只剩 {x=[1]}——token 被切成3段跳过、flag 切成1段跳过
		parts.setParams("token=abc=def&flag=&x=1");

		Object token = parts.paramMap().get("token");
		assertThat(parts.paramMap().containsKey("token")).as("token=abc=def 不应被丢弃").isTrue();
		assertThat(((java.util.List) token).get(0)).isEqualTo("abc=def");

		assertThat(parts.paramMap().containsKey("flag")).as("空值参数 flag= 不应被丢弃").isTrue();
		Object flag = ((java.util.List) parts.paramMap().get("flag")).get(0);
		assertThat(flag).isEqualTo("");

		assertThat(((java.util.List) parts.paramMap().get("x")).get(0)).isEqualTo("1");
	}

	// ---------- P1-6 ----------

	@Test
	public void testTeardownSingleDigitPort() {
		//修复前实测: http://localhost:8/app → port=80(走了默认分支)、path=":8/app"
		UrlParts parts = RegexUtils.teardown("http://localhost:8/app");
		assertThat(parts).isNotNull();
		assertThat(parts.getPort()).isEqualTo(8);
		assertThat(parts.getPath()).isEqualTo("/app");

		//合法端口不能被改坏
		UrlParts full = RegexUtils.teardown("https://www.google.com:8443/dir/search.html?arg=0-a#hash");
		assertThat(full.getPort()).isEqualTo(8443);
		assertThat(full.getPath()).isEqualTo("/dir/search.html");

		//无端口时的默认值语义保持
		UrlParts noPort = RegexUtils.teardown("http://example.com/x");
		assertThat(noPort.getPort()).isEqualTo(80);
		UrlParts noPortHttps = RegexUtils.teardown("https://example.com/x");
		assertThat(noPortHttps.getPort()).isEqualTo(443);
	}

	// ---------- P1-7 ----------

	@Test
	public void testToPrimitivePrimitiveTypes() {
		//修复前实测: double.class 与 char.class 都返回 null
		double d = PrimitiveUtils.toPrimitive("1.5".getBytes(StandardCharsets.UTF_8), double.class);
		assertThat(d).isEqualTo(1.5d);
		char c = PrimitiveUtils.toPrimitive("x".getBytes(StandardCharsets.UTF_8), char.class);
		assertThat(c).isEqualTo('x');

		double d2 = PrimitiveUtils.toPrimitive((Object) "2.5", double.class);
		assertThat(d2).isEqualTo(2.5d);
		char c2 = PrimitiveUtils.toPrimitive((Object) "y", char.class);
		assertThat(c2).isEqualTo('y');

		//isPrimitive 形参是值实例(内部取 value.getClass()), 不是 Class 对象
		//修复前该分支的笔误(Character.class || Short.TYPE)恰好被 Character 实例命中而未出错,
		//这里连跑三个实例锁住"修正后行为不变"
		assertThat(PrimitiveUtils.isPrimitive('x')).isTrue();
		assertThat(PrimitiveUtils.isPrimitive((short) 1)).isTrue();
		assertThat(PrimitiveUtils.isPrimitive(1.5d)).isTrue();
		assertThat(PrimitiveUtils.isPrimitive("str")).isFalse();
	}

	// ---------- P1-15 ----------

	@Test
	public void testToBigDecimalAcrossTypes() {
		//修复前实测: BigInteger 3000000000 走 intValue 返回 -1294967296
		assertThat(PrimitiveUtils.toBigDecimal(new BigInteger("3000000000")))
				.isEqualTo(new BigDecimal("3000000000"));
		//修复前实测: 3.9 返回 0（没有 Double/Float 分支）
		assertThat(PrimitiveUtils.toBigDecimal(3.9d)).isEqualTo(new BigDecimal("3.9"));
		assertThat(PrimitiveUtils.toBigDecimal(1.25f)).isEqualTo(new BigDecimal("1.25"));
		//修复前实测: "1.5" 抛 NumberFormatException（用 Integer.parseInt）
		assertThat(PrimitiveUtils.toBigDecimal("1.5")).isEqualTo(new BigDecimal("1.5"));
		assertThat(PrimitiveUtils.toBigDecimal(" 42 ")).isEqualTo(new BigDecimal("42"));
		//原有正确路径不变
		assertThat(PrimitiveUtils.toBigDecimal(null)).isEqualTo(BigDecimal.ZERO);
		assertThat(PrimitiveUtils.toBigDecimal(7)).isEqualTo(new BigDecimal("7"));
		assertThat(PrimitiveUtils.toBigDecimal(7L)).isEqualTo(new BigDecimal("7"));
	}

	// ---------- P1-17 ----------

	@Test
	public void testStringToBooleanParsing() {
		//修复前实测: Boolean.getBoolean("true") 是读系统属性, 返回 false 且不报错
		assertThat(ValueHandlerFactory.convert("true", Boolean.class)).isTrue();
		assertThat(Transformers.convert("TRUE", Boolean.class)).isTrue();
		assertThat(Transformers.convert(" false ", Boolean.class)).isFalse();
		assertThat(Transformers.convert("yes", Boolean.class)).isFalse(); //非 true 字面量仍为 false 的语义不变
	}
}
