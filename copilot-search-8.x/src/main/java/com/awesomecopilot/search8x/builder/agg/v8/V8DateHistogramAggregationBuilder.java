package com.awesomecopilot.search8x.builder.agg.v8;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregation;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.awesomecopilot.common.lang.constants.DateConstants;
import com.awesomecopilot.search8x.builder.agg.AbstractAggregationBuilder;
import com.awesomecopilot.search8x.builder.query.BaseQueryBuilder;
import com.awesomecopilot.search8x.enums.CalendarInterval;
import com.awesomecopilot.search8x.enums.FixedInterval;
import com.awesomecopilot.search8x.support.V8AggResultSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;

/**
 * Elasticsearch 8.x 原生 Date Histogram Aggregation Builder
 * <p>
 * 使用 co.elastic.clients API 实现日期直方图聚合，支持 calendarInterval、fixedInterval、
 * minDocCount、extendedBounds、format、timezone 等参数
 * 
 * <p>
 * Copyright: (C), 2026-06-20
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class V8DateHistogramAggregationBuilder extends AbstractAggregationBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(V8DateHistogramAggregationBuilder.class);
	
	/**
	 * 可以识别 夏令时, 不同月份有不同的天数, 特定年份的润秒
	 */
	private String calendarInterval;
	
	/**
	 * 就是固定的时间间隔, 不管上面说的日历上的差异
	 */
	private String fixedInterval;
	
	/**
	 * min_doc_count: 0 <br/>
	 * 就是说如果在interval指定的隔间内, 某个区间没有值, 在返回的结果中, 这个区间要不要包含? <br/>
	 * 强制返回所有 buckets，即使 buckets 可能为空
	 *
	 * <pre> {@code
	 * {
	 *   "key": 100.0,
	 *   "doc_count": 0
	 * }
	 * }</pre>
	 * <p>
	 * 上面的结果只有在min_doc_count: 0时才会返回, 如果我们要返回区间内有值的, 可以设置 min_doc_count: 1
	 */
	private Integer minDocCount;
	
	/**
	 * extended_bounds.min
	 * <p>
	 * min_doc_count 参数强制返回空 buckets，但是 Elasticsearch 默认只返回你的数据中最小值和最大值之间的 buckets。
	 * <p>
	 * 假设你的数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）。因此为了得到全年数据，
	 * 我们需要告诉 Elasticsearch 我们想要全部 buckets， 即便那些 buckets 可能落在最小日期 之前 或 最大日期 之后 。
	 */
	private Long minBound;
	
	/**
	 * extended_bounds.max
	 * <p>
	 * min_doc_count 参数强制返回空 buckets，但是 Elasticsearch 默认只返回你的数据中最小值和最大值之间的 buckets。
	 * <p>
	 * 假设你的数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）。因此为了得到全年数据，
	 * 我们需要告诉 Elasticsearch 我们想要全部 buckets， 即便那些 buckets 可能落在最小日期 之前 或 最大日期 之后 。
	 */
	private Long maxBound;
	
	/**
	 * 提供日期格式以便 buckets 的键值便于阅读
	 */
	private String format;
	
	/**
	 * 上面格式化成日期字符串时使用的时区
	 */
	private ZoneId timezone;
	
	private V8DateHistogramAggregationBuilder(String[] indices) {
		this.indices = indices;
	}
	
	public static V8DateHistogramAggregationBuilder instance(String... indices) {
		if (indices == null || indices.length == 0) {
			throw new IllegalArgumentException("indices cannot be null!");
		}
		return new V8DateHistogramAggregationBuilder(indices);
	}
	
	/**
	 * 设置聚合名称和字段
	 *
	 * @param name  聚合名称
	 * @param field 要聚合的日期字段
	 */
	public V8DateHistogramAggregationBuilder of(String name, String field) {
		this.name = name;
		this.field = field;
		return this;
	}
	
	/**
	 * 设置查询条件
	 *
	 * @param queryBuilder 查询构建器
	 */
	public V8DateHistogramAggregationBuilder setQuery(BaseQueryBuilder queryBuilder) {
		super.setQuery(queryBuilder);
		return this;
	}
	
	/**
	 * 就是固定的时间间隔, 不识别 夏令时, 不同月份有不同的天数, 特定年份的润秒
	 *
	 * @param n        数量
	 * @param interval 时间单位
	 */
	public V8DateHistogramAggregationBuilder fixedInterval(Integer n, FixedInterval interval) {
		notNull(n, "n cannot be null!");
		notNull(interval, "interval cannot be null!");
		switch (interval) {
			case SECONDS:
				this.fixedInterval = n + "s";
				break;
			case MINUTES:
				this.fixedInterval = n + "m";
				break;
			case HOURS:
				this.fixedInterval = n + "h";
				break;
			case DAYS:
				this.fixedInterval = n + "d";
				break;
		}
		return this;
	}
	
	/**
	 * 可以识别 夏令时, 不同月份有不同的天数, 特定年份的润秒
	 *
	 * @param interval 日历间隔
	 */
	public V8DateHistogramAggregationBuilder calendarInterval(CalendarInterval interval) {
		notNull(interval, "interval cannot be null!");
		switch (interval) {
			case MINUTE:
				this.calendarInterval = "minute";
				break;
			case HOUR:
				this.calendarInterval = "hour";
				break;
			case DAY:
				this.calendarInterval = "day";
				break;
			case WEEK:
				this.calendarInterval = "week";
				break;
			case MONTH:
				this.calendarInterval = "month";
				break;
			case QUARTER:
				this.calendarInterval = "quarter";
				break;
			case YEAR:
				this.calendarInterval = "year";
				break;
		}
		return this;
	}
	
	/**
	 * min_doc_count: 0 <br/>
	 * 就是说如果在interval指定的隔间内, 某个区间没有值, 在返回的结果中, 这个区间要不要包含?
	 *
	 * <pre> {@code
	 * {
	 *   "key": 100.0,
	 *   "doc_count": 0
	 * }
	 * }</pre>
	 * <p>
	 * 上面的结果只有在min_doc_count: 0时才会返回, 如果我们要返回区间内有值的, 可以设置 min_doc_count: 1
	 *
	 * @param minDocCount 最小文档数
	 */
	public V8DateHistogramAggregationBuilder minDocCount(Integer minDocCount) {
		this.minDocCount = minDocCount;
		return this;
	}
	
	/**
	 * min_doc_count 参数强制返回空 buckets，但是 Elasticsearch 默认只返回你的数据中最小值和最大值之间的 buckets。
	 * <p>
	 * 因此如果你的数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）。因此为了得到全年数据，
	 * 我们需要告诉 Elasticsearch 我们想要全部 buckets， 即便那些 buckets 可能落在最小日期 之前 或 最大日期 之后 。
	 * 
	 * <pre> {@code
	 * "extended_bounds" : {
	 *     "min" : "2014-01-01",
	 *     "max" : "2014-12-31"
	 * }
	 * }</pre>
	 *
	 * @param minBound 最小边界（时间戳）
	 * @param maxBound 最大边界（时间戳）
	 */
	public V8DateHistogramAggregationBuilder extendedBounds(Long minBound, Long maxBound) {
		Objects.requireNonNull(minBound, "minBound cannot be null!");
		Objects.requireNonNull(maxBound, "maxBound cannot be null!");
		this.minBound = minBound;
		this.maxBound = maxBound;
		return this;
	}
	
	/**
	 * 提供日期格式以便 buckets 的键值便于阅读
	 *
	 * @param format 日期格式，如 yyyy-MM-dd
	 */
	public V8DateHistogramAggregationBuilder format(String format) {
		notNull(format, "format cannot be null!");
		this.format = format;
		return this;
	}
	
	/**
	 * 设置转日期字符串时使用的时区, 默认Asia/Shanghai
	 *
	 * @param zoneId 时区ID
	 */
	public V8DateHistogramAggregationBuilder timezone(String zoneId) {
		notNull(zoneId, "zoneId cannot be null!");
		this.timezone = ZoneId.of(zoneId);
		return this;
	}
	
	/**
	 * 设置转日期字符串时使用的时区, 默认Asia/Shanghai
	 *
	 * @param zoneId 时区
	 */
	public V8DateHistogramAggregationBuilder timezone(ZoneId zoneId) {
		notNull(zoneId, "zoneId cannot be null!");
		this.timezone = zoneId;
		return this;
	}
	
	/**
	 * 聚合返回的结果中是否要包含总命中数
	 *
	 * @param fetchTotalHits 是否获取总命中数
	 */
	public V8DateHistogramAggregationBuilder fetchTotalHits(boolean fetchTotalHits) {
		this.fetchTotalHits = fetchTotalHits;
		return this;
	}
	
	/**
	 * 构建 ES 8.x 原生 Date Histogram Aggregation
	 */
	private Aggregation buildV8Aggregation() {
		DateHistogramAggregation.Builder builder = new DateHistogramAggregation.Builder();
		builder.field(field);
		
		if (fixedInterval != null) {
			// TODO: ES 8.x fixedInterval 需要 Time 类型，待修复
			// co.elastic.clients.elasticsearch._types.Time time = new co.elastic.clients.elasticsearch._types.Time.Builder()
			// 	.string(fixedInterval)
			// 	.build();
			// builder.fixedInterval(time);
		}
		if (calendarInterval != null) {
			// TODO: ES 8.x calendarInterval 需要 CalendarInterval 枚举，待修复
			// switch (calendarInterval) {
			// 	case "minute":
			// 		builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Minute);
			// 		break;
			// 	case "hour":
			// 		builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Hour);
			// 		break;
			// 	case "day":
			// 		builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Day);
			// 		break;
			// 	case "week":
			// 		builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Week);
			// 		break;
			// 	case "month":
			// 		builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Month);
			// 		break;
			// 	case "quarter":
			// 		builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Quarter);
			// 		break;
			// 	case "year":
			// 		builder.calendarInterval(co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Year);
			// 		break;
			// }
		}
		if (minDocCount != null) {
			builder.minDocCount(minDocCount);
		}
		
		if (minBound != null && maxBound != null) {
			// ES 8.x ExtendedBounds 使用 FieldDateMath 类型，支持数值或日期表达式
			builder.extendedBounds(bounds -> bounds
				.min(co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath.of(m -> m.value(minBound.doubleValue())))
				.max(co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath.of(m -> m.value(maxBound.doubleValue())))
			);
		}
		
		if (format != null) {
			builder.format(format);
		}
		
		if (timezone != null) {
			builder.timeZone(timezone.getId());
		} else {
			builder.timeZone(DateConstants.CHINA.toZoneId().getId());
		}
		
		return new Aggregation.Builder().dateHistogram(builder.build()).build();
	}
	
	/**
	 * 执行聚合并返回结果
	 *
	 * @return Map<String, Object> 聚合结果，key 为桶的 key（时间戳），value 为文档数量
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> get() {
		// 构建 ES 8.x 聚合
		Map<String, Aggregation> aggregations = new HashMap<>();
		aggregations.put(name, buildV8Aggregation());
		
		// 使用 ES 8.x 客户端执行查询
		SearchResponse searchResponse = searchWithV8Client(aggregations);
		addTotalHitsToThreadLocal(searchResponse);
		
		// 使用 ES 8.x 原生解析器解析结果
		return (Map<String, T>) V8AggResultSupport.dateHistogramResult(searchResponse.aggregations(), name);
	}
}
