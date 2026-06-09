package com.awesomecopilot.search8x.builder.admin;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;

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
		ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();
		if (persistentSettingBuilder != null) {
			Settings persistentSettings = ReflectionUtils.invokeMethod("build", persistentSettingBuilder);
			request.persistentSettings(persistentSettings);
		}
		try {
			return ElasticUtils.CLIENT.cluster().putSettings(request, RequestOptions.DEFAULT).isAcknowledged();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
