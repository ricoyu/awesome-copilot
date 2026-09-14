package com.awesomecopilot.json;

import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-2 回归测试：parseJson 不再无条件跑 JSON.cleanup。
 * <p>
 * 旧实现在每次解析前跑 cleanup（三次全串 replace），会把含合法转义双引号的 JSON
 * 改成语法非法串，所有读取静默返回 null。新实现改为"原样解析 + Map/List 树级展开内嵌
 * JSON，解析失败才 cleanup 抢救"，这些用例锁住新旧边界行为。
 */
public class JsonPathUtilsCleanupTest {

	@Test
	public void validJsonWithEscapedQuotesSurvives() {
		// 修复前: $.note 返回 null(整串被 cleanup 改坏)
		String json = "{\"note\":\"he said \\\"hi\\\"\",\"amount\":3,\"tail\":\"end\"}";
		assertThat((Object) JsonPathUtils.readNode(json, "$.note")).isEqualTo("he said \"hi\"");
		assertThat((Object) JsonPathUtils.readNode(json, "$.tail")).isEqualTo("end");
		assertThat(JsonPathUtils.ifExists(json, "$.note")).isTrue();
	}

	@Test
	public void embeddedJsonStringIsUnwrappedForPagingPaths() {
		// 老用法: 内嵌JSON字符串被二次字符串化存储, $.billJson.FBillNo 要能穿透
		String json = "{\"method\":\"save\",\"billJson\":\"{\\\"FBillNo\\\":\\\"XSCK001\\\",\\\"qty\\\":2}\",\"userName\":\"张三\"}";
		assertThat((Object) JsonPathUtils.readNode(json, "$.billJson.FBillNo")).isEqualTo("XSCK001");
		assertThat((Object) JsonPathUtils.readNode(json, "$.userName")).isEqualTo("张三");
		// 行为变化说明: 取整棵子树时, 拿到的是展开后的对象而不是原始字符串
		Object billJson = JsonPathUtils.readNode(json, "$.billJson");
		assertThat(billJson).isInstanceOf(java.util.Map.class);
	}

	@Test
	public void doubleStringifiedValueAlsoUnwrapped() {
		// 值本身又是被加引号的字符串化JSON("\"{...}\")", 剥一层后展开
		String json = "{\"a\":\"b\",\"emb\":\"\\\"{\\\\\\\"k\\\\\\\":9}\\\"\"}";
		assertThat((Object) JsonPathUtils.readNode(json, "$.emb.k")).isEqualTo(9);
	}

	@Test
	public void nonJsonTemplateTextKeptAsIs() {
		// 以 { 开头但不是合法JSON的模板串不能被误提升, 也不能被改坏
		String json = "{\"tpl\":\"{name} not json\",\"q\":\"a\\\"b\"}";
		assertThat((Object) JsonPathUtils.readNode(json, "$.tpl")).isEqualTo("{name} not json");
		assertThat((Object) JsonPathUtils.readNode(json, "$.q")).isEqualTo("a\"b");
	}

	@Test
	public void malformedJsonStillRescuedByCleanup() {
		// 老 cleanup 的存在理由: 双重转义弄坏外层结构的脏数据, 直接解析失败后仍要能读
		String dirty = "{\"a\":\"b\",\"billJson\":\"{\"x\":1}\"}";
		assertThat((Object) JsonPathUtils.readNode(dirty, "$.billJson.x")).isEqualTo(1);
	}

	@Test
	public void readListNodeStillWorks() {
		String json = "{\"movies\":[{\"title\":\"A\"},{\"title\":\"B\"}]}";
		List<String> titles = JsonPathUtils.readListNode(json, "$.movies[*].title");
		assertThat(titles).containsExactly("A", "B");
	}

	@Test
	public void embeddedJsonBeyondFiveNativeLevelsStillUnwrapped() {
		// 评审发现的回归: 深度预算曾按"距根的树深"计数, 6层原生嵌套里的内嵌JSON读不出.
		// 现在预算只计"字符串化展开次数", 原生下钻不计数, 深嵌套的 payload 也要能穿透.
		String deep = "{\"l1\":{\"l2\":{\"l3\":{\"l4\":{\"l5\":{\"payload\":\"{\\\"inner\\\":42}\"}}}}}}";
		assertThat((Object) JsonPathUtils.readNode(deep, "$.l1.l2.l3.l4.l5.payload.inner")).isEqualTo(42);
	}

	@Test
	public void wholeDocumentStringifiedOnceIsUnwrapped() {
		// 整篇文档被字符串化(根节点是字符串): 旧cleanup能读, 重写后的根级展开也要能读
		String whole = "\"{\\\"a\\\":1}\"";
		assertThat((Object) JsonPathUtils.readNode(whole, "$.a")).isEqualTo(1);
	}

	@Test
	public void multiLayerQuotedValueIsStrippedInLoop() {
		// 值被反复字符串化(剥1层引号后还是引号开头): 循环剥层直到见到 '{'
		String inner = "{\"k\":7}";
		String once = com.awesomecopilot.json.jackson.JacksonUtils.toJson(inner); // 文本以 " 开头
		String twice = com.awesomecopilot.json.jackson.JacksonUtils.toJson(once); // 再包一层, 仍以 " 开头
		// emb 的值直接放 twice(剥第1层引号得到 once, 剥第2层才见 '{'), 旧实现只剥1层会失败
		java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
		m.put("emb", twice);
		String docJson = com.awesomecopilot.json.jackson.JacksonUtils.toJson(m);
		assertThat((Object) JsonPathUtils.readNode(docJson, "$.emb.k")).isEqualTo(7);
	}

	@Test
	public void singleQuotedJsonParsesSameAsJacksonUtils() {
		// 回归 P1-7: JacksonJsonProvider 必须复用全局装饰 mapper,
		// 否则 ALLOW_SINGLE_QUOTES 只在 JacksonUtils.toObject 端生效, 这里返回 null
		String singleQuotes = "{'name': 'John', 'age': 30}";
		assertThat((Object) JsonPathUtils.readNode(singleQuotes, "$.name")).isEqualTo("John");
		assertThat((Object) JsonPathUtils.readNode(singleQuotes, "$.age")).isEqualTo(30);
	}
}
