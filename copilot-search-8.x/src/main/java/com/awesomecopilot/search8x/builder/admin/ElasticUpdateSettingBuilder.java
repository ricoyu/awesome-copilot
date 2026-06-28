package com.awesomecopilot.search8x.builder.admin;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.support.IndicesRestSupport;
import org.elasticsearch.common.settings.Settings;

/**
 * <p>
 * Copyright: (C), 2021-04-10 9:51
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ElasticUpdateSettingBuilder extends ElasticSettingsBuilder {
	
	private String[] indices;
	
	public ElasticUpdateSettingBuilder(String... indices) {
		this.indices = indices;
	}
	
	@Override
	public ElasticUpdateSettingBuilder numberOfShards(int numberOfShards) {
		super.numberOfShards(numberOfShards);
		return this;
	}
	
	@Override
	public ElasticUpdateSettingBuilder numberOfReplicas(int numberOfReplicas) {
		super.numberOfReplicas(numberOfReplicas);
		return this;
	}
	
	@Override
	public ElasticUpdateSettingBuilder defaultPipeline(String defaultPipeline) {
		super.defaultPipeline(defaultPipeline);
		return this;
	}
	
	@Override
	public ElasticUpdateSettingBuilder indexRoutingAllocation(String key, String value) {
		super.indexRoutingAllocation(key, value);
		return this;
	}
	
	/**
	 * 更新索引的Settings
	 * @return
	 */
	public boolean thenUpdate() {
		Settings settings = ReflectionUtils.invokeMethod("build", this);
		// 将 Settings 对象转换为 Map
		java.util.Map<String, Object> settingsMap = settingsToMap(settings);
		return IndicesRestSupport.updateIndexSettings(ElasticUtils.QUERY_CLIENT, indices, settingsMap);
	}
	
	/**
	 * 将 Settings 对象转换为 Map<String, Object>
	 * 
	 * @param settings Settings 对象
	 * @return Map 表示的设置
	 */
	private java.util.Map<String, Object> settingsToMap(Settings settings) {
		if (settings == null) {
			return null;
		}
		
		java.util.Map<String, Object> map = new java.util.HashMap<>();
		// 通过 getAsMap() 方法获取 Settings 的 Map 表示
		try {
			java.lang.reflect.Method method = settings.getClass().getMethod("getAsMap");
			method.setAccessible(true);
			@SuppressWarnings("unchecked")
			java.util.Map<String, String> settingsMap = (java.util.Map<String, String>) method.invoke(settings);
			if (settingsMap != null) {
				map.putAll(settingsMap);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert Settings to Map", e);
		}
		
		return map;
	}
}
