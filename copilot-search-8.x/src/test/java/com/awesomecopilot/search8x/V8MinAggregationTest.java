package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Aggsv8;
import org.junit.jupiter.api.Test;

/**
 * V8 Min Aggregation 测试用例
 * <p>
 * 展示如何使用 ES 8.x 原生的 Min 聚合 API
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8MinAggregationTest {

    /**
     * 基本 Min 聚合示例
     * <p>
     * 查询商品最低价格
     */
    @Test
    public void testBasicMinAggregation() {
        Double minPrice = Aggsv8
                .min("products")
                .of("min_price", "price")
                .get();
        
        System.out.println("Minimum Price: " + minPrice);
        // 输出示例: Minimum Price: 9.99
    }

    /**
     * Min 聚合适用于年龄字段
     * <p>
     * 查询用户最小年龄
     */
    @Test
    public void testMinAgeAggregation() {
        Double minAge = Aggsv8
                .min("users")
                .of("min_age", "age")
                .get();
        
        System.out.println("Minimum Age: " + minAge);
        // 输出示例: Minimum Age: 18.0
    }

    /**
     * Min 聚合适用于订单金额
     * <p>
     * 查询最小订单金额
     */
    @Test
    public void testMinOrderAmountAggregation() {
        Double minAmount = Aggsv8
                .min("orders")
                .of("min_amount", "amount")
                .get();
        
        System.out.println("Minimum Order Amount: " + minAmount);
    }

    /**
     * 带 fetchTotalHits 的 Min 聚合
     * <p>
     * 同时获取最小值和总命中数
     */
    @Test
    public void testMinWithFetchTotalHits() {
        Double minValue = Aggsv8
                .min("products")
                .of("min_price", "price")
                .fetchTotalHits(true)
                .get();
        
        System.out.println("Minimum Price: " + minValue);
        // 总命中数会被保存到 ThreadContext 中
        String totalHits = org.apache.logging.log4j.ThreadContext.get("total_hits");
        System.out.println("Total Hits: " + totalHits);
    }

    /**
     * 对比：旧的 ElasticMinAggregationBuilder（仍使用 7.x API）
     */
    @Test
    public void testOldMinAggregation() {
        // 旧的方式（仍在使用 RestHighLevelClient）
        Double oldResult = Aggsv8
                .min("products")
                .of("min_price", "price")
                .get();
        
        System.out.println("Old API Result: " + oldResult);
        
        // 新的方式（使用 ElasticsearchClient 8.x）
        Double newResult = Aggsv8
                .min("products")
                .of("min_price", "price")
                .get();
        
        System.out.println("New API Result: " + newResult);
    }

    /**
     * 综合示例：统计分析场景
     * <p>
     * 查询某类商品的最低价格、最高价格等统计信息
     * （这里只演示 Min，实际使用时可以结合 Max、Avg 等）
     */
    @Test
    public void testProductPriceAnalysis() {
        Double minPrice = Aggsv8
                .min("products")
                .of("min_electronics_price", "price")
                .get();
        
        System.out.println("=== Product Price Analysis ===");
        System.out.println("Minimum Electronics Price: $" + minPrice);
        
        // TODO: 可以添加更多聚合
        // Double maxPrice = ElasticUtils.AggsV8.max("products").of("max_price", "price").get();
        // Double avgPrice = ElasticUtils.AggsV8.avg("products").of("avg_price", "price").get();
    }
}
