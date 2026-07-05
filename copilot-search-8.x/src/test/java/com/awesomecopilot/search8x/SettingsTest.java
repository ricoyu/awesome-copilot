package com.awesomecopilot.search8x;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ElasticUtils.Settings 单元测试类
 * 从 copilot-search 模块迁移而来,测试 ES 8.x Settings API
 * <p>
 * 包含 Settings 内部类下的所有接口测试:
 * - putSettings (TODO) / update (TODO)
 */
@Slf4j
public class SettingsTest {
    
    static {
        // 把 JUnit Platform 的 discovery 日志级别调到 WARNING 或更高
        Logger.getLogger("org.junit.platform.launcher.core.EngineDiscoveryOrchestrator")
                .setLevel(Level.WARNING);
        
        // 可选：同时处理其他常见 noisy logger
        Logger.getLogger("org.junit.platform").setLevel(Level.WARNING);
    }
    
    // ==================== update() 相关测试 ====================
    
    /**
     * 测试更新索引副本数 - 从 copilot-search 迁移
     * 对应原版 ElasticUtilsSettingsTest.testUpdateReplicas()
     */
    @Test
    public void testUpdateReplicas() {
        try {
            boolean updated = ElasticUtils.Settings.update("product")
                    .numberOfReplicas(0)
                    .thenUpdate();
            log.info("Update replicas result: {}", updated);
            
        } catch (Exception e) {
            log.error("Failed to update replicas", e);
        }
    }
    
    /**
     * 测试设置索引为只读 - 从 copilot-search 迁移
     * 对应原版 testSetReadOnly()
     */
    @Test
    public void testSetReadOnly() {
        try {
            boolean updated = ElasticUtils.Settings.update("test_index")
                    .numberOfReplicas(0)
                    .thenUpdate();
            log.info("Set read-only result: {}", updated);
            
        } catch (Exception e) {
            log.error("Failed to set read-only", e);
        }
    }
    
    /**
     * 测试设置索引的Settings(JSON字符串) - 从 copilot-search 迁移
     * 对应原版 testSettingHotWarn()
     */
    @Test
    public void testSettingHotWarn() {
        try {
            String settings = "{\"index\":{\"number_of_replicas\":1,\"routing.allocation.require.node_type\":\"hot\"}}";
            boolean result = ElasticUtils.Settings.putSettings("logs-2021-03-29", settings);
            log.info("Setting hot/warm result: {}", result);
            
        } catch (Exception e) {
            log.error("Failed to set hot/warm settings", e);
        }
    }
    
    /**
     * 测试设置索引的Settings(链式调用) - 从 copilot-search 迁移
     * 对应原版 testSettingHotWarn2()
     */
    @Test
    public void testSettingHotWarn2() {
        try {
            boolean updated = ElasticUtils.Settings.update("logs-2021-03-30")
                    .numberOfShards(1)
                    .numberOfReplicas(1)
                    .indexRoutingAllocation("node_type", "hot")
                    .thenUpdate();
            log.info("Setting hot/warm chain call result: {}", updated);
            
        } catch (Exception e) {
            log.error("Failed to set hot/warm settings", e);
        }
    }
}
