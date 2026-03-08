package com.awesomecopilot.bg.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HDFSUtilsTest {

	@Test
	public void testMkdirs() {
		boolean maked = HDFSUtils.mkdir("/rico");
		assertTrue(maked);
	}
}