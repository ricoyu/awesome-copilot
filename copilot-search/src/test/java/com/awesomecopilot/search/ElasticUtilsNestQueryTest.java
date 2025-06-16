package com.awesomecopilot.search;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ElasticUtilsNestQueryTest {

	@Test
	public void testNestedQuery() {
		List<String> ecommerces = ElasticUtils.Query.matchQuery("ecommerce")
				.nestedPath("comments")
				.query("comments.user", "user123")
				.queryForList();
		assertThat(ecommerces).hasSize(3);
		for (String ecommerce : ecommerces) {
			System.out.println(ecommerce);
		}
	}

	@Test
	public void testNestedBoolQuery() {
		List<Object> ecommerces = ElasticUtils.Query.bool("ecommerce")
				.nestedPath("comments")
				.match("comments.user", "user123").must()
				.range("comments.rating").gte(4.5).must()
				.queryForList();
		assertThat(ecommerces).hasSize(2);
		for (Object ecommerce : ecommerces) {
			System.out.println(ecommerce);
		}
	}
}
