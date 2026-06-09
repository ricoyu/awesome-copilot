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
	
	private final String index;
	
	private AbstractMappingBuilder mappingBuilder;
	
	private ElasticSettingsBuilder settings;
	
	public ElasticIndexBuilder(RestHighLevelClient client, String index) {
		this.client = client;
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
		CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
		if (mappingBuilder != null) {
			createIndexRequest.mapping(ElasticUtils.ONLY_TYPE, mappingBuilder.build());
		}
		if (settings != null) {
			createIndexRequest.settings((Settings) ReflectionUtils.invokeMethod("build", settings));
		}
		return IndicesRestSupport.createIndex(client, createIndexRequest);
	}
	
}
