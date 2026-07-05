package com.awesomecopilot.search8x.builder.agg.sub;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ExtendedBounds;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import com.awesomecopilot.common.lang.constants.DateConstants;
import com.awesomecopilot.search8x.enums.CalendarInterval;
import com.awesomecopilot.search8x.enums.FixedInterval;

import java.time.ZoneId;
import java.util.Objects;
import java.util.TimeZone;

import static com.awesomecopilot.common.lang.utils.Assert.notNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * <p>
 * Copyright: (C), 2021-07-13 14:36
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public abstract class SubDateHistogramAgg {
	
	/**
	 * 聚合名字
	 */
	protected String name;
	
	/**
	 * 要对哪个字段聚合
	 */
	protected String field;
	
	/**
	 * 可以识别 夏令时, 不同月份有不同的天数, 特定年份的润秒
	 */
	private co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval calendarInterval;
	
	/**
	 * 就是固定的时间间隔, 不管上面说的日历上的差异
	 */
	private Time fixedInterval;
	
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
	protected Integer minDocCount;
	
	/**
	 * extended_bounds.min
	 */
	protected Long minBound;
	
	/**
	 * extended_bounds.max
	 */
	protected Long maxBound;
	
	/**
	 * 提供日期格式以便 buckets 的键值便于阅读
	 */
	private String format;
	
	/**
	 * 上面格式化成日期字符串时使用的时区
	 */
	private ZoneId timezone;
	
	/**
	 * 就是固定的时间间隔, 不识别 夏令时, 不同月份有不同的天数, 特定年份的润秒
	 * @param n
	 * @param interval
	 * @return SubDateHistogramAgg
	 */
	public SubDateHistogramAgg fixedInterval(Integer n, FixedInterval interval) {
		notNull(n, "n cannot be null!");
		notNull(interval, "interval cannot be null!");
		switch (interval) {
			case SECONDS:
				this.fixedInterval = Time.of(t -> t.time(n + "s"));
				break;
			case MINUTES:
				this.fixedInterval = Time.of(t -> t.time(n + "m"));
				break;
			case HOURS:
				this.fixedInterval = Time.of(t -> t.time(n + "h"));
				break;
			case DAYS:
				this.fixedInterval = Time.of(t -> t.time(n + "d"));
				break;
		}
		return this;
	}
	
	/**
	 * 可以识别 夏令时, 不同月份有不同的天数, 特定年份的润秒
	 * @param interval
	 * @return SubDateHistogramAgg
	 */
	public SubDateHistogramAgg calendarInterval(CalendarInterval interval) {
		notNull(interval, "interval cannot be null!");
		switch (interval) {
			case MINUTE:
				this.calendarInterval = co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Minute;
				break;
			case HOUR:
				this.calendarInterval = co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Hour;
				break;
			case DAY:
				this.calendarInterval = co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Day;
				break;
			case WEEK:
				this.calendarInterval = co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Week;
				break;
			case MONTH:
				this.calendarInterval = co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Month;
				break;
			case QUARTER:
				this.calendarInterval = co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Quarter;
				break;
			case YEAR:
				this.calendarInterval = co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Year;
				break;
		}
		return this;
	}
	
	
	/**
	 * min_doc_count: 0 <br/>
	 * 就是说如果在interval指定的隔间内, 某个区间没有值, 在返回的结果中, 这个区间要不要包含?
	 *
	 * @param minDocCount
	 * @return
	 */
	public SubDateHistogramAgg minDocCount(Integer minDocCount) {
		this.minDocCount = minDocCount;
		return this;
	}
	
	/**
	 * min_doc_count 参数强制返回空 buckets，但是 Elasticsearch 默认只返回你的数据中最小值和最大值之间的 buckets。
	 * <p>
	 * 因此如果你的数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）。因此为了得到全年数据，
	 * 我们需要告诉 Elasticsearch 我们想要全部 buckets， 即便那些 buckets 可能落在最小日期 之前 或 最大日期 之后 。
	 *
	 * @param minBound
	 * @param maxBound
	 * @return SubHistogramAgg
	 */
	public SubDateHistogramAgg extendedBounds(Long minBound, Long maxBound) {
		Objects.requireNonNull(minBound, "minBound cannot be null!");
		Objects.requireNonNull(maxBound, "maxBound cannot be null!");
		this.minBound = minBound;
		this.maxBound = maxBound;
		return this;
	}
	/**
	 * 提供日期格式以便 buckets 的键值便于阅读
	 * @param format
	 * @return SubDateHistogramAgg
	 */
	public SubDateHistogramAgg format(String format) {
		notNull(format, "format cannot be null!");
		this.format = format;
		return this;
	}
	
	/**
	 * 设置转日期字符串时使用的时区, 默认Asia/Shanghai
	 * @param zoneId
	 * @return SubDateHistogramAgg
	 */
	public SubDateHistogramAgg timezone(String zoneId) {
		notNull(zoneId, "zoneId cannot be null!");
		this.timezone = ZoneId.of(zoneId);
		return this;
	}
	
	/**
	 * 设置转日期字符串时使用的时区, 默认Asia/Shanghai
	 * @param zoneId
	 * @return SubDateHistogramAgg
	 */
	public SubDateHistogramAgg timezone(ZoneId zoneId) {
		notNull(zoneId, "zoneId cannot be null!");
		this.timezone = zoneId;
		return this;
	}
	
	/**
	 * 设置转日期字符串时使用的时区, 默认Asia/Shanghai
	 * @param timeZone
	 * @return SubDateHistogramAgg
	 */
	public SubDateHistogramAgg timezone(TimeZone timeZone) {
		notNull(timeZone, "timeZone cannot be null!");
		this.timezone = timeZone.toZoneId();
		return this;
	}
	
	protected Aggregation build() {
		return Aggregation.of(a -> a.dateHistogram(dh -> {
			dh.field(field);
			if (fixedInterval != null) {
				dh.fixedInterval(fixedInterval);
			}
			if (calendarInterval != null) {
				dh.calendarInterval(calendarInterval);
			}
			if (minDocCount != null) {
				dh.minDocCount(minDocCount);
			}
			if (minDocCount != null && maxBound != null) {
				dh.extendedBounds(ExtendedBounds.of(eb -> eb
						.min(FieldDateMath.of(f -> f.value(minBound.doubleValue())))
						.max(FieldDateMath.of(f -> f.value(maxBound.doubleValue())))));
			}
			if (isNotBlank(format)) {
				dh.format(format);
			}
			if (timezone != null) {
				dh.timeZone(timezone.getId());
			} else {
				dh.timeZone(DateConstants.CHINA.toZoneId().getId());
			}
			return dh;
		}));
	}
	
	public abstract Object and();
}
