package com.awesomecopilot.search8x;


import org.junit.jupiter.api.Test;

import java.util.List;

public class ElasticUtilsMatchPhraseTest {

	@Test
	public void testMatchPhrase() {
		List<Object> movies = ElasticUtils.Query.matchPhraseQuery("movies")
				.query("title", "one love")
				.slop(1)
				.queryForList();

		for (Object movie : movies) {
			System.out.println(movie);
		}
	}
}