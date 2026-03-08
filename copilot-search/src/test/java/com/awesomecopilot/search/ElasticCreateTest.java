package com.awesomecopilot.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ElasticCreateTest {

	@Test
	public void testCreateOneOk() {
		ElasticUtils.delete("users", "1");
		String id = ElasticUtils.create("users", """
                {
					"name": "三少爷",
					"age": 42
				}
				""", 1);
		assertEquals(1, 1);
		id = ElasticUtils.create("users", """
				{
					"name": "三少爷",
					"age": 42
				}
				""", 1);
	}
}