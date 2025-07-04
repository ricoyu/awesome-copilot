package com.awesomecopilot.search;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search.ElasticUtils.Aggs;
import com.awesomecopilot.search.builder.ElasticRangeQueryBuilder;
import com.awesomecopilot.search.builder.agg.sub.SubAggregations;
import com.awesomecopilot.search.enums.CalendarInterval;
import com.awesomecopilot.search.enums.FixedInterval;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.json.jackson.JacksonUtils.toPrettyJson;
import static com.awesomecopilot.search.builder.agg.sub.SubAggregations.avg;
import static com.awesomecopilot.search.builder.agg.sub.SubAggregations.dateHistogram;
import static org.elasticsearch.search.aggregations.AggregationBuilders.max;
import static org.elasticsearch.search.aggregations.AggregationBuilders.min;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

/**
 * <p>
 * Copyright: (C), 2021-05-10 10:42
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class ElasticAggsTest {
	
	/**
	 * 统计每种颜色的汽车数量
	 */
	@Test
	public void testAggRaw() {
		TermsAggregationBuilder arrregationBuilder = AggregationBuilders.terms("popular_colors")
				.field("color.keyword");
		SearchResponse searchResponse = ElasticUtils.CLIENT.prepareSearch("cars")
				.addAggregation(arrregationBuilder)
				.get();
		Aggregations aggregations = searchResponse.getAggregations();
		
		for (Aggregation aggregation : aggregations) {
			Map<String, Object> metaData = aggregation.getMetadata();
			String name = aggregation.getName();
			System.out.println("Aggregation: " + name);
			List<Bucket> buckets = ((StringTerms) aggregation).getBuckets();
			for (Bucket bucket : buckets) {
				String key = bucket.getKeyAsString();
				long docCount = bucket.getDocCount();
				System.out.println("Key: " + key + ", Doc Count: " + docCount);
			}
		}
	}
	
	@Test
	public void testAgg() {
		//List<Map<String, Object>> aggResults = Aggs.terms("cars")
		//		.of("cars-color", "color.keyword")
		//		.size(5)
		//		.get();
		//assertThat(aggResults.size()).isEqualTo(3);
		//System.out.println(toPrettyJson(aggResults));
		
		List<Map<String, Object>> aggResults1 = Aggs.terms("kibana_sample_data_flights")
				.of("dest-country", "DestCountry")
				.sort("count")
				.get();
		System.out.println(toPrettyJson(aggResults1));
	}
	
	/**
	 * 查看航班目的地的统计信息, 增加平均, 最高最低价格
	 */
	@Test
	public void testFlightTermsMinMaxAvg() {
		TermsAggregationBuilder termsAggregationBuilder = terms("flight_dest").field("DestCountry")
				.subAggregation(AggregationBuilders.avg("avg_price").field("AvgTicketPrice"))
				.subAggregation(max("max_price").field("AvgTicketPrice"))
				.subAggregation(min("min_price").field("AvgTicketPrice"));
		
		SearchResponse response = ElasticUtils.CLIENT.prepareSearch("kibana_sample_data_flights")
				.setSize(0)
				.addAggregation(termsAggregationBuilder)
				.get();
		
		Aggregations aggregations = response.getAggregations();
		for (Aggregation aggregation : aggregations) {
			System.out.println("Aggregation: " + aggregation.getName());
			List<Bucket> buckets = ((StringTerms) aggregation).getBuckets();
			for (Bucket bucket : buckets) {
				String key = bucket.getKeyAsString();
				long docCount = bucket.getDocCount();
				System.out.println("Key: " + key + ", Doc Count: " + docCount);
				
				Aggregations subAggs = bucket.getAggregations();
				if (subAggs != null) {
					for (Aggregation subAgg : subAggs) {
						System.out.println(toJson(subAgg));
						String name = subAgg.getName(); //max_price
						String writeableName = ((NamedWriteable) subAgg).getWriteableName(); //max min 等聚合的类型
						Object value = ReflectionUtils.getFieldValue(writeableName, subAgg);
						if (writeableName.equals("avg")) {
							value = ReflectionUtils.invokeMethod("getValue", subAgg);
						}
						System.out.println(writeableName + ":" + value);
					}
				}
			}
		}
	}
	
	@Test
	public void testHistogramAgg() {
		Map<String, Object> resultMap = Aggs.histogram("employees")
				.of("salary_histogram", "salary")
				.interval(5000)
				.extendedBounds(0L, 100000L)
				.get();
		
		System.out.println(toPrettyJson(resultMap));
	}
	
	@Test
	public void testHistogramAggWithMinDocCountAndExtendedBounds() {
		//long min = 1625642342501L - 5 * 60 * 60 * 1000L;
		long min = 1625642342501L;
		long max = 1625642350168L;
		
		log.info("Min {}", new Date(min));
		log.info("Max {}", new Date(max));
		
		ElasticRangeQueryBuilder rangeQueryBuilder = ElasticUtils.Query.range("event_2021-07-07")
				.field("create_time")
				.gte(min)
				.lte(max);
		
		Map<String, Object> resultMap = Aggs.histogram("event_2021-07-07")
				.of("event_count_agg", "create_time")
				.setQuery(rangeQueryBuilder)
				.interval(2000)
				.minDocCount(0)
				//.extendedBounds(min - 10 * 1000, max)
				.get();
		
		log.info(toPrettyJson(resultMap));
	}
	
	@Test
	public void testDateHistogramAggWithMinDocCountAndExtendedBounds() {
		//long min = 1625642342501L - 5 * 60 * 60 * 1000L;
		long min = 1625642342501L;
		long max = 1625642350168L;
		
		log.info("Min {}", new Date(min));
		log.info("Max {}", new Date(max));
		
		ElasticRangeQueryBuilder rangeQueryBuilder = ElasticUtils.Query.range("event_2021-07-07")
				.field("create_time")
				.gte(min)
				.lte(max);
		
		Map<String, Object> resultMap = Aggs.dateHistogram("event_2021-07-07")
				.of("event_count_agg", "create_time")
				.setQuery(rangeQueryBuilder)
				.calendarInterval(CalendarInterval.MINUTE)
				.minDocCount(0)
				//.extendedBounds(min - 10 * 1000, max)
				.get();
		
		log.info(toPrettyJson(resultMap));
	}
	
	@Test
	public void testSubAgg() {
		List<Map<String, Object>> resultMap = Aggs.terms("event_2021-08-02")
				.of("event_engine_agg", "event_engine")
				.subAggregation(SubAggregations.histogram("create_time_agg", "create_time")
						.interval(1000)
						.minDocCount(0)
						.extendedBounds(1627874204597L, 1627874204597L))
				.get();
		/*List<Map<String, Object>> resultMap = Aggs.terms("event_2021-08-02")
				.of("event_engine_agg", "event_engine")
				.subAggregation(SubAggregation.instance(AggregationBuilders.histogram("create_time_agg")
						.field("create_time")
						.interval(10000)
						.minDocCount(0)
						.extendedBounds(1627874190000L, 1627874200000L)))
				.get();*/
		
		log.info(toPrettyJson(resultMap));
	}
	
	@Test
	public void testSubDateAgg() {
		List<Map<String, Object>> resultMap = Aggs.terms("event_2021-08-02")
				.of("event_engine_agg", "event_engine")
				.subAggregation(dateHistogram("create_time_agg", "create_time")
						.calendarInterval(CalendarInterval.MINUTE)
						.minDocCount(0)
						.extendedBounds(1627874204597L, 1627874204597L))
				.get();
		
		log.info(toPrettyJson(resultMap));
	}
	
	@Test
	public void testDateHistogramSubAvgAgg() {
		Map<String, Object> resultMap = Aggs.dateHistogram("event_2021-08-02")
				.of("date_his_agg", "create_time")
				.fixedInterval(5, FixedInterval.MINUTES)
				.subAggregation(avg("event_count_avg", "event_count"))
				.get();
		
		System.out.println(toJson(resultMap));
	}
	
	@Test
	public void testTermsThenDateHistogramThenAvg() {
		/*ElasticSubAggregation subAggregation =
				ElasticSubAggregation.instance(AggregationBuilders.dateHistogram("time_agg").field("timestamp").fixedInterval(DateHistogramInterval.DAY))
						.subAggregation(AggregationBuilders.avg("in_bytes_avg").field("in_bytes"));*/
		
		List<Map<String, Object>> results = Aggs.terms("flow_2021-08-18")
				.of("tags_agg", "tags")
				.subAggregation(dateHistogram("time_agg", "timestamp")
					.fixedInterval(5, FixedInterval.DAYS)
					.avgSubAggregation("in_bytes_avg", "in_bytes"))
				.get();
		
		System.out.println(toPrettyJson(results));
	}
	
	@Test
	public void testElasticTopHitsSubAggregation() {
		List<Map<String, Object>> resultMap = Aggs.terms("flow_*")
				.of("agg_data", "tags")
				.subAggregation(SubAggregations.topHits("top_hits")
						.sort("-timestamp")
						.size(2)
						.includeSources("tags", "status", "in_bytes", "speed", "type"))
				.get();
		
		log.info(toPrettyJson(resultMap));
	}

	/**
	 * POST kibana_sample_data_flights/_search
	 * {
	 *   "size": 0,
	 *   "aggs": {
	 *     "flight_dest": {
	 *       "terms": {
	 *         "field": "DestCountry",
	 *         "size": 2
	 *       },
	 *       "aggs": {
	 *         "average_price": {
	 *           "avg": {
	 *             "field": "AvgTicketPrice"
	 *           }
	 *         },
	 *         "weather": {
	 *           "terms": {
	 *             "field": "DestWeather"
	 *           }
	 *         }
	 *       }
	 *     }
	 *   }
	 */
	@Test
	public void testDestCountrythenAvgThenWeather() {
		List<Map<String, Object>> resultMap = Aggs.terms("kibana_sample_data_flights")
				.of("flight_dest", "DestCountry").size(2)
				.subAggregation(avg("average_price", "AvgTicketPrice"))
				.subAggregation(SubAggregations.terms("weather", "DestWeather"))
				.get();
		System.out.println(toPrettyJson(resultMap));
	}
	
	@Test
	public void testDestCountrythenStatsThenWeather() {
		List<Map<String, Object>> resultMap = Aggs.terms("kibana_sample_data_flights")
				.of("flight_dest", "DestCountry").size(2)
				.subAggregation(SubAggregations.stats("state_price", "AvgTicketPrice"))
				.subAggregation(SubAggregations.terms("weather", "DestWeather").size(5))
				.get();
		System.out.println(toPrettyJson(resultMap));
	}

	@Test
	public void testSubAgg2() {
		List<Map<String, Object>> aggResult = Aggs.terms("kibana_sample_data_flights")
				.of("flight_dest", "DestCountry")
				.size(2)
				.subAggregation(avg("average_price", "AvgTicketPrice"))
				.subAggregation(SubAggregations.terms("weather", "DestWeather").size(5))
				.get();

		System.out.println(toPrettyJson(aggResult));
	}

	/**
	 * POST kibana_sample_data_flights/_search
	 * {
	 *   "size": 0,
	 *   "aggs": {
	 *     "flight_dest": {
	 *       "terms": {
	 *         "field": "DestCountry",
	 *         "size": 3
	 *       },
	 *       "aggs": {
	 *         "average_price": {
	 *           "avg": {
	 *             "field": "AvgTicketPrice"
	 *           }
	 *         },
	 *         "max_price":{
	 *           "max": {
	 *             "field": "AvgTicketPrice"
	 *           }
	 *         },
	 *         "min_price":{
	 *           "min": {
	 *             "field": "AvgTicketPrice"
	 *           }
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 */
	@Test
	public void testSubAgg3() {
		List<Map<String, Object>> results = Aggs.terms("kibana_sample_data_flights").of("flight_dest", "DestCountry").size(3)
				.subAggregation(avg("average_price", "AvgTicketPrice"))
				.subAggregation(SubAggregations.max("max_price", "AvgTicketPrice"))
				.subAggregation(SubAggregations.min("min_price", "AvgTicketPrice"))
				.get();

		System.out.println(toPrettyJson(results));
	}

	/**
	 * POST kibana_sample_data_flights/_search
	 * {
	 *   "size": 0,
	 *   "aggs": {
	 *     "flight_dest": {
	 *       "terms": {
	 *         "field": "DestCountry",
	 *         "size": 2
	 *       },
	 *       "aggs": {
	 *         "average_price": {
	 *           "avg": {
	 *             "field": "AvgTicketPrice"
	 *           }
	 *         },
	 *         "weather": {
	 *           "terms": {
	 *             "field": "DestWeather",
	 *             "size": 5
	 *           }
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 */
	@Test
	public void testBucketMixMetric() {
		List<Map<String, Object>> results = Aggs.terms("kibana_sample_data_flights")
				.of("flight_dest", "DestCountry").size(2)
				.subAggregation(avg("average_price", "AvgTicketPrice"))
				.subAggregation(SubAggregations.terms("weather", "DestWeather").size(5))
				.get();
		System.out.println(toPrettyJson(results));
	}

	@Test
	public void testRangeAgg() {
		Map<String, Object> resultMap = Aggs.range("products")
				.of("price_ranges", "price")
				.addUnboundedTo(100)
				.addRange(100, 500)
				.addUnboundedFrom(500)
				.get();

		System.out.println(toPrettyJson(resultMap));
	}
}
