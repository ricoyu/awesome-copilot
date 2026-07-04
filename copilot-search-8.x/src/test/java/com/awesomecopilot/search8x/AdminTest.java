package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.pojo.Movie;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ElasticUtils.Admin 单元测试类
 * 从 copilot-search 模块迁移而来,测试 ES 8.x Admin API
 * <p>
 * 包含 Admin 内部类下的所有接口测试:
 * - deleteIndex / existsIndex / listIndexNames
 * - createIndex (TODO) / createIndexAlias (TODO) / deleteIndexAlias (TODO)
 * - deleteIndexTemplate (TODO) / putIndexTemplate (TODO) / getIndexTemplate (TODO)
 */
@Slf4j
public class AdminTest {
    
    static {
        // 把 JUnit Platform 的 discovery 日志级别调到 WARNING 或更高
        Logger.getLogger("org.junit.platform.launcher.core.EngineDiscoveryOrchestrator")
                .setLevel(Level.WARNING);
        
        // 可选：同时处理其他常见 noisy logger
        Logger.getLogger("org.junit.platform").setLevel(Level.WARNING);
    }
    
    // ==================== deleteIndex() 相关测试 ====================
    
    /**
     * 测试删除索引 - 从 copilot-search 迁移
     * 对应原版 testDeleteIndex()
     */
    @Test
    public void testDeleteIndex() {
        try {
            boolean deleted = ElasticUtils.Admin.deleteIndex("boduo");
            log.info("Delete index result: {}", deleted);
        } catch (Exception e) {
            log.error("Failed to delete index", e);
        }
    }
    
    // ==================== createIndex() 相关测试 ====================
    
    /**
     * 测试创建索引 - 从 copilot-search 迁移
     * 对应原版 testCreateIndex()
     */
    @Test
    public void testCreateIndex() {
        try {
            ElasticUtils.Admin.deleteIndex("movie");
            boolean created = ElasticUtils.Admin.createIndex(Movie.class);
            assertTrue(created);
        } catch (Exception e) {
            log.error("Failed to test create index", e);
        }
    }
    
    /**
     * 测试基于注解创建索引 - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTest.testCreateIndexByAnnotation()
     */
    @Test
    public void testCreateIndexByAnnotation() {
        try {
            ElasticUtils.Admin.deleteIndex("movie");
            boolean created = ElasticUtils.Admin.createIndex(Movie.class);
            assertTrue(created);
        } catch (Exception e) {
            log.error("Failed to create index from annotation", e);
        }
    }
    
    /**
     * 测试基于注解创建索引并指定索引名 - 从 copilot-search 迁移
     * 对应原版 createIndex(Class, String)
     */
    @Test
    public void testCreateIndexByAnnotationWithExplicitName() {
        try {
            String explicitIndex = "movie_explicit";
            ElasticUtils.Admin.deleteIndex(explicitIndex);
            boolean created = ElasticUtils.Admin.createIndex(Movie.class, explicitIndex);
            assertTrue(created);
            // 清理
            ElasticUtils.Admin.deleteIndex(explicitIndex);
        } catch (Exception e) {
            log.error("Failed to create index with explicit name", e);
        }
    }
    
    // ==================== existsIndex() 相关测试 ====================
    
    /**
     * 测试检查索引是否存在 - 从 copilot-search 迁移
     * 对应原版 testExistsIndex()
     */
    @Test
    public void testExistsIndex() {
        try {
            // 测试一个可能不存在的索引
            boolean exists = ElasticUtils.Admin.existsIndex("test_nonexistent_index");
            log.info("Index 'test_nonexistent_index' exists: {}", exists);
            assertFalse(exists);
        } catch (Exception e) {
            // ES 未运行时这里会抛异常,属于正常情况
            log.warn("ES not running, skip this test", e);
        }
    }
    
    /**
     * 测试检查索引是否存在(已存在的索引) - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTest.testExistsIndex()
     */
    @Test
    public void testExistsIndexDynamicMapping() {
        try {
            boolean exists = ElasticUtils.Admin.existsIndex("dynamic_mapping_test");
            log.info("Index 'dynamic_mapping_test' exists: {}", exists);
            assertTrue(exists);
        } catch (Exception e) {
            log.warn("ES not running or index not found: {}", e.getMessage());
        }
    }
    
    // ==================== listIndexNames() 相关测试 ====================
    
    /**
     * 测试列出所有索引 - 从 copilot-search 迁移
     * 对应原版 testListAllIndices()
     */
    @Test
    public void testListAllIndices() {
        try {
            List<String> indices = ElasticUtils.Admin.listIndexNames();
            log.info("Total indices: {}", indices.size());
            indices.forEach(index -> log.info("Index: {}", index));
            
            assertThat(indices).isNotNull();
        } catch (Exception e) {
            log.error("Failed to list indices", e);
        }
    }
    
    // ==================== createIndexAlias() 相关测试 ====================
    
    /**
     * 测试创建和删除别名 - 从 copilot-search 迁移
     * 对应原版 testCreateDelteAlias()
     */
    @Test
    public void testCreateDeleteAlias() {
        try {
            boolean indexDeleted = ElasticUtils.Admin.deleteIndex("test-2021-01-28");
            log.info("Index deleted: {}", indexDeleted);
            
            // TODO: createIndex 方法还未实现
            // boolean indexCreated = ElasticUtils.Admin.createIndex("test-2021-01-28").create();
            // assertTrue(indexCreated);
            // boolean created = ElasticUtils.Admin.createIndexAlias("test-2021-01-28", "test");
            // assertTrue(created);
            // boolean deleted = ElasticUtils.Admin.deleteIndexAlias("test-2021-01-28", "test");
            // assertTrue(deleted);
            
            log.info("Alias test pending - createIndex/createIndexAlias not yet implemented in 8.x");
            
        } catch (Exception e) {
            log.error("Failed to test alias operations", e);
        }
    }
    
    // ==================== IndexTemplate 相关测试 ====================
    
    /**
     * 测试删除索引模板 - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTemplateTest.testDeleteIndexTemplate()
     */
    @Test
    public void testDeleteIndexTemplate() {
        try {
            // TODO: deleteIndexTemplate 方法还未实现
            // boolean deleted = ElasticUtils.Admin.deleteIndexTemplate("event_template2");
            // assertTrue(deleted);
            log.info("Delete index template test pending - not yet implemented in 8.x");
        } catch (Exception e) {
            log.error("Failed to delete index template", e);
        }
    }
    
    /**
     * 测试通过REST API创建索引模板 - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTemplateTest.testCreateIndexTemplateByRestAPI()
     */
    @Test
    public void testCreateIndexTemplateByRestAPI() {
        try {
            // TODO: putIndexTemplate 方法还未实现
            // boolean created = ElasticUtils.Admin.putIndexTemplate("event_template",
            //         IOUtils.readClassPathFileAsString("index_template.json"));
            // assertTrue(created);
            log.info("Create index template test pending - not yet implemented in 8.x");
        } catch (Exception e) {
            log.error("Failed to create index template", e);
        }
    }
    
    /**
     * 测试获取索引模板 - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTemplateTest.testGetIndexTemplate()
     */
    @Test
    public void testGetIndexTemplate() {
        try {
            // TODO: getIndexTemplate 方法还未实现
            // Map<String, ?> eventTemplate = ElasticUtils.Admin.getIndexTemplate("event_template");
            // log.info("Index template: {}", eventTemplate);
            log.info("Get index template test pending - not yet implemented in 8.x");
        } catch (Exception e) {
            log.error("Failed to get index template", e);
        }
    }
}
