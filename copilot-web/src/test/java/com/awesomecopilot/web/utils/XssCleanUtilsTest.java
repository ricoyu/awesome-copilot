package com.awesomecopilot.web.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XssXleanUtilsTest {

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
	 * 测试场景4：纯文本恶意函数调用（无标签包裹）
	 */
	@Test
	public void testMaliciousFuncInPlainText() {
		String testInput = "用户输入：eval('123') + alert(456) + confirm(789)";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("用户输入：++", cleanResult);
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
	 */
	@Test
	public void testMixedXssAttack() {
		String testInput = "<SCRIPT>eval('xss')</SCRIPT><a href=javascript:alert(1)>链接</a><div onmouseover=confirm(2)>文本</div>正常内容";
		String cleanResult = XssCleanUtils.clean(testInput);
		assertEquals("<a href=>链接</a><div >文本</div>正常内容", cleanResult);
	}
}
