package com.awesomecopilot.utils;

import com.awesomecopilot.common.lang.utils.MathUtils;
import org.junit.Test;

public class MathUtilsTest {

	@Test
	public void testRound() {
		String val = MathUtils.format(1305.4d, 2);
		System.out.println(val);
		
	}
}
