package com.awesomecopilot.search8x;

import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.enums.Dynamic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ElasticUtils.Mappings 单元测试类
 * 从 copilot-search 模块迁移而来,测试 ES 8.x Mapping API
 * <p>
 * 包含 Mappings 内部类下的所有接口测试:
 * - getMapping / putMapping
 */
public class MappingsTest {

    private static final Logger log = LoggerFactory.getLogger(MappingsTest.class);
    
    static {
        java.util.logging.Logger.getLogger("org.junit.platform.launcher.core.EngineDiscoveryOrchestrator")
                .setLevel(java.util.logging.Level.WARNING);
        java.util.logging.Logger.getLogger("org.junit.platform").setLevel(java.util.logging.Level.WARNING);
    }
    
    // ==================== getMapping() 相关测试 ====================
    
    /**
     * 测试获取 Mapping - 从 copilot-search 迁移
     * 对应原版 testGetMapping()
     */
    @Test
    public void testGetMapping() {
        try {
            // 获取 movies 索引的 mapping
            var mapping = ElasticUtils.Mappings.getMapping("movies");
            log.info("Mapping: {}", JacksonUtils.toPrettyJson(mapping));
            
            assertThat(mapping).isNotNull();
            
        } catch (Exception e) {
            log.error("Failed to get mapping", e);
        }
    }
    
    /**
     * 测试获取指定字段的 Mapping - 从 copilot-search 迁移
     * 对应原版 testGetFieldMapping()
     */
    @Test
    public void testGetFieldMapping() {
        try {
            Map<String, Map<String, Object>> result = ElasticUtils.Mappings.getMapping("boduo", "carrer", "fans", "income");
            log.info("Field mapping: {}", JacksonUtils.toJson(result));
            
        } catch (Exception e) {
            log.error("Failed to get field mapping", e);
        }
    }
    
    // ==================== putMapping() 相关测试 ====================
    
    /**
     * 测试设置Mapping - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTest.testPutMapping()
     */
    @Test
    public void testPutMapping() {
        try {
            boolean acknowledged = ElasticUtils.Mappings.putMapping("rico", Dynamic.FALSE)
                    .copy("movies")
                    .field("title", com.awesomecopilot.search8x.enums.FieldType.KEYWORD).index(true)
                    .analyzer(com.awesomecopilot.search8x.enums.Analyzer.ENGLISH)
                    .searchAnalyzer(com.awesomecopilot.search8x.enums.Analyzer.ENGLISH)
                    .thenCreate();
            log.info("Put mapping result: {}", acknowledged);
            
        } catch (Exception e) {
            log.error("Failed to put mapping", e);
        }
    }
    
    /**
     * 测试设置Mapping(带删除字段) - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTest.testPutMappingWithDeleteFieldDef()
     */
    @Test
    public void testPutMappingWithDeleteFieldDef() {
        try {
            boolean acknowledged = ElasticUtils.Mappings.putMapping("rico", Dynamic.TRUE)
                    .copy("movies")
                    .field("title", com.awesomecopilot.search8x.enums.FieldType.KEYWORD)
                    .index(true)
                    .and()
                    .delete("user", "genre")
                    .thenCreate();
            log.info("Put mapping with delete result: {}", acknowledged);
            
        } catch (Exception e) {
            log.error("Failed to put mapping with delete", e);
        }
    }
    
    /**
     * 测试添加新字段到Mapping - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTest.testPutMappingAddNewFields()
     */
    @Test
    public void testPutMappingAddNewFields() {
        try {
            ElasticUtils.Mappings.putMapping("boduo", Dynamic.TRUE)
                    .field("fans", com.awesomecopilot.search8x.enums.FieldType.TEXT)
                    .thenCreate();
            log.info("Put mapping add new fields success");
            
        } catch (Exception e) {
            log.error("Failed to add new fields to mapping", e);
        }
    }
    
    /**
     * 测试设置Mapping(含子字段) - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsIndexTest.testPutMappingWithChildField()
     */
    @Test
    public void testPutMappingWithChildField() {
        try {
            // TODO: createIndex with mapping 还未实现
            // Admin.deleteIndex("titles");
            // boolean acknowledged = Admin.createIndex("titles")
            //         .mapping()
            //         .field("title", FieldType.TEXT)
            //         .fields(FieldDef.builder("std", FieldType.TEXT).analyzer(Analyzer.STANDARD))
            //         .thenCreate();
            log.info("Put mapping with child field test pending - not yet implemented in 8.x");
            
        } catch (Exception e) {
            log.error("Failed to put mapping with child field", e);
        }
    }
    
    /**
     * 测试创建索引带Settings和Mapping - 从 copilot-search 迁移
     * 对应原版 testCreateIndexWithSettingsMapping()
     */
    @Test
    public void testCreateIndexWithSettingsMapping() {
        try {
            ElasticUtils.Admin.deleteIndex("product");
            String mapping = IOUtils.readClassPathFileAsString("product_mapping.json");
            boolean acknowledged = ElasticUtils.Mappings.putMapping("product", mapping);
            log.info("Put mapping result: {}", acknowledged);
            
        } catch (Exception e) {
            log.error("Failed to create index with settings and mapping", e);
        }
    }
}
