package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

public class QueryTest {
	
	@Test
	public void testQueryById() {
		String test = ElasticUtils.get("test", 1);
		System.out.println(test);
	}
	
	@Test
	public void testMatchAll() {
		List<Object> tests = Query.matchAllQuery("test").queryForList();
		for (Object test : tests) {
			System.out.println(test);
		}
	}
	
	@Test
	public void testQueryWithPage() {
		List<Object> tests = Query.matchAllQuery("test")
				.from(0)
				.size(2)
				.queryForList();
		for (Object test : tests) {
			System.out.println(test);
		}
	}
}
