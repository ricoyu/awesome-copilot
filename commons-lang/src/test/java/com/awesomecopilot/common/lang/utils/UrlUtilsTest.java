package com.awesomecopilot.common.lang.utils;

import org.junit.Test;

public class UrlUtilsTest {

	@Test
	public void testEncode() {
		//String url = "http://192.168.100.101:9200/movies/_search?q=year:>=1980&sort=year:asc";
		String url = "q=year:>=1980";
		String encoded = UrlUtils.encodeUrl(url);
		System.out.println(encoded);
	}


}
