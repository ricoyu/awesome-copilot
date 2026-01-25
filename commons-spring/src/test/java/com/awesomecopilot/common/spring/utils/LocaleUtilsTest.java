package com.awesomecopilot.common.spring.utils;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Copyright: (C), 2020-09-02 13:53
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class LocaleUtilsTest {
	
	@Test
	public void testZhCN() {
		Locale locale = LocaleUtils.toLocale("zh_CN");
		assertTrue(locale.equals(Locale.CHINA));
		
		Locale locale2 = LocaleUtils.toLocale("ZH_CN");
		assertTrue(locale.equals(Locale.CHINA));
	}
}
