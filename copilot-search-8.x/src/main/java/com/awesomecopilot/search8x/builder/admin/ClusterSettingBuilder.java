package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch.cluster.PutClusterSettingsRequest;
import co.elastic.clients.elasticsearch.cluster.PutClusterSettingsResponse;
import co.elastic.clients.json.JsonData;
import com.awesomecopilot.search8x.ElasticUtils;

import java.io.IOException;
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
	
	void setPersistentSettingBuilder(ClusterPersistentSettingBuilder persistentSettingBuilder) {
		this.persistentSettingBuilder = persistentSettingBuilder;
	}
	
	public boolean update() {
		try {
			PutClusterSettingsRequest.Builder reqBuilder = new PutClusterSettingsRequest.Builder();
			if (persistentSettingBuilder != null) {
				Map<String, JsonData> persistentSettings = persistentSettingBuilder.build();
				reqBuilder.persistent(persistentSettings);
			}
			
			PutClusterSettingsResponse response = ElasticUtils.QUERY_CLIENT.cluster().putSettings(reqBuilder.build());
			return response.acknowledged();
		} catch (IOException e) {
			throw new RuntimeException("Failed to update cluster settings", e);
		}
	}
}
