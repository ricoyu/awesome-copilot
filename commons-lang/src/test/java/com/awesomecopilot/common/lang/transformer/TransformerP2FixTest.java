package com.awesomecopilot.common.lang.transformer;

import com.awesomecopilot.common.lang.exception.NoSuitableValueHandlerException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-26 / P2-27 ValueHandlerFactory 与 Transformers 回归测试
 * （CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <ul>
 * <li>P2-26 ShortValueHandler.convert(null): 修复前实测返回 0, 同文件其余 handler 全部
 * 返回 null——DB 的 NULL smallint 列会变成 0。改 null;</li>
 * <li>P2-26 BigDecimalValueHandler: 修复前实测 convert(100L) 抛异常且消息写
 * "to requested type [java.lang.Float]"(复制粘贴痕迹), Long/Short/String 本可无损转换
 * 却直接拒绝。补分支、报错文案指向 BigDecimal;</li>
 * <li>P2-26 render 后缀: 修复前实测 StringValueHandler.render("abc") 返回 "abcF"、
 * DateValueHandler.render 返回 "...1970F"——+ 'F' 只属于浮点字面量。String 改为带引号、
 * Date 去后缀、Double 后缀改 'D'(java 里 1.5d 是 D 字面量, 'F' 是 Float 的);</li>
 * <li>P2-27 Transformers.convert 消息模板写了 {3} 但只传 3 个参数, 实测异常文本以字面量
 * "to expected type[{3}]" 结尾, 真正要转的目标类型丢了。改 {2};</li>
 * <li>P2-27 determineAppropriateHandler(List.class, null) 实测 NPE
 * (GenericTypeInspector 直接 field.getType()); 单参重载对 List 永远返回 null,
 * Transformers.convert(任何List, List.class) 必抛"没有合适的 handler"——
 * 集合转换能力存在但两个入口都走不到。修复: 两参重载 field 为 null 时退化为默认
 * List&lt;String&gt; handler; 元素类型非 String/Integer 时返回 null 而不是掉出末尾;
 * 单参重载对 List 返回 StringListValueHandler 默认。</li>
 * </ul>
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class TransformerP2FixTest {

	// ---------------- P2-26 ShortValueHandler null ----------------

	@Test
	public void testShortHandlerOfNullReturnsNull() {
		//修复前实测返回 0(DB 的 NULL smallint 列会变成 0)
		assertThat(ValueHandlerFactory.ShortValueHandler.INSTANCE.convert(null))
				.as("convert(null) 必须返回 null(修复前实测返回 0)")
				.isNull();
		assertThat(ValueHandlerFactory.ShortValueHandler.INSTANCE.convert((short) 5)).isEqualTo((short) 5);
	}

	// ---------------- P2-26 BigDecimalValueHandler ----------------

	@Test
	public void testBigDecimalHandlerAcceptsLongShortString() {
		//修复前实测: convert(100L) 抛 IllegalArgumentException 且消息写 Float(复制粘贴痕迹)
		assertThat(ValueHandlerFactory.BigDecimalValueHandler.INSTANCE.convert(100L))
				.isEqualByComparingTo(new BigDecimal("100"));
		assertThat(ValueHandlerFactory.BigDecimalValueHandler.INSTANCE.convert((short) 7))
				.isEqualByComparingTo(new BigDecimal("7"));
		assertThat(ValueHandlerFactory.BigDecimalValueHandler.INSTANCE.convert("12.5"))
				.isEqualByComparingTo(new BigDecimal("12.5"));
		assertThat(ValueHandlerFactory.BigDecimalValueHandler.INSTANCE.convert(100))
				.isEqualByComparingTo(new BigDecimal("100"));
	}

	@Test
	public void testBigDecimalHandlerErrorNamesBigDecimal() {
		assertThatThrownBy(() -> ValueHandlerFactory.BigDecimalValueHandler.INSTANCE.convert(new Object()))
				.as("不支持的类型应报错, 且消息指向 BigDecimal(修复前写 java.lang.Float)")
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("java.math.BigDecimal")
				.hasMessageNotContaining("java.lang.Float");
	}

	// ---------------- P2-26 render 后缀 ----------------

	@Test
	public void testStringRenderQuotedNotFloatSuffix() {
		//修复前实测 render("abc") 返回 "abcF"
		assertThat(ValueHandlerFactory.StringValueHandler.INSTANCE.render("abc"))
				.as("字符串渲染应带引号(修复前实测返回 abcF)")
				.isEqualTo("\"abc\"");
	}

	@Test
	public void testDateRenderHasNoFloatSuffix() {
		//修复前实测: 日期渲染返回 "Thu Jan 01 ... 1970F"
		Date d = new Date(0L);
		assertThat(ValueHandlerFactory.DateValueHandler.INSTANCE.render(d))
				.as("日期渲染 = toString(), 不得带 'F' 后缀")
				.isEqualTo(d.toString());
	}

	@Test
	public void testFloatRenderSuffixesUnchanged() {
		assertThat(ValueHandlerFactory.FloatValueHandler.INSTANCE.render(1.5f)).isEqualTo("1.5F");
		//Double 的 'F' 修正为 'D'(java 字面量语义)
		assertThat(ValueHandlerFactory.DoubleValueHandler.INSTANCE.render(1.5d))
				.as("Double 渲染后缀应为 D(F 是 Float 的字面量后缀)")
				.isEqualTo("1.5D");
	}

	// ---------------- P2-27 Transformers 消息模板 {3} ----------------

	@Test
	public void testTransformersErrorMessageCarriesTargetType() {
		//修复前实测: 异常文本以字面量 "to expected type[{3}]" 结尾, 目标类型丢了
		assertThatThrownBy(() -> Transformers.convert("abc", Map.class))
				.isInstanceOf(NoSuitableValueHandlerException.class)
				.hasMessageContaining("java.util.Map")
				.hasMessageNotContaining("{3}");
	}

	// ---------------- P2-27 List handler 入口 ----------------

	@Test
	public void testDetermineHandlerForListWithNullFieldDoesNotThrowNpe() {
		//修复前实测: NPE(GenericTypeInspector.inspectGenericTypes 直接 field.getType())
		assertThatCode(() -> ValueHandlerFactory.determineAppropriateHandler(List.class, null))
				.doesNotThrowAnyException();
		assertThat(ValueHandlerFactory.determineAppropriateHandler(List.class, null))
				.as("无 field 可查元素类型时给默认的 String 列表 handler")
				.isNotNull();
	}

	@Test
	public void testTransformersConvertToWorksWithListTarget() {
		//修复前实测: 单参入口对 List 永远返回 null, 这里必抛 NoSuitableValueHandlerException
		List<String> list = Transformers.convert("[a, b, c]", List.class);
		assertThat(list).containsExactly("a", "b", "c");
	}
}
