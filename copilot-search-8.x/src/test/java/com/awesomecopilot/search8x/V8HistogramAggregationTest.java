package com.awesomecopilot.search8x;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * V8 Histogram Aggregation 测试用例
 * <p>
 * 展示如何使用 ES 8.x 原生的 Histogram 聚合 API
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8HistogramAggregationTest {

    /**
     * 基本 Histogram 聚合示例
     * <p>
     * 按价格区间统计商品数量，间隔为 100
     */
    @Test
    public void testBasicHistogramAggregation() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .histogram("products")
                .of("price_histogram", "price")
                .interval(100.0)
                .get();
        
        System.out.println("Price Histogram: " + result);
        // 输出示例: {"0.0": 10, "100.0": 25, "200.0": 15, ...}
    }

    /**
     * 带 minDocCount 的 Histogram 聚合
     * <p>
     * 只返回文档数 >= 5 的桶
     */
    @Test
    public void testHistogramWithMinDocCount() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .histogram("products")
                .of("price_histogram", "price")
                .interval(100.0)
                .minDocCount(5)
                .get();
        
        System.out.println("Price Histogram (minDocCount=5): " + result);
    }

    /**
     * 带 extendedBounds 的 Histogram 聚合
     * <p>
     * 强制返回指定范围内的所有桶，即使某些桶为空
     */
    @Test
    public void testHistogramWithExtendedBounds() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .histogram("products")
                .of("price_histogram", "price")
                .interval(100.0)
                .extendedBounds(0L, 1000L)
                .get();
        
        System.out.println("Price Histogram (extendedBounds 0-1000): " + result);
        // 会返回 0.0, 100.0, 200.0, ..., 1000.0 的所有桶
    }

    /**
     * 年龄分布统计示例
     * <p>
     * 按 10 岁为一个区间统计用户分布
     */
    @Test
    public void testAgeDistributionHistogram() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .histogram("users")
                .of("age_distribution", "age")
                .interval(10.0)
                .extendedBounds(0L, 100L)
                .get();
        
        System.out.println("Age Distribution: " + result);
        // 输出示例: {"0.0": 50, "10.0": 120, "20.0": 200, "30.0": 180, ...}
    }

    /**
     * 带查询条件的 Histogram 聚合
     * <p>
     * 注意：当前版本 setQuery 还未完全实现（需要将 7.x QueryBuilder 转换为 8.x Query）
     * 这里仅演示 API 签名
     */
    @Test
    public void testHistogramWithQuery() {
        // TODO: 待实现查询条件转换后启用
        Map<String, Long> result = ElasticUtils.AggsV8
                .histogram("products")
                .of("price_histogram", "price")
                .interval(100.0)
                // .setQuery(ElasticUtils.Query.bool("products").filter(...))
                .get();
        
        System.out.println("Price Histogram with Query: " + result);
    }

    /**
     * 对比：旧的 ElasticHistogramAggregationBuilder（仍使用 7.x API）
     */
    @Test
    public void testOldHistogramAggregation() {
        // 旧的方式（仍在使用 RestHighLevelClient）
        Map<String, Long> oldResult = ElasticUtils.Aggs
                .histogram("products")
                .of("price_histogram", "price")
                .interval(100.0)
                .get();
        
        System.out.println("Old API Result: " + oldResult);
        
        // 新的方式（使用 ElasticsearchClient 8.x）
        Map<String, Long> newResult = ElasticUtils.AggsV8
                .histogram("products")
                .of("price_histogram", "price")
                .interval(100.0)
                .get();
        
        System.out.println("New API Result: " + newResult);
    }
}
