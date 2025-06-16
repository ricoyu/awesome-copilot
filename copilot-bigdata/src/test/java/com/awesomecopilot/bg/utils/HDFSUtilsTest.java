package com.awesomecopilot.bg.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class HDFSUtilsTest {

	@Test
	public void testMkdirs() {
		boolean maked = HDFSUtils.mkdir("/rico");
		assertTrue(maked);
	}
}
