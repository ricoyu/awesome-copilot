package com.awesomecopilot.search8x;


import org.junit.jupiter.api.Test;

import java.util.List;

public class ElasticUtilsQueryStringQueryTest {

	@Test
	public void testQueryWithDefaultField() {
		List<String> books = ElasticUtils.Query.queryString("books")
				.defaultField("title")
				.query("Easlticsearch AND -categories:beginner")
				.queryForList();

		for (String book : books) {
			System.out.println(book);
		}
	}
}