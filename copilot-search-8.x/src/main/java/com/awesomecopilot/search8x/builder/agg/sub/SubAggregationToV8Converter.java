package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AverageAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MaxAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.MinAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StatsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregation;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.awesomecopilot.search8x.enums.CalendarInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ES 7.x SubAggregation 到 ES 8.x Aggregation 的转换器
 * <p>
 * 用于将旧的 SubAggregation API 转换为新的 ES 8.x co.elastic.clients API
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public final class SubAggregationToV8Converter {

	private static final Logger log = LoggerFactory.getLogger(SubAggregationToV8Converter.class);

	private SubAggregationToV8Converter() {
	}

	/**
	 * 将 SubAggregation 列表转换为 ES 8.x Aggregation Map
	 *
	 * @param subAggregations ES 7.x SubAggregation 列表
	 * @return ES 8.x Aggregation Map (name -> Aggregation)
	 */
	public static Map<String, Aggregation> convert(List<SubAggregation> subAggregations) {
		Map<String, Aggregation> result = new HashMap<>();
		
		if (subAggregations == null || subAggregations.isEmpty()) {
			return result;
		}

		for (SubAggregation subAgg : subAggregations) {
			Aggregation v8Agg = convertSingle(subAgg);
			if (v8Agg != null) {
				result.put(subAgg.getName(), v8Agg);
			}
		}

		return result;
	}

	/**
	 * 转换单个 SubAggregation 为 ES 8.x Aggregation
	 *
	 * @param subAggregation ES 7.x SubAggregation
	 * @return ES 8.x Aggregation
	 */
	private static Aggregation convertSingle(SubAggregation subAggregation) {
		if (subAggregation == null) {
			return null;
		}

		// 根据具体类型进行转换
		if (subAggregation instanceof ElasticAvgSubAggregation) {
			return convertAvg((ElasticAvgSubAggregation) subAggregation);
		} else if (subAggregation instanceof ElasticSumSubAggregation) {
			return convertSum((ElasticSumSubAggregation) subAggregation);
		} else if (subAggregation instanceof ElasticMinSubAggregation) {
			return convertMin((ElasticMinSubAggregation) subAggregation);
		} else if (subAggregation instanceof ElasticMaxSubAggregation) {
			return convertMax((ElasticMaxSubAggregation) subAggregation);
		} else if (subAggregation instanceof ElasticStatsSubAggregation) {
			return convertStats((ElasticStatsSubAggregation) subAggregation);
		} else if (subAggregation instanceof ElsticHistogramSubAggregation) {
			return convertHistogram((ElsticHistogramSubAggregation) subAggregation);
		} else if (subAggregation instanceof ElasticDateHistogramSubAggregation) {
			return convertDateHistogram((ElasticDateHistogramSubAggregation) subAggregation);
		} else if (subAggregation instanceof ElasticTermsSubAggregation) {
			return convertTerms((ElasticTermsSubAggregation) subAggregation);
		} else if (subAggregation instanceof ElasticBucketSortSubAggregation) {
			// BucketSort 是 Pipeline Aggregation，在 ES 8.x 中需要特殊处理
			// 暂时跳过，后续实现
			log.warn("BucketSort sub-aggregation is not yet supported in V8 converter");
			return null;
		} else if (subAggregation instanceof ElasticTopHitsSubAggregation) {
			return convertTopHits((ElasticTopHitsSubAggregation) subAggregation);
		} else {
			log.warn("Unsupported sub-aggregation type: {}", subAggregation.getClass().getName());
			return null;
		}
	}

	/**
	 * 转换 Avg 子聚合
	 */
	private static Aggregation convertAvg(ElasticAvgSubAggregation avg) {
		AverageAggregation avgAgg = AverageAggregation.of(a -> a.field(avg.field));
		return new Aggregation.Builder().avg(avgAgg).build();
	}

	/**
	 * 转换 Sum 子聚合
	 */
	private static Aggregation convertSum(ElasticSumSubAggregation sum) {
		SumAggregation sumAgg = SumAggregation.of(s -> s.field(sum.field));
		return new Aggregation.Builder().sum(sumAgg).build();
	}

	/**
	 * 转换 Min 子聚合
	 */
	private static Aggregation convertMin(ElasticMinSubAggregation min) {
		MinAggregation minAgg = MinAggregation.of(m -> m.field(min.field));
		return new Aggregation.Builder().min(minAgg).build();
	}

	/**
	 * 转换 Max 子聚合
	 */
	private static Aggregation convertMax(ElasticMaxSubAggregation max) {
		MaxAggregation maxAgg = MaxAggregation.of(m -> m.field(max.field));
		return new Aggregation.Builder().max(maxAgg).build();
	}

	/**
	 * 转换 Stats 子聚合
	 */
	private static Aggregation convertStats(ElasticStatsSubAggregation stats) {
		StatsAggregation statsAgg = StatsAggregation.of(s -> s.field(stats.field));
		return new Aggregation.Builder().stats(statsAgg).build();
	}

	/**
	 * 转换 Histogram 子聚合
	 */
	private static Aggregation convertHistogram(ElsticHistogramSubAggregation histogram) {
		HistogramAggregation.Builder builder = new HistogramAggregation.Builder();
		builder.field(histogram.field);
		builder.interval(histogram.interval);

		if (histogram.minDocCount != null) {
			builder.minDocCount(histogram.minDocCount);
		}

		if (histogram.minBound != null && histogram.maxBound != null) {
			builder.extendedBounds(bounds -> bounds
				.min(histogram.minBound.doubleValue())
				.max(histogram.maxBound.doubleValue())
			);
		}

		// 递归处理子聚合
		if (!histogram.subAggregations.isEmpty()) {
			Map<String, Aggregation> subAggs = convert(histogram.subAggregations);
			if (!subAggs.isEmpty()) {
				return new Aggregation.Builder()
					.histogram(builder.build())
					.aggregations(subAggs)
					.build();
			}
		}

		return new Aggregation.Builder().histogram(builder.build()).build();
	}

	/**
	 * 转换 DateHistogram 子聚合
	 */
	private static Aggregation convertDateHistogram(ElasticDateHistogramSubAggregation dateHistogram) {
		DateHistogramAggregation.Builder builder = new DateHistogramAggregation.Builder();
		builder.field(dateHistogram.field);

		// 设置时间间隔（优先使用 calendarInterval）
		if (dateHistogram.calendarInterval != null) {
			// 将 DateHistogramInterval 转换为字符串表达式
			String intervalStr = dateHistogram.calendarInterval.toString();
			// ES 8.x 使用字符串直接设置 calendar_interval
			switch (intervalStr.toUpperCase()) {
				case "MINUTE":
					builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Minute);
					break;
				case "HOUR":
					builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Hour);
					break;
				case "DAY":
					builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Day);
					break;
				case "WEEK":
					builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Week);
					break;
				case "MONTH":
					builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Month);
					break;
				case "QUARTER":
					builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Quarter);
					break;
				case "YEAR":
					builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Year);
					break;
			}
		} else if (dateHistogram.fixedInterval != null) {
			// 将 DateHistogramInterval 转换为字符串表达式
			String intervalStr = dateHistogram.fixedInterval.toString();
			// ES 8.x 使用 Time 对象设置 fixed_interval
			builder.fixedInterval(co.elastic.clients.elasticsearch._types.Time.of(t -> t.time(intervalStr)));
		}

		if (dateHistogram.minDocCount != null) {
			builder.minDocCount(dateHistogram.minDocCount);
		}

		if (dateHistogram.minBound != null && dateHistogram.maxBound != null) {
			// ES 8.x ExtendedBounds 使用 FieldDateMath 类型
			builder.extendedBounds(bounds -> bounds
				.min(co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath.of(m -> m.value(dateHistogram.minBound.doubleValue())))
				.max(co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath.of(m -> m.value(dateHistogram.maxBound.doubleValue())))
			);
		}

		// 递归处理子聚合
		if (!dateHistogram.subAggregations.isEmpty()) {
			Map<String, Aggregation> subAggs = convert(dateHistogram.subAggregations);
			if (!subAggs.isEmpty()) {
				return new Aggregation.Builder()
					.dateHistogram(builder.build())
					.aggregations(subAggs)
					.build();
			}
		}

		return new Aggregation.Builder().dateHistogram(builder.build()).build();
	}

	/**
	 * 转换 Terms 子聚合
	 */
	private static Aggregation convertTerms(ElasticTermsSubAggregation terms) {
		TermsAggregation.Builder builder = new TermsAggregation.Builder();
		builder.field(terms.field);

		if (terms.size != null) {
			builder.size(terms.size);
		}

		// 递归处理子聚合
		if (!terms.subAggregations.isEmpty()) {
			Map<String, Aggregation> subAggs = convert(terms.subAggregations);
			if (!subAggs.isEmpty()) {
				return new Aggregation.Builder()
					.terms(builder.build())
					.aggregations(subAggs)
					.build();
			}
		}

		return new Aggregation.Builder().terms(builder.build()).build();
	}

	/**
	 * 转换 TopHits 子聚合
	 */
	private static Aggregation convertTopHits(ElasticTopHitsSubAggregation topHits) {
		TopHitsAggregation.Builder builder = new TopHitsAggregation.Builder();

		if (topHits.from != null) {
			builder.from(topHits.from);
		}
		if (topHits.size != null) {
			builder.size(topHits.size);
		}

		// 设置 _source 过滤
		if (!topHits.fetchSource) {
			builder.source(s -> s.fetch(false));
		} else if (topHits.includeSource != null || topHits.excludeSource != null) {
			builder.source(s -> {
				if (topHits.includeSource != null && topHits.includeSource.length > 0) {
					s.filter(f -> f.includes(java.util.Arrays.asList(topHits.includeSource)));
				}
				if (topHits.excludeSource != null && topHits.excludeSource.length > 0) {
					s.filter(f -> f.excludes(java.util.Arrays.asList(topHits.excludeSource)));
				}
				return s;
			});
		}

		// 设置排序
		if (topHits.sortOrders != null && !topHits.sortOrders.isEmpty()) {
			List<SortOptions> sortOptions = new java.util.ArrayList<>();
			for (com.awesomecopilot.search8x.enums.SortOrder sortOrder : topHits.sortOrders) {
				SortOptions sortOption = SortOptions.of(s -> s
					.field(f -> f
						.field(sortOrder.getField())
						.order(sortOrder.isAsc() ? SortOrder.Asc : SortOrder.Desc)
					)
				);
				sortOptions.add(sortOption);
			}
			if (!sortOptions.isEmpty()) {
				builder.sort(sortOptions);
			}
		}

		return new Aggregation.Builder().topHits(builder.build()).build();
	}
}
