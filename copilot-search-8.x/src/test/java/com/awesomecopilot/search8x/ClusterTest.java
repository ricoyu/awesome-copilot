package com.awesomecopilot.search8x;

import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.enums.cluster.AllocationEnable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ElasticUtils.Cluster 单元测试类
 * 从 copilot-search 模块迁移而来,测试 ES 8.x Cluster API
 * <p>
 * 包含 Cluster 内部类下的所有接口测试:
 * - health / settings (TODO) / allSettings (TODO)
 */
@Slf4j
public class ClusterTest {
    
    static {
        // 把 JUnit Platform 的 discovery 日志级别调到 WARNING 或更高
        Logger.getLogger("org.junit.platform.launcher.core.EngineDiscoveryOrchestrator")
                .setLevel(Level.WARNING);
        
        // 可选：同时处理其他常见 noisy logger
        Logger.getLogger("org.junit.platform").setLevel(Level.WARNING);
    }
    
    // ==================== health() 相关测试 ====================
    
    /**
     * 测试集群健康状态 - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsClusterTest.testClusterHealth()
     */
    @Test
    public void testClusterHealth() {
        try {
            String health = ElasticUtils.Cluster.health();
            log.info("Cluster health: {}", health);
            
            assertThat(health).isIn("green", "yellow", "red");
            
        } catch (Exception e) {
            log.error("Failed to get cluster health", e);
        }
    }
    
    /**
     * 测试集群健康状态(多次调用) - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsClusterTest.testClusterHealth() 第二次调用
     */
    @Test
    public void testClusterHealthMultiple() {
        try {
            String health = ElasticUtils.Cluster.health();
            log.info("Cluster health (first call): {}", health);
            
            String health2 = ElasticUtils.Cluster.health();
            log.info("Cluster health (second call): {}", health2);
            
            assertThat(health).isIn("green", "yellow", "red");
            assertThat(health2).isIn("green", "yellow", "red");
            
        } catch (Exception e) {
            log.error("Failed to get cluster health", e);
        }
    }
    
    // ==================== settings() 相关测试 ====================
    
    /**
     * 测试集群持久化设置 - 从 copilot-search 迁移
     * 对应原版 testClusterPersistentSettings()
     */
    @Test
    public void testClusterPersistentSettings() {
        try {
            boolean acknowledge = ElasticUtils.Cluster.settings()
                    .persistent()
                    .routingAllocationEnable(AllocationEnable.ALL)
                    .and()
                    .update();
            log.info("Cluster persistent settings result: {}", acknowledge);
            
        } catch (Exception e) {
            log.error("Failed to set cluster persistent settings", e);
        }
    }
    
    /**
     * 测试获取所有集群设置 - 从 copilot-search 迁移
     * 对应原版 testAllClusterSettings()
     */
    @Test
    public void testAllClusterSettings() {
        try {
            java.util.Map<String, Object> allSettings = ElasticUtils.Cluster.allSettings();
            log.info("All cluster settings: {}", JacksonUtils.toPrettyJson(allSettings));
            
        } catch (Exception e) {
            log.error("Failed to get all cluster settings", e);
        }
    }
}
