package com.awesomecopilot.search;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

/**
 * <p>
 * Copyright: (C), 2023-08-01 22:29
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticUtilsBoolQueryTest {
	
	@Test
	public void testBoolMust() {
		List<Object> movies = ElasticUtils.Query
				.bool("newmovies")
				.term("genre.keyword", "Comedy").must()
				.term("genre_count", 1).must()
				.queryForList();
		assertEquals(movies.size(), 1);
	}
	
	@Test
	public void testBoolFilter() {
		List<Object> movies = ElasticUtils.Query
				.bool("newmovies")
				.term("genre.keyword", "Comedy").filter()
				.term("genre_count", 1).filter()
				.queryForList();
		assertEquals(movies.size(), 1);
				
	}
	
	@Test
	public void testBoolQueryOptimize() {
		List<Object> news = ElasticUtils.Query
				.bool("news")
				.match("content", "apple").must()
				.match("content", "pie").mustNot()
				.queryForList();
		 assertEquals(2, news.size());
	}
	
	@Test
	public void testBoolBoostingQuery() {
	}

	/**
	 * 对应的Query DSL
	 * <pre>
	 * GET ecommerce/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "must": [
	 *         {"range": {
	 *           "price": {
	 *             "gte": 1000,
	 *             "lte": 5000
	 *           }
	 *         }}
	 *       ],
	 *       "should": [
	 *         {"term": {
	 *           "category": "手机"
	 *         }},
	 *         {
	 *           "term": {
	 *             "category":"笔记本"
	 *           }
	 *         }
	 *       ],
	 *       "must_not": [
	 *         {"term": {
	 *           "tags": "苹果"
	 *         }}
	 *       ],
	 *       "filter": [
	 *         {"range": {
	 *           "rating": {
	 *             "gte": 4.5
	 *           }
	 *         }}
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testComplexBoolQuery() {
		List<Object> objects = ElasticUtils.Query.bool("ecommerce")
				.range("price").gte(1000).lte(5000).must()
				.term("category", "笔记本").should()
				.term("category", "手机").should()
				.term("tags", "苹果").mustNot()
				.range("rating").gte(4.5).filter()
				.queryForList();

		objects.forEach(System.out::println);
		assertThat(objects.size()).isEqualTo(1);
	}

	/**
	 * <pre>
	 * GET ecommerce/_search
	 * {
	 *   "query": {
	 *     "match_phrase": {
	 *       "description": {
	 *         "query": "旗舰手机",
	 *         "slop": 2
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testMatchPgrase() {
		List<Object> ecommerces = ElasticUtils.Query.matchPhraseQuery("ecommerce")
				.query("description", "旗舰手机")
				.slop(2)
				.queryForList();
		ecommerces.forEach(System.out::println);
		assertThat(ecommerces.size()).isEqualTo(2);
	}
	
	@Test
	public void testDateRagequery() {
		List<String> ecommerces = ElasticUtils.Query.range("ecommerce")
				.field("created_at").gte("2023-12-01||-1y/y").lte("2023-12-01")
				.includeSources("_id")
				.queryForList();
		ecommerces.forEach(System.out::println);
		assertThat(ecommerces.size()).isEqualTo(4);
	}
}
