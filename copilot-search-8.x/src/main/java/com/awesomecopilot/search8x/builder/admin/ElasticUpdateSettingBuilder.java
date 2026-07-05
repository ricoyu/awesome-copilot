package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.awesomecopilot.search8x.ElasticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

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
	
	private static final Logger log = LoggerFactory.getLogger(ElasticUpdateSettingBuilder.class);
	
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
		Map<String, Object> settingsMap = build();
		
		try {
			PutIndicesSettingsRequest request = PutIndicesSettingsRequest.of(b -> {
				b.index(java.util.Arrays.asList(indices));
				b.settings(IndexSettings.of(sb -> {
					if (settingsMap.containsKey("number_of_shards")) {
						sb.numberOfShards(String.valueOf(settingsMap.get("number_of_shards")));
					}
					if (settingsMap.containsKey("number_of_replicas")) {
						sb.numberOfReplicas(String.valueOf(settingsMap.get("number_of_replicas")));
					}
					if (settingsMap.containsKey("index.default_pipeline")) {
						sb.defaultPipeline(settingsMap.get("index.default_pipeline").toString());
					}
					if (settingsMap.containsKey("index.blocks.write")) {
						sb.blocks(bl -> bl.write(true));
					}
					// routing allocation require settings
					java.util.Map<String, co.elastic.clients.json.JsonData> otherSettings = new java.util.HashMap<>();
					for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
						if (entry.getKey().startsWith("index.routing.allocation.require.")) {
							otherSettings.put(entry.getKey(), co.elastic.clients.json.JsonData.of(entry.getValue().toString()));
						}
					}
					if (!otherSettings.isEmpty()) {
						sb.otherSettings(otherSettings);
					}
					return sb;
				}));
				return b;
			});
			
			PutIndicesSettingsResponse response = ElasticUtils.QUERY_CLIENT.indices().putSettings(request);
			return response.acknowledged();
		} catch (IOException e) {
			throw new RuntimeException("Failed to update settings for indices: " + java.util.Arrays.toString(indices), e);
		}
	}
}
