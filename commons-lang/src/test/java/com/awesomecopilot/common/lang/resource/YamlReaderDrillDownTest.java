package com.awesomecopilot.common.lang.resource;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-14 回归测试（CODE_REVIEW_REPORT 2026-09-22）：高优先级文件里有半个 key 前缀，
 * 会把低优先级文件的完整值挡掉。
 * <p>
 * 修复前实测（探针构造 config/application.yml 只有 my.other + application.yml 有完整
 * copilot.text.name）：getString("copilot.text.name") 返回 null——逐层下钻在第一个
 * yaml 源上走到一半取不到，整个方法直接 return null，没有换下一个优先级的源继续找。
 * <p>
 * 下钻逻辑抽取为包级静态方法 drillDown 以便脱离文件系统单测（get() 内调用它）。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class YamlReaderDrillDownTest {

	private static Map<String, Object> nested(String outer, String inner, Object value) {
		Map<String, Object> leaf = new HashMap<>();
		leaf.put(inner, value);
		Map<String, Object> mid = new HashMap<>();
		mid.put(outer, leaf);
		Map<String, Object> root = new HashMap<>();
		root.put("copilot", mid);
		return root;
	}

	@Test
	public void testHalfPrefixInHighPriorityFileDoesNotBlockLowPriorityValue() {
		//高优先级源只有 copilot 下别的子键(没有 text), 低优先级源有完整 copilot.text.name
		Map<String, Object> high = new HashMap<>();
		high.put("copilot", nestedWithOther());
		Map<String, Object> low = nested("text", "name", "main-value");

		Object value = YamlReader.drillDown(asList(high, low), "copilot.text.name");

		//修复前实测: 返回 null
		assertThat(value).as("半前缀不应挡掉低优先级文件的完整值").isEqualTo("main-value");
	}

	private static Map<String, Object> nestedWithOther() {
		Map<String, Object> other = new HashMap<>();
		other.put("x", 1);
		Map<String, Object> copilot = new HashMap<>();
		copilot.put("other", other);
		return copilot;
	}

	@Test
	public void testHighPriorityValueStillWins() {
		//合法路径不能被改坏: 高优先级源里有完整值时仍取高优先级
		Map<String, Object> high = nested("text", "name", "config-value");
		Map<String, Object> low = nested("text", "name", "main-value");

		assertThat(YamlReader.drillDown(asList(high, low), "copilot.text.name"))
				.isEqualTo("config-value");
	}

	@Test
	public void testMissingEverywhereReturnsNull() {
		Map<String, Object> high = nested("text", "name", "v");
		Map<String, Object> low = nested("other", "x", "y");

		assertThat(YamlReader.drillDown(asList(high, low), "copilot.missing.key")).isNull();
		//中途不是 Map(值是标量)时也不能抛 ClassCastException, 应视为该源没有此路径
		assertThat(YamlReader.drillDown(asList(high, low), "copilot.text.name.deeper")).isNull();
	}

	@Test
	public void testNullSourcesSkipped() {
		//某个位置没有文件(yaml 为 null)不影响其它源查询
		Map<String, Object> low = nested("text", "name", "main-value");
		assertThat(YamlReader.drillDown(asList(null, low), "copilot.text.name")).isEqualTo("main-value");
	}
}
