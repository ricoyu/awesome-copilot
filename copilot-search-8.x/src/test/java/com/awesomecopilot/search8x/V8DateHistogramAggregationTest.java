package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.enums.CalendarInterval;
import com.awesomecopilot.search8x.enums.FixedInterval;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * V8 Date Histogram Aggregation 测试用例
 * <p>
 * 展示如何使用 ES 8.x 原生的 Date Histogram 聚合 API
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8DateHistogramAggregationTest {

    /**
     * 基本 Date Histogram 聚合示例（使用 fixedInterval）
     * <p>
     * 按天统计订单数量，间隔为 1 天
     */
    @Test
    public void testBasicDateHistogramAggregation() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .dateHistogram("orders")
                .of("orders_by_day", "order_date")
                .fixedInterval(1, FixedInterval.DAYS)
                .get();
        
        System.out.println("Orders by Day: " + result);
        // 输出示例: {"1640995200000": 10, "1641081600000": 15, ...}
        // key 是时间戳（毫秒）
    }

    /**
     * 使用 calendarInterval 的 Date Histogram 聚合
     * <p>
     * 按月统计销售额，间隔为 1 个月（考虑不同月份天数差异）
     */
    @Test
    public void testDateHistogramWithCalendarInterval() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .dateHistogram("sales")
                .of("sales_by_month", "sale_date")
                .calendarInterval(CalendarInterval.MONTH)
                .get();
        
        System.out.println("Sales by Month: " + result);
    }

    /**
     * 带 minDocCount 的 Date Histogram 聚合
     * <p>
     * 只返回文档数 >= 5 的时间桶
     */
    @Test
    public void testDateHistogramWithMinDocCount() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .dateHistogram("orders")
                .of("orders_by_hour", "order_date")
                .fixedInterval(1, FixedInterval.HOURS)
                .minDocCount(5)
                .get();
        
        System.out.println("Orders by Hour (minDocCount=5): " + result);
    }

    /**
     * 带 extendedBounds 的 Date Histogram 聚合
     * <p>
     * 强制返回指定时间范围内的所有桶，即使某些桶为空
     */
    @Test
    public void testDateHistogramWithExtendedBounds() {
        long startTime = 1640995200000L; // 2022-01-01 00:00:00
        long endTime = 1643673600000L;   // 2022-02-01 00:00:00
        
        Map<String, Long> result = ElasticUtils.AggsV8
                .dateHistogram("orders")
                .of("orders_by_day", "order_date")
                .fixedInterval(1, FixedInterval.DAYS)
                .extendedBounds(startTime, endTime)
                .get();
        
        System.out.println("Orders by Day (with bounds): " + result);
        // 会返回 startTime 到 endTime 之间所有天的桶
    }

    /**
     * 带日期格式的 Date Histogram 聚合
     * <p>
     * 使用自定义日期格式使结果更易读
     */
    @Test
    public void testDateHistogramWithFormat() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .dateHistogram("orders")
                .of("orders_by_day", "order_date")
                .fixedInterval(1, FixedInterval.DAYS)
                .format("yyyy-MM-dd")
                .get();
        
        System.out.println("Orders by Day (formatted): " + result);
        // 注意：format 只影响显示，key 仍然是时间戳
    }

    /**
     * 带时区设置的 Date Histogram 聚合
     * <p>
     * 使用 UTC 时区进行统计
     */
    @Test
    public void testDateHistogramWithTimezone() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .dateHistogram("orders")
                .of("orders_by_day", "order_date")
                .fixedInterval(1, FixedInterval.DAYS)
                .timezone("UTC")
                .get();
        
        System.out.println("Orders by Day (UTC): " + result);
    }

    /**
     * 用户注册趋势分析示例
     * <p>
     * 按周统计新用户注册数量
     */
    @Test
    public void testUserRegistrationTrend() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .dateHistogram("users")
                .of("registrations_by_week", "registration_date")
                .calendarInterval(CalendarInterval.WEEK)
                .extendedBounds(1640995200000L, 1672531200000L) // 2022全年
                .get();
        
        System.out.println("User Registrations by Week: " + result);
    }

    /**
     * 对比：旧的 ElasticDateHistogramAggregationBuilder（仍使用 7.x API）
     */
    @Test
    public void testOldDateHistogramAggregation() {
        // 旧的方式（仍在使用 RestHighLevelClient）
        Map<String, Long> oldResult = ElasticUtils.Aggs
                .dateHistogram("orders")
                .of("orders_by_day", "order_date")
                .fixedInterval(1, FixedInterval.DAYS)
                .get();
        
        System.out.println("Old API Result: " + oldResult);
        
        // 新的方式（使用 ElasticsearchClient 8.x）
        Map<String, Long> newResult = ElasticUtils.AggsV8
                .dateHistogram("orders")
                .of("orders_by_day", "order_date")
                .fixedInterval(1, FixedInterval.DAYS)
                .get();
        
        System.out.println("New API Result: " + newResult);
    }
}
