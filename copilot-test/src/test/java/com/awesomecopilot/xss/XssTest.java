package com.awesomecopilot.xss;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.jupiter.api.Test;

public class XssTest {

	@Test
	public void testEscapeHtml() {
		String html = "<script>alert('XSS')</script>";
		String escaped = StringEscapeUtils.escapeHtml4(html);
		System.out.println(escaped);
	}
}
