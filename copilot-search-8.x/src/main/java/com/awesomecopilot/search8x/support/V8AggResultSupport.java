package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.RangeAggregate;
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
public final class V8AggResultSupport {

	private static final Logger log = LoggerFactory.getLogger(V8AggResultSupport.class);

	private V8AggResultSupport() {
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
						var bucket = (co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket) obj;
						Map<String, T> result = new HashMap<>();
						String key = bucket.key().stringValue();
						long docCount = bucket.docCount();
						
						log.debug("Terms Bucket [{}]: Key={}, DocCount={}", aggName, key, docCount);
						result.put(key, (T) Long.valueOf(docCount));
						
						// TODO: 处理子聚合（如果需要）
						
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
						result.put(key, (T) Long.valueOf(docCount));
						
						// TODO: 处理子聚合（如果需要）
						
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
					var bucket = (co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket) obj;
					Map<String, T> result = new HashMap<>();
					String key = bucket.key().stringValue();
					long docCount = bucket.docCount();
					
					log.debug("Terms Bucket: Key={}, DocCount={}", key, docCount);
					result.put(key, (T) Long.valueOf(docCount));
					
					// TODO: 处理子聚合（如果需要）
					
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
					result.put(key, (T) Long.valueOf(docCount));
					
					// TODO: 处理子聚合（如果需要）
					
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
}
