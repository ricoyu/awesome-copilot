package com.awesomecopilot.search8x.builder.admin;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.Dynamic;
import com.awesomecopilot.search8x.support.IndicesRestSupport;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <p>
 * Copyright: (C), 2021-01-03 14:41
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticIndexBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticIndexBuilder.class);
	
	private final RestHighLevelClient client;
	
	private final co.elastic.clients.elasticsearch.ElasticsearchClient es8Client;
	
	private final String index;
	
	private AbstractMappingBuilder mappingBuilder;
	
	private ElasticSettingsBuilder settings;
	
	public ElasticIndexBuilder(RestHighLevelClient client, String index) {
		this.client = client;
		this.es8Client = null;
		this.index = index;
	}
	
	public ElasticIndexBuilder(co.elastic.clients.elasticsearch.ElasticsearchClient es8Client, String index) {
		this.client = null;
		this.es8Client = es8Client;
		this.index = index;
	}
	
	/**
	 * 通过MappingBuilder设置Index的Mapping, 默认dynamic为true
	 *
	 * @return IndexBuilder
	 */
	public ElasticIndexMappingBuilder mapping() {
		ElasticIndexMappingBuilder builder = new ElasticIndexMappingBuilder(this, Dynamic.TRUE);
		this.mappingBuilder = builder;
		return builder;
	}
	
	/**
	 * 通过MappingBuilder设置Index的Mapping
	 *
	 * @param dynamic
	 * @return IndexBuilder
	 */
	public ElasticIndexMappingBuilder mapping(Dynamic dynamic) {
		ElasticIndexMappingBuilder builder = new ElasticIndexMappingBuilder(this, dynamic);
		this.mappingBuilder = builder;
		return builder;
	}
	
	/**
	 * 通过MappingBuilder设置Index的Mapping
	 *
	 * @param mappingBuilder
	 * @return IndexBuilder
	 */
	public ElasticIndexBuilder mapping(AbstractMappingBuilder mappingBuilder) {
		this.mappingBuilder = mappingBuilder;
		return this;
	}
	
	/**
	 * 设置Index的Settings
	 *
	 * @param settings
	 * @return IndexBuilder
	 */
	public ElasticIndexBuilder settings(ElasticSettingsBuilder settings) {
		this.settings = settings;
		return this;
	}
	
	/**
	 * 设置Index的主分片数
	 *
	 * @return IndexBuilder
	 */
	public ElasticIndexSettingsBuilder settings() {
		ElasticIndexSettingsBuilder elasticSettingsBuilder = new ElasticIndexSettingsBuilder(this);
		elasticSettingsBuilder.numberOfShards(1);
		return elasticSettingsBuilder;
	}
	
	/**
	 * 创建index
	 *
	 * @return
	 */
	public boolean create() {
		// 优先使用 ES 8.x Java Client
		if (es8Client != null) {
			return createWithES8Client();
		} else if (client != null) {
			return createWithES7Client();
		} else {
			throw new IllegalStateException("No client available");
		}
	}
	
	/**
	 * 使用 ES 8.x Java Client 创建索引
	 */
	private boolean createWithES8Client() {
		Map<String, Object> mappings = null;
		Map<String, Object> settingsMap = null;
		
		if (mappingBuilder != null) {
			// 提取 properties 部分
			Map<String, Object> fullMapping = mappingBuilder.build();
			mappings = new java.util.HashMap<>();
			mappings.put("properties", fullMapping.get("properties"));
		}
		
		if (settings != null) {
			settingsMap = (Map<String, Object>) ReflectionUtils.invokeMethod("build", settings);
		}
		
		return IndicesRestSupport.createIndex(es8Client, index, settingsMap, mappings);
	}
	
	/**
	 * 使用 ES 7.x RestHighLevelClient 创建索引（已废弃）
	 */
	@Deprecated
	private boolean createWithES7Client() {
		CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
		
		if (mappingBuilder != null) {
			// ES 7.x: 使用 source() 方法设置完整的索引配置
			Map<String, Object> requestBody = new java.util.HashMap<>();
			
			// 添加 mappings（只包含 properties）
			Map<String, Object> fullMapping = mappingBuilder.build();
			Map<String, Object> mappingsWrapper = new java.util.HashMap<>();
			mappingsWrapper.put("properties", fullMapping.get("properties"));
			requestBody.put("mappings", mappingsWrapper);
			
			// 添加 settings
			if (settings != null) {
				requestBody.put("settings", ReflectionUtils.invokeMethod("build", settings));
			}
			
			String requestJson = com.awesomecopilot.json.jackson.JacksonUtils.toJson(requestBody);
			createIndexRequest.source(requestJson, org.elasticsearch.xcontent.XContentType.JSON);
		} else if (settings != null) {
			createIndexRequest.settings((Settings) ReflectionUtils.invokeMethod("build", settings));
		}
		return IndicesRestSupport.createIndex(client, createIndexRequest);
	}
	
}
