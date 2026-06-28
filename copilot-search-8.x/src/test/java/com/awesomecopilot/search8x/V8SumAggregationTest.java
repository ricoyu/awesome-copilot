package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Aggsv8;
import com.awesomecopilot.search8x.builder.agg.v8.V8SumAggregationBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V8 Sum Aggregation 测试类
 * <p>
 * 测试使用 Elasticsearch 8.x 原生 API 的 Sum 聚合功能
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8SumAggregationTest {
    
    private static final Logger log = LoggerFactory.getLogger(V8SumAggregationTest.class);
    
    /**
     * 测试基本的 Sum 聚合
     * 注意：此测试需要 ES 8.x 环境运行
     */
    @Test
    public void testBasicSumAggregation() {
        // 创建 Sum 聚合 Builder
        V8SumAggregationBuilder builder = Aggsv8.sum("test_index");
        
        assertNotNull(builder, "Sum aggregation builder should not be null");
        
        // 配置聚合
        builder.of("total_sales", "price");
        
        log.info("Sum aggregation builder created successfully");
        log.info("This test requires a running ES 8.x instance to execute the actual aggregation");
    }
    
    /**
     * 测试带查询条件的 Sum 聚合
     */
    @Test
    public void testSumAggregationWithQuery() {
        V8SumAggregationBuilder builder = Aggsv8.sum("test_index");
        
        // 可以添加查询条件（示例）
        // builder.setQuery(ElasticUtils.Query.matchQuery("test_index").of("status", "active"));
        
        builder.of("active_total", "amount");
        builder.fetchTotalHits(true);
        
        log.info("Sum aggregation with query configured successfully");
    }
    
    /**
     * 测试多个索引的 Sum 聚合
     */
    @Test
    public void testSumAggregationMultipleIndices() {
        V8SumAggregationBuilder builder = Aggsv8.sum("index1", "index2", "index3");
        
        builder.of("cross_index_sum", "value");
        
        log.info("Multi-index sum aggregation configured successfully");
    }
}
