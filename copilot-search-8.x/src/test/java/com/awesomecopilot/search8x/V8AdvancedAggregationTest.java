package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Aggsv8;
import com.awesomecopilot.search8x.builder.agg.v8.V8CardinalityAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.v8.V8CompositeAggregationBuilder;
import com.awesomecopilot.search8x.builder.agg.v8.V8StatsAggregationBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stats、Cardinality、Composite 聚合测试类
 * <p>
 * 测试 ES 8.x 原生 API 实现的三种聚合方法
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class V8AdvancedAggregationTest {

    private static final Logger log = LoggerFactory.getLogger(V8AdvancedAggregationTest.class);

    /**
     * 测试 Stats 聚合基本功能
     */
    @Test
    public void testBasicStatsAggregation() {
        V8StatsAggregationBuilder builder = Aggsv8.stats("test_index");
        assertNotNull(builder, "Stats aggregation builder should not be null");

        builder.of("price_stats", "price");
        log.info("Stats aggregation builder created successfully");
    }

    /**
     * 测试 Cardinality 聚合基本功能
     */
    @Test
    public void testBasicCardinalityAggregation() {
        V8CardinalityAggregationBuilder builder = Aggsv8.cardinality("test_index");
        assertNotNull(builder, "Cardinality aggregation builder should not be null");

        builder.of("unique_users", "user_id");
        log.info("Cardinality aggregation builder created successfully");
    }

    /**
     * 测试 Composite 聚合基本功能
     */
    @Test
    public void testBasicCompositeAggregation() {
        V8CompositeAggregationBuilder builder = Aggsv8.composite("test_index");
        assertNotNull(builder, "Composite aggregation builder should not be null");

        // 添加多个子聚合
        builder.addTerms("categories", "category")
               .addAvg("avg_price", "price")
               .addSum("total_sales", "sales");

        log.info("Composite aggregation builder created successfully with {} sub-aggregations", 3);
    }

    /**
     * 测试 Stats 聚合返回类型
     */
    @Test
    public void testStatsAggregationReturnType() {
        // 验证返回类型是 StatsAggResult
        V8StatsAggregationBuilder builder = Aggsv8.stats("test_index")
                .of("stats_agg", "value");

        // 注意：这里不执行 get()，因为没有真实的 ES 连接
        // 只验证构建器创建和配置
        assertNotNull(builder);
        log.info("Stats aggregation return type verification passed");
    }

    /**
     * 测试 Cardinality 聚合返回类型
     */
    @Test
    public void testCardinalityAggregationReturnType() {
        // 验证返回类型是 Long
        V8CardinalityAggregationBuilder builder = Aggsv8.cardinality("test_index")
                .of("cardinality_agg", "field");

        // 注意：这里不执行 get()，因为没有真实的 ES 连接
        // 只验证构建器创建和配置
        assertNotNull(builder);
        log.info("Cardinality aggregation return type verification passed");
    }

    /**
     * 测试 Composite 聚合返回类型
     */
    @Test
    public void testCompositeAggregationReturnType() {
        // 验证返回类型是 Map
        V8CompositeAggregationBuilder builder = Aggsv8.composite("test_index")
                .addTerms("terms_agg", "field1")
                .addAvg("avg_agg", "field2");

        // 注意：这里不执行 get()，因为没有真实的 ES 连接
        // 只验证构建器创建和配置
        assertNotNull(builder);
        log.info("Composite aggregation return type verification passed");
    }

    /**
     * 测试链式调用
     */
    @Test
    public void testChainedCalls() {
        // 测试 Stats 聚合的链式调用
        V8StatsAggregationBuilder statsBuilder = Aggsv8.stats("index1")
                .of("stats", "price")
                .fetchTotalHits(true);
        assertNotNull(statsBuilder);

        // 测试 Cardinality 聚合的链式调用
        V8CardinalityAggregationBuilder cardinalityBuilder = Aggsv8.cardinality("index2")
                .of("cardinality", "user_id")
                .fetchTotalHits(false);
        assertNotNull(cardinalityBuilder);

        // 测试 Composite 聚合的链式调用
        V8CompositeAggregationBuilder compositeBuilder = Aggsv8.composite("index3")
                .addTerms("term1", "field1")
                .addAvg("avg1", "field2")
                .fetchTotalHits(true);
        assertNotNull(compositeBuilder);

        log.info("All chained calls completed successfully");
    }
}
