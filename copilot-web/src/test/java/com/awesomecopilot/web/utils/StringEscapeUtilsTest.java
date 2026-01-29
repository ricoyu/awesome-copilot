package com.awesomecopilot.web.utils;

import com.awesomecopilot.common.lang.utils.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringEscapeUtilsTest {

	@Test
	public void testRMBSign() {
		String s = "2 年碎屏险 ¥259.00";
		String escaped = StringUtils.escapeHtml4(s);
		//assertEquals("2 年碎屏险 &yen;259.00", escaped);
		assertEquals("2 年碎屏险 ¥259.00", escaped);
	}
}
