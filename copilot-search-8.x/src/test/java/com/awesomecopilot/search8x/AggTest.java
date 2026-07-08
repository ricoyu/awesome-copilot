package com.awesomecopilot.search8x;

import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils.Aggs;
import com.awesomecopilot.search8x.builder.agg.sub.SubAggregations;
import com.awesomecopilot.search8x.builder.agg.support.RangeAggResult;
import com.awesomecopilot.search8x.builder.query.ElasticMatchQueryBuilder;
import com.awesomecopilot.search8x.support.StatsAggResult;
import com.awesomecopilot.search8x.support.ValueCountAggResult;
import com.awesomecopilot.search8x.vo.ElasticPage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.json.jackson.JacksonUtils.toPrettyJson;

/**
 * 聚合分析
 * <p>
 * Copyright: (C), 2021-06-03 9:17
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class AggTest {
	
	/**
	 * 基础单值指标（value_count/sum/avg/max/min/count）
	 */
	@Test
	public void testBasicValue() {
		ValueCountAggResult result = ElasticUtils.Aggs.valueCount("ecommerce_order_v2")
				.of("total_order_count", "order_no")
				.get();
		System.out.println(toPrettyJson(result));
	}

	@Test
	public void testBankAgeTerms() {
		List<Map<String, Object>> result = Aggs.terms("bank")
				.of("age_agg", "age")
				.size(20)
				//.sort("key:asc")
				.get();

		System.out.println(JacksonUtils.toPrettyJson(result));
	}


	@Test
	public void testTermsAvgAgg() {
		ElasticMatchQueryBuilder queryBuilder = ElasticUtils.Query.matchQuery("bank")
				.query("address", "mill");
		Map<String, Object> map = ElasticUtils.Aggs.composite("bank")
				.terms("age_agg", "age").and()
				.avg("age_avg", "age")
				.setQuery(queryBuilder)
				.get();
		System.out.println(toPrettyJson(map));
	}

	/**
	 * 查出所有年龄分布, 并且这些年龄段中M的平均薪资以及这个年龄段所有的薪资情况
	 * <p>
	 * 相当于下面这个Query DSL
	 *
	 * <pre>
	 * POST bank/_search
	 * {
	 *   "query": {
	 *     "match_all": {}
	 *   },
	 *   "size": 0,
	 *   "aggs": {
	 *     "age_agg": {
	 *       "terms": {
	 *         "field": "age",
	 *         "size": 20
	 *       },
	 *       "aggs": {
	 *         "gender_agg": {
	 *           "terms": {
	 *             "field": "gender.keyword",
	 *             "size": 10
	 *           },
	 *           "aggs": {
	 *             "salary_avg": {
	 *               "stats": {
	 *                 "field": "balance"
	 *               }
	 *             }
	 *           }
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testAgeGenderSubAgg() {
		List<Map<String, Object>> result = ElasticUtils.Aggs.terms("bank")
				.of("age_term", "age")
				.subAggregation(SubAggregations.terms("gender_term", "gender.keyword"))
				.subAggregation(SubAggregations.avg("balance_avg", "balance"))
				.get();

		System.out.println(toPrettyJson(result));
	}

	@Test
	public void testAgeTermsSalarySubAgg() {
		//按照年龄段聚合, 并请求这些年龄段的人的平均工资
		List<Map<String, Object>> aggResult = ElasticUtils.Aggs.terms("bank")
				.of("age_term", "age")
				.subAggregation(SubAggregations.avg("salary_avg", "balance"))
				.get();

		System.out.println(toPrettyJson(aggResult));
	}

	@Test
	public void testAgeGenderStatsNestedSubAgg() {
		// 查出所有年龄分布，并且这些年龄段中各性别的薪资统计情况
		// 等价 DSL:
		// aggs: age_agg(terms age) -> gender_agg(terms gender.keyword) -> salary_stats(stats balance)
		List<Map<String, Object>> aggResult = ElasticUtils.Aggs.terms("bank")
				.of("age_agg", "age").size(20)
				.subAggregation(SubAggregations.terms("gender_agg", "gender.keyword").size(10)
								.subAggregation(SubAggregations.stats("salary_stats", "balance"))
				)
				.get();

		System.out.println(toPrettyJson(aggResult));
	}


	@Test
	public void testStatAgg() {
		StatsAggResult statsAggResult = ElasticUtils.Aggs.stats("employees")
				.of("stats_salary", "salary")
				.get();

		System.out.println(toPrettyJson(statsAggResult));
	}

	@Test
	public void testTermsAgg() {
		List<Map<String, Object>> results = ElasticUtils.Aggs.terms("employees")
				.of("jobs", "job.keyword")
				.size(20)
				.sort("key:asc")
				.get();

		System.out.println(toPrettyJson(results));
	}

	@Test
	public void testAggOnDestContry() {
		List<Map<String, Object>> aggResults = ElasticUtils.Aggs.terms("kibana_sample_data_flights")
				.of("flight_dest", "DestCountry")
				.sort("count")
				.get();

		System.out.println(toPrettyJson(aggResults));
	}

	@Test
	public void testFlightDest() {
		List<Map<String, Object>> resultMap = ElasticUtils.Aggs.terms("kibana_sample_data_flights")
				.of("flight_dest", "DestCountry")
				.subAggregation(SubAggregations.avg("average_price", "AvgTicketPrice"))
				.subAggregation(SubAggregations.max("max_price", "AvgTicketPrice"))
				.subAggregation(SubAggregations.min("min_price", "AvgTicketPrice"))
				.get();

		System.out.println(toPrettyJson(resultMap));
	}

	@Test
	public void testCardinalityAgg() {
		Long count = ElasticUtils.Aggs.cardinality("employees")
				.of("cardinality_agg", "job.keyword")
				.get();

		System.out.println(count);
	}

	@Test
	public void testMin() {
		Double minSalary = ElasticUtils.Aggs.min("employees")
				.of("min_salary", "salary")
				.get();
		log.info("Min Salary: {}", minSalary);
	}

	@Test
	public void testMax() {
		Double maxSalary = ElasticUtils.Aggs.max("employees")
				.of("max_salary", "salary")
				.get();
		log.info("Max Salary: {}", maxSalary);
	}

	@Test
	public void testAvg() {
		Double avgSalary = ElasticUtils.Aggs.avg("employees")
				.of("avg_salary", "salary")
				.get();
		log.info("Avg Salary: {}", avgSalary);
	}

	@Test
	public void testAvgNotAccurate() {
		Double avgRating = ElasticUtils.Aggs.avg("ratings")
				.of("rating_avg", "rating")
				.get();
		System.out.println(avgRating);
	}

	@Test
	public void testSum() {
		Double sum = ElasticUtils.Aggs.sum("employees")
				.of("sum_agg", "age")
				.get();
		System.out.println(sum);
	}

	@Test
	public void testComposite() {
		Map<String, Object> result = ElasticUtils.Aggs.composite("employees")
				.min("min_salary", "salary")
				.max("max_salary", "salary")
				.avg("avg_salary", "salary")
				//.terms("term_agg", "age").and()
				.get();

		System.out.println(toPrettyJson(result));
	}

	@Test
	public void testTopHitsAgg() {
		List<Map<String, Object>> result = ElasticUtils.Aggs.terms("employees")
				.of("jobs_agg", "job.keyword")
				.subAggregation(SubAggregations.topHits("old_employee")
						.sort("age:desc")
						.size(3))
				.get();

		System.out.println(toPrettyJson(result));
	}

	@Test
	public void testRangeAgg() {
		Map<String, List<RangeAggResult>> map = ElasticUtils.Aggs.range("employees")
				.of("salary_range", "salary")
				.addRange(10000, 20000)
				.addUnboundedTo(10000)
				.addUnboundedFrom(">20000", 20000)
				.get();

		System.out.println(toPrettyJson(map));
	}

	@Test
	public void testNestAgg() {
		List<Map<String, Object>> result = ElasticUtils.Aggs.terms("employees")
				.of("job_term", "job.keyword")
				.subAggregation(SubAggregations.stats("salary_stats", "salary"))
				.get();

		System.out.println(toPrettyJson(result));
	}

	@Test
	public void testQueryThenAgg() {
		var rangeQueryBuilder = ElasticUtils.Query.range("employees").field("age").gt(20);
		List<Map<String, Object>> aggResults = ElasticUtils.Aggs.terms("employees")
				.of("jobs", "job.keyword")
				.setQuery(rangeQueryBuilder)
				.get();

		System.out.println(toPrettyJson(aggResults));
	}

	@Test
	public void testQueryThenComposite() {
		var rangeQueryBuilder = ElasticUtils.Query.range("event_2021-07-05")
				.field("create_time")
				.gte(1625414400000L)
				.lte(1625500740000L);

		Map<String, Object> stringObjectMap = ElasticUtils.Aggs.composite("event_2021-07-05")
				.setQuery(rangeQueryBuilder)
				.terms("severity", "severity").and()
				.terms("attach_result", "attack_result").and()
				.terms("attacker_ip", "attacker_ip").size(5).and()
				.terms("victim_ip", "victim_ip").size(5).and()
				.fetchTotalHits(true)
				.get();

		System.out.println(toJson(stringObjectMap));
	}

	@Test
	public void testMultiTerms() {
		List<Map<String, Object>> results = ElasticUtils.Aggs.multiTerms("event_*")
				.of("multi_terms_agg", "src_ip", "src_port")
				.get();

		results.forEach(result -> System.out.println(toPrettyJson(result)));
	}

	@Test
	public void testMultiTermsBucketSort() {
		List<Map<String, Object>> results = ElasticUtils.Aggs.multiTerms("event_*")
				.of("multi_terms_agg", "src_ip", "src_port")
				.get();

		ElasticPage page = ElasticUtils.Aggs.multiTerms("event_*")
				.of("multi_terms_agg", "src_ip", "src_port")
				.sort("-count")
				.subAggregation(SubAggregations.bucketSort("multi_terms_sort")
						.paging(0, 5))
				.getPage();
		page.setTotalCount(results.size());
		log.info("第{}页, 每页{}条, 总共{}条", page.getPageNum(), page.getPageSize(), page.getTotalCount());
		page.getResults().forEach((result) -> System.out.println(toPrettyJson(result)));


		page = ElasticUtils.Aggs.multiTerms("event_*")
				.of("multi_terms_agg", "src_ip", "src_port")
				.sort("-count")
				.subAggregation(SubAggregations.bucketSort("multi_terms_sort")
						.paging(5, 5))
				.getPage();
		page.setTotalCount(results.size());
		log.info("第{}页, 每页{}条, 总共{}条", page.getPageNum(), page.getPageSize(), page.getTotalCount());
		page.getResults().forEach((result) -> System.out.println(toPrettyJson(result)));
	}

	@Test
	public void testTerms() {
		List<Map<String, Object>> maps = ElasticUtils.Aggs.terms("event_*")
				.of("src_ip_term", "src_ip")
				.sort("count")
				.get();

		System.out.println(toJson(maps));
	}

	@Test
	public void testTermsPage() {
		ElasticPage page = ElasticUtils.Aggs.terms("event_*")
				.of("src_ip_term", "src_ip")
				.sort("count:desc")
				.subAggregation(SubAggregations.bucketSort("src_ip_term_sort")
						.paging(0, 4))
				.getPage();

		List<Map<String, Object>> results = ElasticUtils.Aggs.terms("netlog_2021-07-16")
				.of("src_ip_term", "src_ip")
				.get();
		page.setTotalCount(results.size());
		log.info("第{}页, 每页{}条, 总共{}条", page.getPageNum(), page.getPageSize(), page.getTotalCount());
		page.getResults().forEach((result) -> System.out.println(toPrettyJson(result)));

		page = ElasticUtils.Aggs.terms("event_*")
				.of("src_ip_term", "src_ip")
				.sort("count:desc")
				.subAggregation(SubAggregations.bucketSort("src_ip_term_sort")
						.paging(4, 4))
				.getPage();
		page.setTotalCount(results.size());
		log.info("第{}页, 每页{}条, 总共{}条", page.getPageNum(), page.getPageSize(), page.getTotalCount());
		page.getResults().forEach((result) -> System.out.println(toPrettyJson(result)));
	}

	/**
	 * 组合多个 Metric 聚合: value_count + sum + avg + max + min
	 * <p>
	 * 等价 DSL:
	 * <pre>
	 * POST ecommerce_order_v2/_search
	 * {
	 *   "size": 0,
	 *   "aggs": {
	 *     "total_order_count": { "value_count": { "field": "order_no" } },
	 *     "total_sales":       { "sum":         { "field": "price" } },
	 *     "avg_price":         { "avg":         { "field": "price" } },
	 *     "max_price":         { "max":         { "field": "price" } },
	 *     "min_price":         { "min":         { "field": "price" } }
	 *   }
	 * }
	 * </pre>
	 */
	@Test
	public void testCompositeMetricAggs() {
		Map<String, Object> result = ElasticUtils.Aggs.composite("ecommerce_order_v2")
				.valueCount("total_order_count", "order_no")
				.sum("total_sales", "price")
				.avg("avg_price", "price")
				.max("max_price", "price")
				.min("min_price", "price")
				.get();

		System.out.println(toPrettyJson(result));
	}
}
