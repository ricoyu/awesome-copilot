package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ElasticUtilsBoolTest {

	@Test
	public void testBool() {
		List<Object> products = ElasticUtils.Query.bool("product")
				.match("name", "游戏手机")
				.should()
				.match("desc", "游戏手机")
				.queryForList();

		products.forEach(System.out::println);
	}
	
	@Test
	public void testBankBool() {
		List<Object> banks = Query.bool("bank")
				.match("age", "40").must()
				.match("state", "ID").mustNot()
				.size(100)
				.queryForList();
		assertThat( banks.size()).isEqualTo(43);
		banks.forEach(System.out::println);
	}
	
	/**
	 * 对应这个搜索:
	 * <pre>
	 * POST /bank/_search
	 * {
	 *   "query": {
	 *     "bool": {
	 *       "must": [
	 *         {"match": {
	 *           "gender": "M"
	 *         }},
	 *         {"match": {
	 *           "address": "mill"
	 *         }}
	 *       ],
	 *       "should": [
	 *         {"match": {
	 *           "state": "AK"
	 *         }}
	 *       ]
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testMustMustShould() {
		List<Object> banks = Query.bool("bank")
				.match("gender", "M").must()
				.match("address", "mill").must()
				.match("state", "AK").should()
				.queryForList();
		assertThat( banks.size()).isEqualTo(3);
		banks.forEach(System.out::println);
	}
}