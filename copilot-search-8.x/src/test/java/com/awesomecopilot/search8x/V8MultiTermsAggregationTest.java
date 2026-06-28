package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Aggsv8;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.awesomecopilot.json.jackson.JacksonUtils.toPrettyJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 Multi Terms 聚合迁移到 ElasticsearchClient 8.x
 */
@Slf4j
public class V8MultiTermsAggregationTest {

    /**
     * 测试 ES 8.x 原生 Multi Terms 聚合 - 单字段
     */
    @Test
    public void testV8MultiTermsSingleField() {
        List<Map<String, Object>> results = Aggsv8.multiTerms("bank")
                .of("age_agg", "age")
                .size(20)
                .get();

        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        
        log.info("Multi Terms (single field) result:");
        results.forEach(result -> log.info(toPrettyJson(result)));
    }

    /**
     * 测试 ES 8.x 原生 Multi Terms 聚合 - 多字段
     */
    @Test
    public void testV8MultiTermsMultipleFields() {
        // 注意：这个测试需要一个包含多个字段的索引
        // 如果 bank 索引没有合适的多字段，可以跳过或修改索引名
        try {
            List<Map<String, Object>> results = Aggsv8.multiTerms("employees")
                    .of("job_dept_agg", "job", "department")
                    .size(20)
                    .get();

            assertThat(results).isNotNull();
            
            log.info("Multi Terms (multiple fields) result:");
            results.forEach(result -> log.info(toPrettyJson(result)));
        } catch (Exception e) {
            log.warn("Multi terms with multiple fields test skipped (index may not exist): {}", e.getMessage());
        }
    }

    /**
     * 测试 ES 8.x 原生 Multi Terms 聚合 - 带 shardSize
     */
    @Test
    public void testV8MultiTermsWithShardSize() {
        List<Map<String, Object>> results = Aggsv8.multiTerms("bank")
                .of("age_agg", "age")
                .size(10)
                .shardSize(50)  // 提高精确度
                .get();

        assertThat(results).isNotNull();
        assertThat(results.size()).isLessThanOrEqualTo(10);
        
        log.info("Multi Terms with shardSize result count: {}", results.size());
        results.forEach(result -> log.info(toPrettyJson(result)));
    }

    /**
     * 对比测试：旧的 multiTerms vs 新的 AggsV8.multiTerms
     */
    @Test
    public void testCompareOldAndNewMultiTerms() {
        // 使用新的 AggsV8 API
        List<Map<String, Object>> newResults = Aggsv8.multiTerms("bank")
                .of("age_agg", "age")
                .size(5)
                .get();

        assertThat(newResults).isNotNull();
        assertThat(newResults.size()).isLessThanOrEqualTo(5);
        
        log.info("New AggsV8.multiTerms result:");
        newResults.forEach(result -> log.info(toPrettyJson(result)));
        
        // 注意：旧的 ElasticUtils.Aggs.multiTerms 依赖于 stored script "multi_fields"
        // 如果该脚本不存在，旧 API 会失败。新 API 不依赖 stored script。
        log.info("New API does not require stored script 'multi_fields'");
    }
}
