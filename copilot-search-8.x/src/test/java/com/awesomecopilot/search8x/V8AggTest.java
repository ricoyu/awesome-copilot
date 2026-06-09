package com.awesomecopilot.search8x;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.awesomecopilot.json.jackson.JacksonUtils.toPrettyJson;

/**
 * ES 8.x 原生 API 聚合测试
 * 
 * 这些测试使用 co.elastic.clients.elasticsearch API,而不是 RestHighLevelClient
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
@Slf4j
public class V8AggTest {

	/**
	 * 测试 ES 8.x 原生 Terms 聚合
	 */
	@Test
	public void testV8TermsAgg() {
		List<Map<String, Object>> results = ElasticUtils.AggsV8.terms("employees")
				.of("jobs", "job")
				.size(20)
				.get();

		System.out.println(toPrettyJson(results));
	}

	/**
	 * 测试 ES 8.x 原生 Terms 聚合 - bank 索引
	 */
	@Test
	public void testV8BankAgeTerms() {
		List<Map<String, Object>> results = ElasticUtils.AggsV8.terms("bank")
				.of("age_agg", "age")
				.size(20)
				.get();

		results.forEach(result -> System.out.println(toPrettyJson(result)));
	}
}
