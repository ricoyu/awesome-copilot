package com.awesomecopilot.search8x;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * V8RangeAggregationBuilder 使用示例和测试
 * 
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8RangeAggregationTest {

    /**
     * 基本 Range 聚合示例
     * 对价格字段进行范围分组：0-100, 100-500, 500+
     */
    @Test
    public void testBasicRangeAggregation() {
        // 使用 ES 8.x 原生 API
        Map<String, Long> result = ElasticUtils.AggsV8
                .range("products")  // 索引名
                .of("price_ranges", "price")  // 聚合名称和字段
                .addRange(0.0, 100.0)         // 0-100
                .addRange(100.0, 500.0)       // 100-500
                .addUnboundedFrom(500.0)      // 500+
                .fetchTotalHits(true)
                .get();
        
        // 结果示例：
        // {
        //   "0.0-100.0": 150,
        //   "100.0-500.0": 80,
        //   "500.0-*": 20
        // }
        
        System.out.println("Range Aggregation Result: " + result);
        System.out.println("Total Hits: " + ElasticUtils.Aggs.totalHits());
    }

    /**
     * 带自定义 Key 的 Range 聚合示例
     */
    @Test
    public void testRangeAggregationWithKeys() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .range("products")
                .of("price_levels", "price")
                .addRange("budget", 0.0, 100.0)      // 经济型
                .addRange("mid-range", 100.0, 500.0) // 中端
                .addRange("premium", 500.0, null)    // 高端（无上界）
                .get();
        
        // 结果示例：
        // {
        //   "budget": 150,
        //   "mid-range": 80,
        //   "premium": 20
        // }
        
        System.out.println("Range Aggregation with Keys: " + result);
    }

    /**
     * 年龄分布 Range 聚合示例
     */
    @Test
    public void testAgeRangeAggregation() {
        Map<String, Long> result = ElasticUtils.AggsV8
                .range("users")
                .of("age_groups", "age")
                .addUnboundedTo("under_18", 18.0)           // < 18
                .addRange("18_to_25", 18.0, 25.0)          // 18-25
                .addRange("26_to_35", 26.0, 35.0)          // 26-35
                .addRange("36_to_50", 36.0, 50.0)          // 36-50
                .addUnboundedFrom("over_50", 50.0)         // > 50
                .get();
        
        System.out.println("Age Distribution: " + result);
    }

    /**
     * 带查询条件的 Range 聚合示例
     */
    @Test
    public void testRangeAggregationWithQuery() {
        // 注意：V8RangeAggregationBuilder 目前还不支持 setQuery（因为需要将 7.x QueryBuilder 转换为 8.x Query）
        // 这里仅演示 API 签名，实际使用时需要先过滤数据再聚合
        Map<String, Long> result = ElasticUtils.AggsV8
                .range("products")
                .of("price_by_category", "price")
                .addRange(0.0, 100.0)
                .addRange(100.0, 500.0)
                .addUnboundedFrom(500.0)
                .get();
        
        System.out.println("Price Range: " + result);
    }

    /**
     * 对比：旧的 ElasticRangeAggregationBuilder（仍使用 7.x API）
     */
    @Test
    public void testOldRangeAggregation() {
        // 旧的方式（仍在使用 RestHighLevelClient）
        Map<String, Long> oldResult = ElasticUtils.Aggs
                .range("products")
                .of("price_ranges", "price")
                .addRange(0.0, 100.0)
                .addRange(100.0, 500.0)
                .addUnboundedFrom(500.0)
                .get();
        
        System.out.println("Old API Result: " + oldResult);
        
        // 新的方式（使用 ElasticsearchClient 8.x）
        Map<String, Long> newResult = ElasticUtils.AggsV8
                .range("products")
                .of("price_ranges", "price")
                .addRange(0.0, 100.0)
                .addRange(100.0, 500.0)
                .addUnboundedFrom(500.0)
                .get();
        
        System.out.println("New API Result: " + newResult);
    }
}
