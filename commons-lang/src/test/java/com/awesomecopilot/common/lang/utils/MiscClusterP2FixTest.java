package com.awesomecopilot.common.lang.utils;

import com.awesomecopilot.common.lang.constants.DateConstants;
import com.awesomecopilot.common.lang.exception.ApplicationException;
import com.awesomecopilot.common.lang.exception.BusinessException;
import com.awesomecopilot.common.lang.exception.DateParseException;
import com.awesomecopilot.common.lang.exception.EntityNotFoundException;
import com.awesomecopilot.common.lang.exception.InvalidCodeException;
import com.awesomecopilot.common.lang.exception.IORuntimeException;
import com.awesomecopilot.common.lang.exception.LocalDateParseException;
import com.awesomecopilot.common.lang.exception.LocalDateTimeException;
import com.awesomecopilot.common.lang.exception.NoDateFormatFoundException;
import com.awesomecopilot.common.lang.exception.NoSuitableValueHandlerException;
import com.awesomecopilot.common.lang.exception.SerializeException;
import com.awesomecopilot.common.lang.exception.SqlParseException;
import com.awesomecopilot.common.lang.exception.UnsupportedLocalDateFormatException;
import com.awesomecopilot.common.lang.exception.UnsupportedLocalDateTimeFormatException;
import com.awesomecopilot.common.lang.exception.UnsupportedLocalTimeFormatException;
import com.awesomecopilot.common.lang.exception.UnsupportedSizeUnitException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * P2-39 杂项小缺陷簇回归测试（CODE_REVIEW_REPORT 四、P2 级问题，2026-09-23 批次）。
 * <p>
 * 覆盖其中的非破坏性子项：
 * <ul>
 * <li>Types/ArrayTypes 数组类型注册表缺 boolean[]/short[]/byte[]/char[]（及包装类数组），
 * 未命中时 arrayTypes() 返回 null 且没有任何提示；Types 的手写 put 清单与 ArrayTypes
 * 枚举是两份维护点，遍历 values() 统一构建；</li>
 * <li>BusinessException(errorType, params...) 存的 messageParams 是
 * Arrays.asList 定长视图——getMessageParams().add(...) 实测抛
 * UnsupportedOperationException；</li>
 * <li>exception 包 17 个异常类缺 serialVersionUID（同包另外 4 个有）——
 * 跨版本反序列化时 JVM 按类结构自动生成的 UID 会漂移；</li>
 * <li>AlgorithmUtils.printArray 用 System.out（库代码统一走 slf4j）；</li>
 * <li>DateConstants 英文日期 4 对：正则与 DateTimeFormatter 的分隔符必须一致
 * （报告记录过一次"正则连字符/格式串斜杠"的次生风险，用往返测试守住）。</li>
 * </ul>
 * 破坏性候选（ErrorTypes 码格式统一、AbstractErrorType 去 @Data、
 * ErrorType.message() 默认 null、FileUtils MD5 截 12 位）另议，不在本测试范围。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class MiscClusterP2FixTest {

	// ---------------- Types / ArrayTypes ----------------

	@Test
	public void testArrayTypesCoversAllPrimitiveArrayKinds() {
		//修复前实测: boolean[]/byte[]/char[]/short[] 四类基本类型数组不在注册表里,
		//Types.arrayTypes(new byte[0]) 返回 null 且无任何提示
		assertThat(Types.arrayTypes(new boolean[0])).isNotNull();
		assertThat(Types.arrayTypes(new short[0])).isNotNull();
		assertThat(Types.arrayTypes(new byte[0])).isNotNull();
		assertThat(Types.arrayTypes(new char[0])).isNotNull();
		assertThat(Types.arrayTypes(new int[0])).isNotNull();
		assertThat(Types.arrayTypes(new long[0])).isNotNull();
		assertThat(Types.arrayTypes(new float[0])).isNotNull();
		assertThat(Types.arrayTypes(new double[0])).isNotNull();
		assertThat(Types.arrayTypes(new Boolean[0])).isNotNull();
		assertThat(Types.arrayTypes(new Short[0])).isNotNull();
		assertThat(Types.arrayTypes(new Byte[0])).isNotNull();
		assertThat(Types.arrayTypes(new Character[0])).isNotNull();
		assertThat(Types.arrayTypes(new Integer[0])).isNotNull();
		assertThat(Types.arrayTypes(new Long[0])).isNotNull();
		assertThat(Types.arrayTypes(new Float[0])).isNotNull();
		assertThat(Types.arrayTypes(new Double[0])).isNotNull();
		assertThat(Types.arrayTypes(new String[0])).isNotNull();
	}

	// ---------------- BusinessException messageParams 可追加 ----------------

	@Test
	public void testBusinessExceptionMessageParamsIsMutable() {
		BusinessException ex = new BusinessException(
				com.awesomecopilot.common.lang.errors.ErrorTypes.BAD_REQUEST, "a", "b");
		assertThatCode(() -> ex.getMessageParams().add("c"))
				.as("修复前实测 Arrays.asList 定长视图, add 抛 UnsupportedOperationException")
				.doesNotThrowAnyException();
		assertThat(ex.getMessageParams()).containsExactly("a", "b", "c");
	}

	// ---------------- exception 包 serialVersionUID 齐备 ----------------

	@Test
	public void testAllExceptionsDeclareSerialVersionUid() {
		List<Class<?>> exceptions = Arrays.asList(
				ApplicationException.class, BusinessException.class, DateParseException.class,
				EntityNotFoundException.class, InvalidCodeException.class, IORuntimeException.class,
				LocalDateParseException.class, LocalDateTimeException.class,
				NoDateFormatFoundException.class, NoSuitableValueHandlerException.class,
				SerializeException.class, SqlParseException.class,
				UnsupportedLocalDateFormatException.class, UnsupportedLocalDateTimeFormatException.class,
				UnsupportedLocalTimeFormatException.class, UnsupportedSizeUnitException.class,
				//原有 4 个有 UID 的也一并纳入清单防回归
				com.awesomecopilot.common.lang.exception.AsyncExecutionException.class,
				com.awesomecopilot.common.lang.exception.ConcurrentOperationException.class,
				com.awesomecopilot.common.lang.exception.FileCopyException.class,
				com.awesomecopilot.common.lang.exception.ServiceException.class);
		for (Class<?> c : exceptions) {
			assertThatCode(() -> {
				//字段是 private, 只需确认可反射取到且为 static final(值本身是编译期常量)
				java.lang.reflect.Field f = c.getDeclaredField("serialVersionUID");
				int mod = f.getModifiers();
				if (!java.lang.reflect.Modifier.isStatic(mod) || !java.lang.reflect.Modifier.isFinal(mod)) {
					throw new IllegalStateException(c.getSimpleName() + ".serialVersionUID 必须是 static final");
				}
			}).as(c.getSimpleName() + " 必须自己声明 serialVersionUID").doesNotThrowAnyException();
		}
	}

	// ---------------- AlgorithmUtils.printArray 不再写 System.out ----------------

	@Test
	public void testPrintArrayDoesNotTouchSystemOut() throws Exception {
		PrintStream original = System.out;
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(buf, true, "UTF-8"));
			AlgorithmUtils.printArray(new int[]{1, 2, 3});
		} finally {
			System.setOut(original);
		}
		//库代码统一走 slf4j(修复前实测直接 System.out 打印 "{1, 2, 3}")
		assertThat(buf.toString("UTF-8")).isEmpty();
	}

	// ---------------- DateConstants 英文日期: 正则与格式器一致 ----------------

	@Test
	public void testEnglishDatePatternsRoundTrip() {
		//每对 (正则, DateTimeFormatter): 正则能匹配的样本, 格式器必须能解析出同值日期
		assertEnDatePair(DateConstants.PT_DATE_EN, DateConstants.DTF_DATE_FORMAT_EN, "12-25-2020", LocalDate.of(2020, 12, 25));
		assertEnDatePair(DateConstants.PT_DATE_EN_1, DateConstants.DTF_DATE_FORMAT_EN_1, "12-5-2020", LocalDate.of(2020, 12, 5));
		assertEnDatePair(DateConstants.PT_DATE_EN_2, DateConstants.DTF_DATE_FORMAT_EN_2, "5-25-2020", LocalDate.of(2020, 5, 25));
		assertEnDatePair(DateConstants.PT_DATE_EN_3, DateConstants.DTF_DATE_FORMAT_EN_3, "5-5-2020", LocalDate.of(2020, 5, 5));
	}

	private void assertEnDatePair(java.util.regex.Pattern pattern, java.time.format.DateTimeFormatter fmt, String sample, java.time.LocalDate expected) {
		assertThat(pattern.matcher(sample).matches())
				.as("正则应匹配 " + sample)
				.isTrue();
		//格式器必须能解析正则放行的样本(修复前记录: 正则要求连字符而格式串是斜杠, 两头不一致)
		assertThat(LocalDate.parse(sample, fmt)).isEqualTo(expected);
	}
	@Test
	public void testEnglishDateExactValues() {
		assertThat(LocalDate.parse("12-25-2020", DateConstants.DTF_DATE_FORMAT_EN))
				.isEqualTo(LocalDate.of(2020, 12, 25));
		assertThat(LocalDate.parse("5-25-2020", DateConstants.DTF_DATE_FORMAT_EN_2))
				.isEqualTo(LocalDate.of(2020, 5, 25));
	}
}
