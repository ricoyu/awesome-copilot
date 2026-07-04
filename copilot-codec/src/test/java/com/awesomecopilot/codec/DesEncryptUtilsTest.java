package com.awesomecopilot.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DesEncryptUtilsTest {
	
	@Test
	public void testEncryptJaJaPwd() {
		String encrypt = DesEncryptUtils.encrypt("123456", "ricoyu_521@hotmail.com");
		System.out.println(encrypt);
	}
	
	@Test
	public void testEncrypt() {
		String encrypt = DesEncryptUtils.encrypt("123456", "zoe@jajalink.com");
		assertEquals("FnwV1nCLDLs=", encrypt);
		System.out.println(encrypt);
	}
	
	@Test
	public void testDescript() {
		String encrypt = DesEncryptUtils.encrypt("123456", "zoe@jajalink.com");
		assertEquals("FnwV1nCLDLs=", encrypt);
		System.out.println(encrypt);
		String passwd = DesEncryptUtils.decrypt("FnwV1nCLDLs=", "zoe@jajalink.com");
		assertEquals("123456", passwd);
		System.out.println(passwd);
	}
}
