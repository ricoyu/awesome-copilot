package com.awesomecopilot.search;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ElasticUtilsUriQueryTest {


	@Test
	public void testByFirstName() {
		List<Object> ecommerces = ElasticUtils.Query.uriQuery("kibana_sample_data_ecommerce")
				.query("customer_first_name=Eddie")
				.queryForList();

		ecommerces.forEach(System.out::println);
	}

	@Test
	public void testQueryWithSpace() {
		List<Object> books = ElasticUtils.Query.queryString("books")
				.query("author:\"Clinton Gormley\"")
				.queryForList();
		books.forEach(System.out::println);
	}

	@Test
	public void testSearchInMultiFields() {
		List<Object> books = ElasticUtils.Query.queryString("books")
				.fields("title", "description")
				.query("elasticsearch AND guide")
				.queryForList();
		books.forEach(System.out::println);
	}
	
	@Test
	public void testBoolcomp() {
		List<Object> books = ElasticUtils.Query.queryString("books")
				.query("(title:elasticsearch OR description:Elasticsearch) AND rating:[4 TO 5]")
				.queryForList();
		assertThat(books.size()).isEqualTo(5);
	}

	@Test
	public void testPhraseQuery() {
		List<Object> books = ElasticUtils.Query.queryString("books")
				.query("description:\"powerful search applications\"")
				.queryForList();
		assertThat(books.size()).isEqualTo(1);
	}

	@Test
	public void testExcludeQuery() {
		List<Object> books = ElasticUtils.Query.queryString("books")
				.query("title:Easlticsearch AND -categories:beginner")
				.queryForList();
		assertThat(books.size()).isEqualTo(0);
	}

	@Test
	public void testQueryWithWeight() {
		List<Object> books = ElasticUtils.Query.queryString("books")
				.query("search")
				.fields("title^2", "description")
				.queryForList();
		books.forEach(System.out::println);
	}
}
