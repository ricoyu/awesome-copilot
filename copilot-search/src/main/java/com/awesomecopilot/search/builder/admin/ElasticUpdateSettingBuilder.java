package com.awesomecopilot.search.builder.admin;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search.ElasticUtils;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
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
		AcknowledgedResponse response = ElasticUtils.CLIENT
				.admin()
				.indices()
				.prepareUpdateSettings(indices)
				.setSettings(settings)
				.get();
		return response.isAcknowledged();
	}
}
