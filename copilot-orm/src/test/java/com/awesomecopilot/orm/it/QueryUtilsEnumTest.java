package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.utils.QueryUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QueryUtils.enumCodeCondition 的 code=-1 语义(P2-13):
 * javadoc 承诺"code 为 -1 表示查询所有, 不加入 params", 旧实现没做这个检查,
 * 按文档写的业务代码会多出一个恒假条件。
 *
 * @author Rico Yu
 */
class QueryUtilsEnumTest {

	/** 带 code 属性的枚举, ALL 的 code=-1 表示"查全部" */
	enum StatusWithCode {
		ALL(-1),
		ENABLED(0),
		DISABLED(1);

		private final int code;

		StatusWithCode(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}

	/** 不带 code 属性的枚举 */
	enum PlainStatus {
		A, B
	}

	@Test
	@DisplayName("code=-1 表示查全部: 不加入 params")
	void codeMinusOneSkipped() {
		Map<String, Object> params = new HashMap<>();
		QueryUtils.enumCodeCondition(params, "status", StatusWithCode.ALL);
		assertFalse(params.containsKey("status"),
				"code=-1 应表示查全部, 不该把条件加进 params");
	}

	@Test
	@DisplayName("code 为正常值时照常加入 params")
	void normalCodeAdded() {
		Map<String, Object> params = new HashMap<>();
		QueryUtils.enumCodeCondition(params, "status", StatusWithCode.ENABLED);
		assertTrue(params.containsKey("status"));
		assertEquals(0, params.get("status"));

		params.clear();
		QueryUtils.enumCodeCondition(params, "status", StatusWithCode.DISABLED);
		assertEquals(1, params.get("status"));
	}

	@Test
	@DisplayName("queryValue 为 null 时不加入 params")
	void nullValueSkipped() {
		Map<String, Object> params = new HashMap<>();
		QueryUtils.enumCodeCondition(params, "status", null);
		assertFalse(params.containsKey("status"));
	}

	@Test
	@DisplayName("枚举没有 code 属性时忽略, 不报错")
	void enumWithoutCodeIgnored() {
		Map<String, Object> params = new HashMap<>();
		assertDoesNotThrow(() -> QueryUtils.enumCodeCondition(params, "status", PlainStatus.A));
		assertFalse(params.containsKey("status"), "没有 code 属性应忽略该条件");
	}
}
