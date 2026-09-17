package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * StringUtils.joinWith 系列重载的行为测试。
 * <p>
 * 重点验证可变参数陷阱：传 List 时必须逐元素拼接，而不是把整个 List 当成一个元素
 * 调 toString 得到 "[a, b]" 这种带方括号的结果（评审报告 P0-1 的根因）。
 * <p>
 * Copyright: Copyright (c) 2019-10-14 17:22
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class StringUtilsJoinWithTest {

	@Test
	public void testJoinWithListJoinsElementsNotToString() {
		List<String> allowedHeaders = new ArrayList<>();
		allowedHeaders.add("*");
		// 单元素 List 必须得到 "*"，如果是 "[*]" 说明 List 被当成了可变参数的一个元素
		assertEquals("*", StringUtils.joinWith(", ", allowedHeaders));

		assertEquals("123,456,123131", StringUtils.joinWith(",", asList("123", "456", "123131")));
	}

	@Test
	public void testJoinWithListTreatsNullElementAsEmptyString() {
		// 方法 javadoc 承诺：null 元素按空字符串参与拼接（不是跳过），与 commons-lang3 的 skipNulls 语义不同
		assertEquals("a,,b", StringUtils.joinWith(",", Arrays.asList("a", null, "b")));
		assertEquals("123,456,123131,", StringUtils.joinWith(",", asList("123", "456", "123131", "")));
		assertEquals("a,,b,", StringUtils.joinWith(",", Arrays.asList("a", null, "b", null)));
	}

	@Test
	public void testJoinWithSetJoinsElementsNotToString() {
		// 评审报告点名的可变参数陷阱：Set/Collection 传进 joinWith 会被 Object... 重载整个 toString 成 "[a, b]"，
		// 必须补 Iterable 重载让所有集合都逐元素拼接
		Set<String> origins = new LinkedHashSet<>(Arrays.asList("http://a.com", "http://b.com"));
		assertEquals("http://a.com, http://b.com", StringUtils.joinWith(", ", origins));
	}

	@Test
	public void testJoinWithVarargsNullSeparatorTreatedAsEmpty() {
		// javadoc 承诺 null 分隔符按空字符串处理；当前 Guava Joiner.on(null) 直接抛 NullPointerException
		assertEquals("ab", StringUtils.joinWith(null, "a", "b"));
		assertEquals("12", StringUtils.joinWith(null, 1, 2));
	}

	@Test
	public void testJoinWithListNullSeparatorTreatedAsEmpty() {
		assertEquals("ab", StringUtils.joinWith(null, asList("a", "b")));
	}

	@Test
	public void testJoinWithListEmptyAndNull() {
		assertEquals("", StringUtils.joinWith(",", new ArrayList<>()));
		assertNull(StringUtils.joinWith(",", (List<?>) null));
	}

	@Test
	public void testJoinWithVarargsStillWork() {
		// 可变参数路径：字符串元素直接拼接
		assertEquals("a,b", StringUtils.joinWith(",", "a", "b"));
		// 整型走 Object... 重载，元素用 toString 参与拼接（copilot-networking 的用法）
		assertEquals("1,2,3", StringUtils.joinWith(",", 1, 2, 3));
	}

	@Test
	public void testJoinWithStringArrayResolvesToCharSequenceOverload() {
		// String[] 按重载解析会命中 joinWith(String, CharSequence...)，应逐元素拼接而不是输出 [Ljava.lang.String;@...
		String[] sources = {"a", "b", "c"};
		assertEquals("a,b,c", StringUtils.joinWith(",", sources));
	}

	@Test
	public void testJoinWithObjectVarargsNullElementTreatedAsEmptyString() {
		// Object... 路径的 null 元素从"跳过"改为"拼成空串"是本次行为变更，需要断言锚定
		assertEquals("a,,b", StringUtils.joinWith(",", "a", null, "b"));
	}

	@Test
	public void testJoinWithListCastToObjectStillExpandsElements() {
		// 显式强转 Object 会走 Object... 重载；修复前整个 List 被 toString 成 "[a, b]"，
		// 修复后 joinInternal 识别元素是 Iterable 并展开，跨入口结果必须一致
		assertEquals("a, b", StringUtils.joinWith(", ", (Object) asList("a", "b")));
	}

	@Test
	public void testJoinWithNestedListsFlattenedRecursively() {
		// 嵌套 List 从任何入口进来都递归展开到标量：
		// List 入口脱一层后内层列表仍需展开；(Object) 强转入口必须得到相同结果
		List<List<String>> nested = asList(asList("a", "b"), asList("c"));
		assertEquals("a,b,c", StringUtils.joinWith(",", nested));
		assertEquals("a,b,c", StringUtils.joinWith(",", (Object) nested));
	}

	@Test
	public void testJoinWithEmptyIterableReturnsEmptyString() {
		Set<String> empty = new LinkedHashSet<>();
		assertEquals("", StringUtils.joinWith(", ", empty));
		assertNull(StringUtils.joinWith(", ", (Iterable<?>) null));
	}
}
