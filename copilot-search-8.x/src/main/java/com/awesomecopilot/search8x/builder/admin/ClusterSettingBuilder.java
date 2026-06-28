package com.awesomecopilot.search8x.builder.admin;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.support.IndicesRestSupport;
import org.elasticsearch.common.settings.Settings;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Copyright: (C), 2021-04-18 8:39
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ClusterSettingBuilder {
	
	private ClusterPersistentSettingBuilder persistentSettingBuilder;
	
	public ClusterPersistentSettingBuilder persistent() {
		return new ClusterPersistentSettingBuilder(this);
	}
	
	public boolean update() {
		Map<String, Object> persistentSettings = null;
		if (persistentSettingBuilder != null) {
			Settings settings = ReflectionUtils.invokeMethod("build", persistentSettingBuilder);
			// 将 Settings 对象转换为 Map
			persistentSettings = settingsToMap(settings);
		}
		
		// 使用 ES 8.x ElasticsearchClient 更新集群设置
		return IndicesRestSupport.updateClusterSettings(ElasticUtils.QUERY_CLIENT, persistentSettings);
	}
	
	/**
	 * 将 Settings 对象转换为 Map<String, Object>
	 * 
	 * @param settings Settings 对象
	 * @return Map 表示的设置
	 */
	private Map<String, Object> settingsToMap(Settings settings) {
		if (settings == null) {
			return null;
		}
		
		Map<String, Object> map = new HashMap<>();
		// 通过 getAsMap() 方法获取 Settings 的 Map 表示
		try {
			java.lang.reflect.Method method = settings.getClass().getMethod("getAsMap");
			method.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, String> settingsMap = (Map<String, String>) method.invoke(settings);
			if (settingsMap != null) {
				map.putAll(settingsMap);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert Settings to Map", e);
		}
		
		return map;
	}
}
