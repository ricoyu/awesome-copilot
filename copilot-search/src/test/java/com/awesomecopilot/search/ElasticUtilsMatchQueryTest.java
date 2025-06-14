package com.awesomecopilot.search;

import org.elasticsearch.index.query.Operator;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * <p>
 * Copyright: (C), 2023-08-01 9:08
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticUtilsMatchQueryTest {
	
	@Test
	public void testMatch() {
		List<Object> movies = ElasticUtils.Query.matchQuery("movies")
				.query("title", "King George")
				.queryForList();
		assertThat(movies.size() == 10);
	}

	@Test
	public void testMatchWithOperator() {
		List<Object> movies = ElasticUtils.Query.matchQuery("movies")
				.query("title", "King George")
				.operator(Operator.AND)
				.queryForList();
		assertThat(movies.size() == 1);
	}
	
	@Test
	public void testMatchWithMinimunShouldMatch() {
		List<Object> movies = ElasticUtils.Query.matchQuery("movies")
				.query("title", "Matrix Reload")
				.minimumShouldMatch(1)
				.queryForList();
		assertThat(movies.size() == 3);
	}
}
