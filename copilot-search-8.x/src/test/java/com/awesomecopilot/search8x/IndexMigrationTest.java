package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Query;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 index() 方法迁移到 ElasticsearchClient 8.x
 */
@Slf4j
public class IndexMigrationTest {

    @Test
    public void testIndexWithStringDoc() {
        // 清理测试索引
        ElasticUtils.Admin.deleteIndex("test_index_migration");
        
        // 创建索引
        boolean created = ElasticUtils.Admin.createIndex("test_index_migration").create();
        assertThat(created).isTrue();
        
        // 测试 index() 方法 - 字符串文档，指定ID
        String id = ElasticUtils.index("test_index_migration", 
            "{\"name\": \"Test User\", \"age\": 25}", "1");
        
        assertThat(id).isEqualTo("1");
        log.info("Created document with ID: {}", id);
        
        // 验证文档已创建
        String doc = ElasticUtils.get("test_index_migration", "1");
        assertThat(doc).isNotNull();
        assertThat(doc).contains("Test User");
        log.info("Retrieved document: {}", doc);
    }
    
    @Test
    public void testIndexWithObjectDoc() {
        // 清理测试索引
        ElasticUtils.Admin.deleteIndex("test_index_object");
        
        // 创建索引
        boolean created = ElasticUtils.Admin.createIndex("test_index_object").create();
        assertThat(created).isTrue();
        
        // 测试 index() 方法 - 对象文档，自动提取ID
        TestUser user = new TestUser();
        user.setId(2);
        user.setName("Object User");
        user.setAge(30);
        
        String id = ElasticUtils.index("test_index_object", user);
        
        assertThat(id).isEqualTo("2");
        log.info("Created document with ID: {}", id);
        
        // 验证文档已创建
        TestUser retrieved = ElasticUtils.get("test_index_object", "2", TestUser.class);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Object User");
        assertThat(retrieved.getAge()).isEqualTo(30);
        log.info("Retrieved user: {}", retrieved);
    }
    
    @Test
    public void testCreateMethod() {
        // 清理测试索引
        ElasticUtils.Admin.deleteIndex("test_create_migration");
        
        // 创建索引
        boolean created = ElasticUtils.Admin.createIndex("test_create_migration").create();
        assertThat(created).isTrue();
        
        // 测试 create() 方法 - 使用 _create API
        String id = ElasticUtils.create("test_create_migration", 
            "{\"name\": \"Create User\", \"age\": 28}", "1");
        
        assertThat(id).isEqualTo("1");
        log.info("Created document with ID: {}", id);
        
        // 尝试再次创建相同ID的文档，应该失败
        try {
            ElasticUtils.create("test_create_migration", 
                "{\"name\": \"Duplicate User\", \"age\": 99}", "1");
            // 如果到这里说明没有抛出异常，测试失败
            assertThat(false).isTrue();
        } catch (Exception e) {
            // 预期会抛出异常（版本冲突或文档已存在）
            log.info("Expected exception when creating duplicate document: {}", e.getMessage());
            assertThat(e).isNotNull();
        }
    }
    
    @Test
    public void testIndexWithoutId() {
        // 清理测试索引
        ElasticUtils.Admin.deleteIndex("test_index_auto_id");
        
        // 创建索引
        boolean created = ElasticUtils.Admin.createIndex("test_index_auto_id").create();
        assertThat(created).isTrue();
        
        // 测试 index() 方法 - 不指定ID，由ES自动生成
        String id = ElasticUtils.index("test_index_auto_id", 
            "{\"name\": \"Auto ID User\", \"age\": 35}");
        
        assertThat(id).isNotNull();
        assertThat(id).isNotEmpty();
        log.info("Created document with auto-generated ID: {}", id);
        
        // 验证文档已创建
        String doc = ElasticUtils.get("test_index_auto_id", id);
        assertThat(doc).isNotNull();
        assertThat(doc).contains("Auto ID User");
        log.info("Retrieved document: {}", doc);
    }
    
    /**
     * 测试用的用户类
     */
    public static class TestUser {
        private Integer id;
        private String name;
        private Integer age;
        
        public Integer getId() {
            return id;
        }
        
        public void setId(Integer id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Integer getAge() {
            return age;
        }
        
        public void setAge(Integer age) {
            this.age = age;
        }
        
        @Override
        public String toString() {
            return "TestUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
        }
    }
}
