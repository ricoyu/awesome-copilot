package com.awesomecopilot.search8x;

import org.junit.jupiter.api.Test;

/**
 * V8 Max Aggregation 测试用例
 * <p>
 * 展示如何使用 ES 8.x 原生的 Max 聚合 API
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8MaxAggregationTest {

    /**
     * 基本 Max 聚合示例
     * <p>
     * 查询商品最高价格
     */
    @Test
    public void testBasicMaxAggregation() {
        Double maxPrice = ElasticUtils.AggsV8
                .max("products")
                .of("max_price", "price")
                .get();
        
        System.out.println("Maximum Price: " + maxPrice);
        // 输出示例: Maximum Price: 999.99
    }

    /**
     * Max 聚合适用于年龄字段
     * <p>
     * 查询用户最大年龄
     */
    @Test
    public void testMaxAgeAggregation() {
        Double maxAge = ElasticUtils.AggsV8
                .max("users")
                .of("max_age", "age")
                .get();
        
        System.out.println("Maximum Age: " + maxAge);
        // 输出示例: Maximum Age: 85.0
    }

    /**
     * Max 聚合适用于订单金额
     * <p>
     * 查询最大订单金额
     */
    @Test
    public void testMaxOrderAmountAggregation() {
        Double maxAmount = ElasticUtils.AggsV8
                .max("orders")
                .of("max_amount", "amount")
                .get();
        
        System.out.println("Maximum Order Amount: " + maxAmount);
    }

    /**
     * 带 fetchTotalHits 的 Max 聚合
     * <p>
     * 同时获取最大值和总命中数
     */
    @Test
    public void testMaxWithFetchTotalHits() {
        Double maxValue = ElasticUtils.AggsV8
                .max("products")
                .of("max_price", "price")
                .fetchTotalHits(true)
                .get();
        
        System.out.println("Maximum Price: " + maxValue);
        // 总命中数会被保存到 ThreadContext 中
        String totalHits = org.apache.logging.log4j.ThreadContext.get("total_hits");
        System.out.println("Total Hits: " + totalHits);
    }

    /**
     * 对比：旧的 ElasticMaxAggregationBuilder（仍使用 7.x API）
     */
    @Test
    public void testOldMaxAggregation() {
        // 旧的方式（仍在使用 RestHighLevelClient）
        Double oldResult = ElasticUtils.Aggs
                .max("products")
                .of("max_price", "price")
                .get();
        
        System.out.println("Old API Result: " + oldResult);
        
        // 新的方式（使用 ElasticsearchClient 8.x）
        Double newResult = ElasticUtils.AggsV8
                .max("products")
                .of("max_price", "price")
                .get();
        
        System.out.println("New API Result: " + newResult);
    }

    /**
     * 综合示例：统计分析场景
     * <p>
     * 查询某类商品的最低价格、最高价格等统计信息
     * （这里只演示 Max，实际使用时可以结合 Min、Avg 等）
     */
    @Test
    public void testProductPriceAnalysis() {
        Double maxPrice = ElasticUtils.AggsV8
                .max("products")
                .of("max_electronics_price", "price")
                .get();
        
        System.out.println("=== Product Price Analysis ===");
        System.out.println("Maximum Electronics Price: $" + maxPrice);
        
        // TODO: 可以添加更多聚合
        // Double minPrice = ElasticUtils.AggsV8.min("products").of("min_price", "price").get();
        // Double avgPrice = ElasticUtils.AggsV8.avg("products").of("avg_price", "price").get();
    }

    /**
     * 温度统计示例
     * <p>
     * 查询某地区的最高温度
     */
    @Test
    public void testMaxTemperatureAggregation() {
        Double maxTemp = ElasticUtils.AggsV8
                .max("weather")
                .of("max_temperature", "temperature")
                .get();
        
        System.out.println("Maximum Temperature: " + maxTemp + "°C");
    }
}
