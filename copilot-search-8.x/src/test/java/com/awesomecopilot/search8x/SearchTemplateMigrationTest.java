package com.awesomecopilot.search8x;

import org.junit.jupiter.api.Test;

/**
 * 测试 Stored Script (Search Template) API 迁移到 ES 8.x
 */
public class SearchTemplateMigrationTest {

    @Test
    public void testCreateAndDeleteSearchTemplate() {
        // 创建模板 - 使用现有的 employee_template.mustache
        boolean created = ElasticUtils.Admin.createSearchTemplate("test_employee_template", "scripts/employee_template.mustache");
        System.out.println("Template created: " + created);
        
        if (created) {
            // 删除模板
            boolean deleted = ElasticUtils.Admin.deleteSearchTemplate("test_employee_template");
            System.out.println("Template deleted: " + deleted);
        }
    }
    
    @Test
    public void testCreateMultiFieldAgg() {
        // 创建多字段聚合脚本
        boolean created = ElasticUtils.Cluster.createMultiFieldAgg();
        System.out.println("Multi-field agg script created: " + created);
        
        if (created) {
            // 清理
            boolean deleted = ElasticUtils.Admin.deleteSearchTemplate("multi_fields");
            System.out.println("Script deleted: " + deleted);
        }
    }
}
