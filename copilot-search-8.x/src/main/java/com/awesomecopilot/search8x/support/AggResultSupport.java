package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.RangeAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.AvgAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.ExtendedStatsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.ValueCountAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ES 8.x 原生聚合结果解析器
 * <p>
 * 直接解析 ES 8.x Java Client 返回的聚合响应，不依赖 7.x API
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public final class AggResultSupport {

	private static final Logger log = LoggerFactory.getLogger(AggResultSupport.class);

	private AggResultSupport() {
	}

	/**
	 * 解析 Terms 聚合结果
	 * <p>
	 * 支持 StringTerms 和 LongTerms 两种类型
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @return 聚合结果列表，每个元素是一个桶的统计信息
	 */
	public static <T> List<Map<String, T>> termsResult(Map<String, Aggregate> aggregations) {
		List<Map<String, T>> aggResults = new ArrayList<>();
		
		if (aggregations == null || aggregations.isEmpty()) {
			return aggResults;
		}

		for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
			String aggName = entry.getKey();
			Aggregate aggregate = entry.getValue();
			
			// 处理 StringTerms 聚合
			if (aggregate.isSterms()) {
				StringTermsAggregate stringTerms = aggregate.sterms();
				Buckets buckets = stringTerms.buckets();
						
				if (buckets.isArray()) {
					List<?> bucketList = buckets.array();
					for (Object obj : bucketList) {
						var bucket = (StringTermsBucket) obj;
						Map<String, T> result = new HashMap<>();
						String key = bucket.key().stringValue();
						long docCount = bucket.docCount();
						
						log.debug("Terms Bucket [{}]: Key={}, DocCount={}", aggName, key, docCount);
						// 使用与 DSL 对齐的格式: key 和 doc_count 作为独立字段
						result.put("key", (T) key);
						result.put("doc_count", (T) Long.valueOf(docCount));
						
						// 处理子聚合，直接平铺到顶层（与 DSL 输出格式对齐）
						if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
							Map<String, Object> subAggResults = parseSubAggregations(bucket.aggregations());
							for (Map.Entry<String, Object> subEntry : subAggResults.entrySet()) {
								result.put(subEntry.getKey(), (T) subEntry.getValue());
							}
						}
						
						aggResults.add(result);
					}
				}
			}
			// 处理 LongTerms 聚合
			else if (aggregate.isLterms()) {
				LongTermsAggregate longTerms = aggregate.lterms();
				Buckets buckets = longTerms.buckets();
						
				if (buckets.isArray()) {
					List<?> bucketList = buckets.array();
					for (Object obj : bucketList) {
						var bucket = (co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket) obj;
						Map<String, T> result = new HashMap<>();
						// LongTermsBucket.key() 返回 long 类型
						String key = String.valueOf(bucket.key());
						long docCount = bucket.docCount();
						
						log.debug("Terms Bucket [{}]: Key={}, DocCount={}", aggName, key, docCount);
						// 使用与 DSL 对齐的格式: key 和 doc_count 作为独立字段
						result.put("key", (T) key);
						result.put("doc_count", (T) Long.valueOf(docCount));
						
						// 处理子聚合，直接平铺到顶层（与 DSL 输出格式对齐）
						if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
							Map<String, Object> subAggResults = parseSubAggregations(bucket.aggregations());
							for (Map.Entry<String, Object> subEntry : subAggResults.entrySet()) {
								result.put(subEntry.getKey(), (T) subEntry.getValue());
							}
						}
						
						aggResults.add(result);
					}
				}
			}
		}

		return aggResults;
	}

	/**
	 * 解析单个 Terms 聚合结果
	 *
	 * @param aggregate ES 8.x 聚合对象
	 * @return 聚合结果列表
	 */
	public static <T> List<Map<String, T>> termsResult(Aggregate aggregate) {
		List<Map<String, T>> aggResults = new ArrayList<>();
		
		if (aggregate == null) {
			return aggResults;
		}

		// 处理 StringTerms 聚合
		if (aggregate.isSterms()) {
			StringTermsAggregate stringTerms = aggregate.sterms();
			Buckets buckets = stringTerms.buckets();
						
			if (buckets.isArray()) {
				List<?> bucketList = buckets.array();
				for (Object obj : bucketList) {
					// ES 8.x StringTermsBucket 的 key() 返回 FieldValue，需要转换为字符串
					var bucket = (StringTermsBucket) obj;
					Map<String, T> result = new HashMap<>();
					String key = bucket.key().stringValue();
					long docCount = bucket.docCount();
					
					log.debug("Terms Bucket: Key={}, DocCount={}", key, docCount);
					// 使用与 DSL 对齐的格式: key 和 doc_count 作为独立字段
					result.put("key", (T) key);
					result.put("doc_count", (T) Long.valueOf(docCount));
					
					// 处理子聚合，直接平铺到顶层（与 DSL 输出格式对齐）
					if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
						Map<String, Object> subAggResults = parseSubAggregations(bucket.aggregations());
						for (Map.Entry<String, Object> subEntry : subAggResults.entrySet()) {
							result.put(subEntry.getKey(), (T) subEntry.getValue());
						}
					}
					
					aggResults.add(result);
				}
			}
		}
		// 处理 LongTerms 聚合
		else if (aggregate.isLterms()) {
			LongTermsAggregate longTerms = aggregate.lterms();
			Buckets buckets = longTerms.buckets();
						
			if (buckets.isArray()) {
				List<?> bucketList = longTerms.buckets().array();
				for (Object obj : bucketList) {
					var bucket = (co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket) obj;
					Map<String, T> result = new HashMap<>();
					// LongTermsBucket.key() 返回 long 类型
					String key = String.valueOf(bucket.key());
					long docCount = bucket.docCount();
					
					log.debug("Terms Bucket: Key={}, DocCount={}", key, docCount);
					// 使用与 DSL 对齐的格式: key 和 doc_count 作为独立字段
					result.put("key", (T) key);
					result.put("doc_count", (T) Long.valueOf(docCount));
					
					// 处理子聚合，直接平铺到顶层（与 DSL 输出格式对齐）
					if (bucket.aggregations() != null && !bucket.aggregations().isEmpty()) {
						Map<String, Object> subAggResults = parseSubAggregations(bucket.aggregations());
						for (Map.Entry<String, Object> subEntry : subAggResults.entrySet()) {
							result.put(subEntry.getKey(), (T) subEntry.getValue());
						}
					}
					
					aggResults.add(result);
				}
			}
		}

		return aggResults;
	}

	/**
	 * 解析 Range 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Map<String, Object> 聚合结果，key 为范围标识，value 为文档数量
	 */
	@SuppressWarnings("unchecked")
	public static <T> Map<String, T> rangeResult(Map<String, Aggregate> aggregations, String aggName) {
		Map<String, T> result = new HashMap<>();
		
		if (aggregations == null || aggregations.isEmpty()) {
			return result;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return result;
		}
		
		// 处理 Range 聚合
		if (aggregate.isRange()) {
			RangeAggregate rangeAggregate = aggregate.range();
			List<?> buckets = rangeAggregate.buckets().array();
			
			for (Object obj : buckets) {
				var bucket = (co.elastic.clients.elasticsearch._types.aggregations.RangeBucket) obj;
				
				// 获取范围的 key（如果有）
				String key = bucket.key();
				if (key == null || key.isEmpty()) {
					// 如果没有 key，使用 from-to 作为标识
					Double from = bucket.from();
					Double to = bucket.to();
					if (from != null && to != null) {
						key = from + "-" + to;
					} else if (from != null) {
						key = from + "-*";
					} else if (to != null) {
						key = "*-" + to;
					} else {
						key = "*-*";
					}
				}
				
				long docCount = bucket.docCount();
				log.debug("Range Bucket [{}]: Key={}, From={}, To={}, DocCount={}", 
						aggName, key, bucket.from(), bucket.to(), docCount);
				result.put(key, (T) Long.valueOf(docCount));
			}
		} else {
			log.warn("Aggregation [{}] is not a Range aggregation, type: {}", aggName, aggregate._kind());
		}
		
		return result;
	}

	/**
	 * 解析 Histogram 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Map<String, Object> 聚合结果，key 为桶的 key（数值），value 为文档数量
	 */
	@SuppressWarnings("unchecked")
	public static <T> Map<String, T> histogramResult(Map<String, Aggregate> aggregations, String aggName) {
		Map<String, T> result = new HashMap<>();
		
		if (aggregations == null || aggregations.isEmpty()) {
			return result;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return result;
		}
		
		// 处理 Histogram 聚合
		if (aggregate.isHistogram()) {
			HistogramAggregate histogramAggregate = aggregate.histogram();
			List<?> buckets = histogramAggregate.buckets().array();
			
			for (Object obj : buckets) {
				var bucket = (co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket) obj;
				
				// Histogram bucket 的 key 是 double 类型
				Double key = bucket.key();
				long docCount = bucket.docCount();
				
				log.debug("Histogram Bucket [{}]: Key={}, DocCount={}", 
						aggName, key, docCount);
				result.put(String.valueOf(key), (T) Long.valueOf(docCount));
			}
		} else {
			log.warn("Aggregation [{}] is not a Histogram aggregation, type: {}", aggName, aggregate._kind());
		}
		
		return result;
	}

	/**
	 * 解析 Date Histogram 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Map<String, Object> 聚合结果，key 为桶的 key（日期字符串或时间戳），value 为文档数量
	 */
	@SuppressWarnings("unchecked")
	public static <T> Map<String, T> dateHistogramResult(Map<String, Aggregate> aggregations, String aggName) {
		Map<String, T> result = new HashMap<>();
		
		if (aggregations == null || aggregations.isEmpty()) {
			return result;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return result;
		}
		
		// 处理 Date Histogram 聚合
		if (aggregate.isDateHistogram()) {
			DateHistogramAggregate dateHistogramAggregate = aggregate.dateHistogram();
			List<?> buckets = dateHistogramAggregate.buckets().array();
			
			for (Object obj : buckets) {
				var bucket = (co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket) obj;
				
				// Date Histogram bucket 的 key 是 long 类型的时间戳（毫秒）
				Long timestamp = bucket.key();
				long docCount = bucket.docCount();
				
				log.debug("Date Histogram Bucket [{}]: Timestamp={}, DocCount={}", 
						aggName, timestamp, docCount);
				result.put(String.valueOf(timestamp), (T) Long.valueOf(docCount));
			}
		} else {
			log.warn("Aggregation [{}] is not a Date Histogram aggregation, type: {}", aggName, aggregate._kind());
		}
		
		return result;
	}

	/**
	 * 解析 Min 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Double 最小值，如果聚合不存在或没有数据则返回 null
	 */
	public static Double minResult(Map<String, Aggregate> aggregations, String aggName) {
		if (aggregations == null || aggregations.isEmpty()) {
			return null;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return null;
		}
		
		// 处理 Min 聚合
		if (aggregate.isMin()) {
			MinAggregate minAggregate = aggregate.min();
			Double value = minAggregate.value();
			
			log.debug("Min Aggregation [{}]: Value={}", aggName, value);
			return value;
		} else {
			log.warn("Aggregation [{}] is not a Min aggregation, type: {}", aggName, aggregate._kind());
			return null;
		}
	}

	/**
	 * 解析 Max 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Double 最大值，如果聚合不存在或没有数据则返回 null
	 */
	public static Double maxResult(Map<String, Aggregate> aggregations, String aggName) {
		if (aggregations == null || aggregations.isEmpty()) {
			return null;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return null;
		}
		
		// 处理 Max 聚合
		if (aggregate.isMax()) {
			MaxAggregate maxAggregate = aggregate.max();
			Double value = maxAggregate.value();
			
			log.debug("Max Aggregation [{}]: Value={}", aggName, value);
			return value;
		} else {
			log.warn("Aggregation [{}] is not a Max aggregation, type: {}", aggName, aggregate._kind());
			return null;
		}
	}

	/**
	 * 解析 Avg 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Double 平均值，如果聚合不存在或没有数据则返回 null
	 */
	public static Double avgResult(Map<String, Aggregate> aggregations, String aggName) {
		if (aggregations == null || aggregations.isEmpty()) {
			return null;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return null;
		}
		
		// 处理 Avg 聚合
		if (aggregate.isAvg()) {
			AvgAggregate avgAggregate = aggregate.avg();
			Double value = avgAggregate.value();
			
			log.debug("Avg Aggregation [{}]: Value={}", aggName, value);
			return value;
		} else {
			log.warn("Aggregation [{}] is not an Avg aggregation, type: {}", aggName, aggregate._kind());
			return null;
		}
	}

	/**
	 * 解析 Sum 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Double 总和值，如果聚合不存在或没有数据则返回 null
	 */
	public static Double sumResult(Map<String, Aggregate> aggregations, String aggName) {
		if (aggregations == null || aggregations.isEmpty()) {
			return null;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return null;
		}
		
		// 处理 Sum 聚合
		if (aggregate.isSum()) {
			SumAggregate sumAggregate = aggregate.sum();
			Double value = sumAggregate.value();
			
			log.debug("Sum Aggregation [{}]: Value={}", aggName, value);
			return value;
		} else {
			log.warn("Aggregation [{}] is not a Sum aggregation, type: {}", aggName, aggregate._kind());
			return null;
		}
	}
	/**
	 * 解析 Stats 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return StatsAggResult 包含 count, min, max, avg, sum 的结果对象
	 */
	public static com.awesomecopilot.search8x.support.StatsAggResult statsResult(Map<String, Aggregate> aggregations, String aggName) {
		if (aggregations == null || aggregations.isEmpty()) {
			return null;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return null;
		}
		
		// 处理 Stats 聚合
		if (aggregate.isStats()) {
			StatsAggregate statsAggregate = aggregate.stats();
			
			long count = statsAggregate.count();
			Double min = statsAggregate.min();
			Double max = statsAggregate.max();
			Double avg = statsAggregate.avg();
			Double sum = statsAggregate.sum();
			
			log.debug("Stats Aggregation [{}]: Count={}, Min={}, Max={}, Avg={}, Sum={}", 
					aggName, count, min, max, avg, sum);
			
			return com.awesomecopilot.search8x.support.StatsAggResult.builder()
					.name(aggName)
					.count(count)
					.min(min != null ? min : 0.0)
					.max(max != null ? max : 0.0)
					.avg(avg != null ? avg : 0.0)
					.sum(sum != null ? sum : 0.0)
					.build();
		} else {
			log.warn("Aggregation [{}] is not a Stats aggregation, type: {}", aggName, aggregate._kind());
			return null;
		}
	}

	/**
	 * 解析 Extended Stats 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return ExtendedStatsAggResult 包含扩展统计信息
	 */
	public static ExtendedStatsAggResult extendedStatsResult(Map<String, Aggregate> aggregations, String aggName) {
		if (aggregations == null || aggregations.isEmpty()) {
			return null;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return null;
		}
		
		// 处理 Extended Stats 聚合
		if (aggregate.isExtendedStats()) {
			ExtendedStatsAggregate extStats = aggregate.extendedStats();
			
			long count = extStats.count();
			Double min = extStats.min();
			Double max = extStats.max();
			Double avg = extStats.avg();
			Double sum = extStats.sum();
			Double sumOfSquares = extStats.sumOfSquares();
			Double variance = extStats.variance();
			Double variancePopulation = extStats.variancePopulation();
			Double varianceSampling = extStats.varianceSampling();
			Double stdDeviation = extStats.stdDeviation();
			Double stdDeviationPopulation = extStats.stdDeviationPopulation();
			Double stdDeviationSampling = extStats.stdDeviationSampling();
			
			Double upper = null;
			Double lower = null;
			Double upperPopulation = null;
			Double lowerPopulation = null;
			Double upperSampling = null;
			Double lowerSampling = null;
			if (extStats.stdDeviationBounds() != null) {
				upper = extStats.stdDeviationBounds().upper();
				lower = extStats.stdDeviationBounds().lower();
				upperPopulation = extStats.stdDeviationBounds().upperPopulation();
				lowerPopulation = extStats.stdDeviationBounds().lowerPopulation();
				upperSampling = extStats.stdDeviationBounds().upperSampling();
				lowerSampling = extStats.stdDeviationBounds().lowerSampling();
			}
			
			log.debug("Extended Stats Aggregation [{}]: Count={}, Min={}, Max={}, Avg={}, Sum={}, SumOfSquares={}, Variance={}, StdDeviation={}",
					aggName, count, min, max, avg, sum, sumOfSquares, variance, stdDeviation);
			
			return ExtendedStatsAggResult.builder()
					.name(aggName)
					.count(count)
					.min(min != null ? min : 0.0)
					.max(max != null ? max : 0.0)
					.avg(avg != null ? avg : 0.0)
					.sum(sum != null ? sum : 0.0)
					.sumOfSquares(sumOfSquares != null ? sumOfSquares : 0.0)
					.variance(variance != null ? variance : 0.0)
					.variancePopulation(variancePopulation)
					.varianceSampling(varianceSampling)
					.stdDeviation(stdDeviation != null ? stdDeviation : 0.0)
					.stdDeviationPopulation(stdDeviationPopulation)
					.stdDeviationSampling(stdDeviationSampling)
					.stdDeviationBoundsUpper(upper)
					.stdDeviationBoundsLower(lower)
					.stdDeviationBoundsUpperPopulation(upperPopulation)
					.stdDeviationBoundsLowerPopulation(lowerPopulation)
					.stdDeviationBoundsUpperSampling(upperSampling)
					.stdDeviationBoundsLowerSampling(lowerSampling)
					.build();
		} else {
			log.warn("Aggregation [{}] is not an Extended Stats aggregation, type: {}", aggName, aggregate._kind());
			return null;
		}
	}

	/**
	 * 解析 Cardinality 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Long 去重后的唯一值数量
	 */
	public static Long cardinalityResult(Map<String, Aggregate> aggregations, String aggName) {
		if (aggregations == null || aggregations.isEmpty()) {
			return null;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return null;
		}
		
		// 处理 Cardinality 聚合
		if (aggregate.isCardinality()) {
			CardinalityAggregate cardinalityAggregate = aggregate.cardinality();
			Long value = cardinalityAggregate.value();
			
			log.debug("Cardinality Aggregation [{}]: Value={}", aggName, value);
			return value;
		} else {
			log.warn("Aggregation [{}] is not a Cardinality aggregation, type: {}", aggName, aggregate._kind());
			return null;
		}
	}

	/**
	 * 解析 Value Count 聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return ValueCountAggResult 包含聚合名称和非空值数量（不做去重）
	 */
	public static ValueCountAggResult valueCountResult(Map<String, Aggregate> aggregations, String aggName) {
		if (aggregations == null || aggregations.isEmpty()) {
			return null;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Aggregation [{}] not found in response", aggName);
			return null;
		}
		
		// 处理 Value Count 聚合
		if (aggregate.isValueCount()) {
			ValueCountAggregate valueCountAggregate = aggregate.valueCount();
			long value = (long) valueCountAggregate.value();
			
			log.debug("Value Count Aggregation [{}]: Value={}", aggName, value);
			return ValueCountAggResult.builder()
					.name(aggName)
					.value(value)
					.build();
		} else {
			log.warn("Aggregation [{}] is not a Value Count aggregation, type: {}", aggName, aggregate._kind());
			return null;
		}
	}

	/**
	 * 解析 Composite 聚合结果（多个子聚合的组合）
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @return Map<String, Object> 包含所有子聚合结果的 Map
	 */
	@SuppressWarnings("unchecked")
	public static <T> Map<String, T> compositeResult(Map<String, Aggregate> aggregations) {
		Map<String, T> result = new HashMap<>();
		
		if (aggregations == null || aggregations.isEmpty()) {
			return result;
		}
		
		// 遍历所有聚合，根据类型分别解析
		for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
			String aggName = entry.getKey();
			Aggregate aggregate = entry.getValue();
			
			if (aggregate == null) {
				continue;
			}
			
			// 根据聚合类型调用相应的解析方法
			if (aggregate.isStats()) {
				result.put(aggName, (T) statsResult(aggregations, aggName));
			} else if (aggregate.isExtendedStats()) {
				result.put(aggName, (T) extendedStatsResult(aggregations, aggName));
			} else if (aggregate.isCardinality()) {
				result.put(aggName, (T) cardinalityResult(aggregations, aggName));
			} else if (aggregate.isValueCount()) {
				ValueCountAggResult valueCountAggResult = valueCountResult(aggregations, aggName);
				result.put(aggName, (T) Long.valueOf(valueCountAggResult.getValue()));
			} else if (aggregate.isSum()) {
				result.put(aggName, (T) sumResult(aggregations, aggName));
			} else if (aggregate.isAvg()) {
				result.put(aggName, (T) avgResult(aggregations, aggName));
			} else if (aggregate.isMin()) {
				result.put(aggName, (T) minResult(aggregations, aggName));
			} else if (aggregate.isMax()) {
				result.put(aggName, (T) maxResult(aggregations, aggName));
			} else if (aggregate.isSterms() || aggregate.isLterms()) {
				result.put(aggName, (T) termsResult(aggregate));
			} else {
				log.warn("Unsupported aggregation type [{}] in composite result", aggregate._kind());
			}
		}
		
		return result;
	}

	/**
	 * 解析子聚合结果（通用方法）
	 *
	 * @param subAggregations 子聚合 Map (name -> Aggregate)
	 * @return 包含所有子聚合结果的 Map
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseSubAggregations(Map<String, Aggregate> subAggregations) {
		Map<String, Object> result = new HashMap<>();
		
		if (subAggregations == null || subAggregations.isEmpty()) {
			return result;
		}
		
		// 遍历所有子聚合，根据类型分别解析
		for (Map.Entry<String, Aggregate> entry : subAggregations.entrySet()) {
			String aggName = entry.getKey();
			Aggregate aggregate = entry.getValue();
			
			if (aggregate == null) {
				continue;
			}
			
			// 根据聚合类型调用相应的解析方法
			if (aggregate.isStats()) {
				result.put(aggName, statsResult(subAggregations, aggName));
			} else if (aggregate.isExtendedStats()) {
				result.put(aggName, extendedStatsResult(subAggregations, aggName));
			} else if (aggregate.isCardinality()) {
				result.put(aggName, cardinalityResult(subAggregations, aggName));
			} else if (aggregate.isValueCount()) {
				result.put(aggName, valueCountResult(subAggregations, aggName));
			} else if (aggregate.isSum()) {
				result.put(aggName, sumResult(subAggregations, aggName));
			} else if (aggregate.isAvg()) {
				result.put(aggName, avgResult(subAggregations, aggName));
			} else if (aggregate.isMin()) {
				result.put(aggName, minResult(subAggregations, aggName));
			} else if (aggregate.isMax()) {
				result.put(aggName, maxResult(subAggregations, aggName));
			} else if (aggregate.isSterms() || aggregate.isLterms()) {
				result.put(aggName, termsResult(aggregate));
			} else if (aggregate.isRange()) {
				result.put(aggName, rangeResult(subAggregations, aggName));
			} else if (aggregate.isHistogram()) {
				result.put(aggName, histogramResult(subAggregations, aggName));
			} else if (aggregate.isDateHistogram()) {
				result.put(aggName, dateHistogramResult(subAggregations, aggName));
			} else if (aggregate.isTopHits()) {
				// TopHits 需要特殊处理，返回原始文档
				log.debug("TopHits aggregation [{}] found, needs special handling", aggName);
				result.put(aggName, "TopHits aggregation (not yet fully parsed)");
			} else {
				log.warn("Unsupported sub-aggregation type [{}] in composite result", aggregate._kind());
			}
		}
		
		return result;
	}

	/**
	 * 解析 Filter 聚合结果
	 * <p>
	 * Filter 聚合返回单个桶, 包含 doc_count 和子聚合结果
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @param aggName      聚合名称
	 * @return Map 包含 doc_count 和所有子聚合结果
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> filterResult(Map<String, Aggregate> aggregations, String aggName) {
		Map<String, Object> result = new HashMap<>();
		
		if (aggregations == null || aggregations.isEmpty()) {
			return result;
		}
		
		Aggregate aggregate = aggregations.get(aggName);
		if (aggregate == null) {
			log.warn("Filter aggregation [{}] not found in response", aggName);
			return result;
		}
		
		if (aggregate.isFilter()) {
			var filterAgg = aggregate.filter();
			result.put("doc_count", filterAgg.docCount());
			
			// 解析子聚合并平铺到顶层
			if (filterAgg.aggregations() != null && !filterAgg.aggregations().isEmpty()) {
				Map<String, Object> subAggResults = parseSubAggregations(filterAgg.aggregations());
				result.putAll(subAggResults);
			}
		} else {
			log.warn("Aggregation [{}] is not a filter aggregation, type: {}", aggName, aggregate._kind());
		}
		
		return result;
	}

	/**
	 * 获取 Terms 聚合的总桶数
	 *
	 * @param aggregations ES 8.x 聚合 Map (name -> Aggregate)
	 * @return 总桶数
	 */
	public static Integer termsTotalBuckets(Map<String, Aggregate> aggregations) {
		if (aggregations == null || aggregations.isEmpty()) {
			return 0;
		}
		
		for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
			Aggregate aggregate = entry.getValue();
			
			if (aggregate == null) {
				continue;
			}
			
			// 处理 StringTerms 聚合
			if (aggregate.isSterms()) {
				StringTermsAggregate stringTerms = aggregate.sterms();
				Buckets buckets = stringTerms.buckets();
				
				if (buckets.isArray()) {
					return buckets.array().size();
				}
			}
			// 处理 LongTerms 聚合
			else if (aggregate.isLterms()) {
				LongTermsAggregate longTerms = aggregate.lterms();
				Buckets buckets = longTerms.buckets();
				
				if (buckets.isArray()) {
					return buckets.array().size();
				}
			}
		}
		
		return 0;
	}
}
