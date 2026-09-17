package com.awesomecopilot.web.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XssCleanUtilsTest {

	@Test
	public void test() {
		String testInput = "<SCRIPT>alert('XSS')</SCRIPT><p>正常内容</p><img src=x ONERROR=alert(1)>";
		String cleanResult = XssCleanUtils.clean(testInput);
		System.out.println(cleanResult);
		// 输出结果：<p>正常内容</p><img src=x >
		assertEquals("<p>正常内容</p><img src=x >", cleanResult);
	}


	/**
	 * 测试场景1：大小写混合的 script 标签 + on事件属性
	 */
	@Test
	public void testScriptTagAndOnEvent() {
		String testInput = "<SCRIPT>alert('XSS')</SCRIPT><p>正常内容</p><img src=x ONERROR=alert(1)>";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("<p>正常内容</p><img src=x >", cleanResult);
	}

	/**
	 * 测试场景2：javascript: 伪协议（含空格/大小写变种）
	 */
	@Test
	public void testJavascriptProtocol() {
		String testInput = "<a href='JavaScript: alert(\"xss\")'>点击</a><a href='java script:confirm(1)'>链接</a>";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("<a href='alert(\"xss\")'>点击</a><a href='confirm(1)'>链接</a>", cleanResult);
	}

	/**
	 * 测试场景3：on事件属性（带引号/无引号/空格变种）
	 */
	@Test
	public void testOnEventAttrVariants() {
		String testInput = "<div onclick = 'alert(1)'>测试</div><img src=1 onload=confirm(2)><button OnClick=eval(3)>按钮</button>";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("<div >测试</div><img src=1 ><button >按钮</button>", cleanResult);
	}

	/**
	 * 测试场景4（评审报告 P0-2 回归锚点）：纯文本里的 eval/alert/confirm 是普通英文词,
	 * 出现在业务文案、字段说明、消息模板里不能被删除——不渲染成 HTML 时它们本就不构成 XSS,
	 * 要渲染时应做输出转义而不是输入删除
	 */
	@Test
	public void testPlainTextFunctionNamesArePreserved() {
		String testInput = "用户输入：eval('123') + alert(456) + confirm(789)";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("用户输入：eval('123') + alert(456) + confirm(789)", cleanResult);
	}

	/**
	 * 评审报告 P0-2 案例原文：正常业务文案里的括号词不能被清洗掉
	 */
	@Test
	public void testNormalBusinessTextWithParenthesesIsPreserved() {
		String testInput = "{\"desc\":\"本模型 alert(risk) 指标偏高\"}";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("{\"desc\":\"本模型 alert(risk) 指标偏高\"}", cleanResult);
	}

	/**
	 * 测试场景5：嵌套/变形的 script 标签（含空格）
	 */
	@Test
	public void testNestedScriptTag() {
		String testInput = "< script type='text/javascript'>alert('xss')< / script >正常文本<script>eval(1)</script>";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("正常文本", cleanResult);
	}

	/**
	 * 测试场景6：空值/空字符串（边界场景）
	 */
	@Test
	public void testEmptyInput() {
		// 测试 null
		String cleanNull = XssCleanUtils.clean(null);
		assertEquals(null, cleanNull);

		// 测试空字符串
		String cleanEmpty = XssCleanUtils.clean("");
		assertEquals("", cleanEmpty);

		// 测试全空格
		String cleanSpace = XssCleanUtils.clean("   ");
		assertEquals("   ", cleanSpace);
	}

	/**
	 * 测试场景7：正常内容（无XSS），验证不破坏正常文本
	 */
	@Test
	public void testNormalContent() {
		String testInput = "<p>Hello World!</p><img src='test.jpg' alt='测试图片'><a href='/home'>首页</a>";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("<p>Hello World!</p><img src='test.jpg' alt='测试图片'><a href='/home'>首页</a>", cleanResult);
	}

	/**
	 * 测试场景8：混合多种XSS攻击方式
	 * 注：删除纯文本函数规则(P0-2)后, javascript: 伪协议前缀仍被清除(危险点消除),
	 * href= 后残留的 alert(1) 只是无害文本, 不再被主动删除
	 */
	@Test
	public void testMixedXssAttack() {
		String testInput = "<SCRIPT>eval('xss')</SCRIPT><a href=javascript:alert(1)>链接</a><div onmouseover=confirm(2)>文本</div>正常内容";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("<a href=alert(1)>链接</a><div >文本</div>正常内容", cleanResult);
	}

	/**
	 * 人民币符号
	 */
	@Test
	public void testRMB() {
		String testInput = "2 年碎屏险 ¥259.00";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("2 年碎屏险 ¥259.00", cleanResult);
	}

	/**
	 * 评审报告 P2-8 回归锚点：cleanObject 处理 List 时, 清洗结果必须体现在返回值里。
	 * 旧实现遍历 Iterable 后丢弃递归结果, 调用方拿回来的元素还是带 <script> 的原文
	 */
	@Test
	public void testCleanObjectReplacesStringElementsInList() {
		List<String> input = new ArrayList<>();
		input.add("<script>alert(1)</script>正文");
		input.add("plain");
		Object cleaned = XssCleanUtils.cleanObject(input);
		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) cleaned;
		assertEquals("正文", list.get(0));
		assertEquals("plain", list.get(1));
	}

	/**
	 * 不可变 List（List.of）也要能清洗——不能依赖 List.set 就地改
	 */
	@Test
	public void testCleanObjectWorksWithImmutableList() {
		List<String> input = List.of("<img src=x onerror=alert(1)>图", "ok");
		Object cleaned = XssCleanUtils.cleanObject(input);
		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) cleaned;
		// onerror 属性被移除, 但 <img> 标签本身保留(与 clean() 既有行为一致)
		assertEquals("<img src=x >图", list.get(0));
		assertEquals("ok", list.get(1));
	}

	/**
	 * 嵌套集合与 String[] 都要递归清洗; Map 的 key/value 也要
	 */
	@Test
	public void testCleanObjectHandlesNestedCollections() {
		List<Object> nested = new ArrayList<>();
		nested.add(List.of("<script>x</script>干净"));
		Object cleaned = XssCleanUtils.cleanObject(nested);
		@SuppressWarnings("unchecked")
		List<Object> outer = (List<Object>) cleaned;
		@SuppressWarnings("unchecked")
		List<String> inner = (List<String>) outer.get(0);
		assertEquals("干净", inner.get(0));

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("k", "<script>v</script>值");
		@SuppressWarnings("unchecked")
		Map<String, Object> cleanedMap = (Map<String, Object>) XssCleanUtils.cleanObject(map);
		assertEquals("值", cleanedMap.get("k"));
	}
}
