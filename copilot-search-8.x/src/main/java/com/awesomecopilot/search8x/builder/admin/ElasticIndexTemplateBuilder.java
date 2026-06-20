package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.Dynamic;
import com.awesomecopilot.search8x.support.IndicesRestSupport;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;

/**
 * <p>
 * Copyright: (C), 2021-01-06 9:07
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public final class ElasticIndexTemplateBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticIndexTemplateBuilder.class);
	
	private RestHighLevelClient client;
	
	private ElasticsearchClient es8Client;
	
	/**
	 * Index Template的名字
	 */
	private String name;
	
	private List<String> patterns = new ArrayList<>();
	
	/**
	 * order越低, 优先级越低, 即同一个设置会被优先级更高的Index Template覆盖
	 */
	private int order;
	
	private Integer version;
	
	/**
	 * 索引的settings, 常用的比如有
	 * <ol>
	 * <li/>number_of_shards
	 * <li/>number_of_replicas
	 * </ol>
	 */
	private ElasticSettingsBuilder settings;
	
	/**
	 * 添加Mappings
	 */
	private ElasticIndexTemplateMappingBuilder mappingBuilder;
	
	private ElasticIndexTemplateBuilder() {
	}
	
	/**
	 * 创建一个IndexTemplateBuilder, 并指定Index Template的名字
	 *
	 * @param name
	 * @return
	 */
	public static ElasticIndexTemplateBuilder newInstance(RestHighLevelClient client, String name) {
		ElasticIndexTemplateBuilder builder = new ElasticIndexTemplateBuilder();
		builder.client = client;
		builder.name = name;
		return builder;
	}
	
	/**
	 * 创建一个IndexTemplateBuilder (ES 8.x), 并指定Index Template的名字
	 *
	 * @param es8Client ES 8.x ElasticsearchClient
	 * @param name      Index Template 名称
	 * @return ElasticIndexTemplateBuilder
	 */
	public static ElasticIndexTemplateBuilder newInstance(ElasticsearchClient es8Client, String name) {
		ElasticIndexTemplateBuilder builder = new ElasticIndexTemplateBuilder();
		builder.es8Client = es8Client;
		builder.name = name;
		return builder;
	}
	
	/**
	 * 这个Index Template匹配的Index的表达式
	 *
	 * @param patterns
	 * @return
	 */
	public ElasticIndexTemplateBuilder patterns(String... patterns) {
		Objects.requireNonNull(patterns, "patterns can not be null");
		this.patterns = asList(patterns);
		return this;
	}
	
	/**
	 * order越低, 优先级越低, 即同一个设置会被优先级更高的Index Template覆盖
	 *
	 * @param order
	 * @return
	 */
	public ElasticIndexTemplateBuilder order(int order) {
		this.order = order;
		return this;
	}
	
	public ElasticIndexTemplateBuilder version(Integer version) {
		this.version = version;
		return this;
	}
	
	
	/**
	 * 索引的settings, 常用的比如有
	 * <ol>
	 * <li/>number_of_shards
	 * <li/>number_of_replicas
	 * </ol>
	 * <p>
	 * 可以用Settings.builder()逐项设置
	 *
	 * @param settings
	 * @return
	 */
	public ElasticIndexTemplateBuilder settings(ElasticSettingsBuilder settings) {
		this.settings = settings;
		return this;
	}
	
	/**
	 * 为索引模板设置Settings
	 *
	 * @param numOfShards
	 * @return ElasticIndexTemplateSettingsBuilder
	 */
	public ElasticIndexTemplateSettingsBuilder settings(int numOfShards) {
		ElasticIndexTemplateSettingsBuilder elasticIndexTemplateSettingsBuilder = new ElasticIndexTemplateSettingsBuilder(this);
		elasticIndexTemplateSettingsBuilder.numberOfShards(numOfShards);
		return elasticIndexTemplateSettingsBuilder;
	}
	
	/**
	 * 通过MappingBuilder逐项配置Mapping
	 *
	 * @return ElasticIndexTemplateMappingBuilder
	 */
	public ElasticIndexTemplateMappingBuilder mappings() {
		this.mappingBuilder = new ElasticIndexTemplateMappingBuilder(this, Dynamic.TRUE);
		return mappingBuilder;
	}
	
	/**
	 * 通过MappingBuilder逐项配置Mapping
	 *
	 * @param dynamic
	 * @return ElasticIndexTemplateMappingBuilder
	 */
	public ElasticIndexTemplateMappingBuilder mappings(Dynamic dynamic) {
		this.mappingBuilder = new ElasticIndexTemplateMappingBuilder(this, dynamic);
		return mappingBuilder;
	}
	
	/**
	 * 执行创建或者更新Index Template
	 */
	public boolean create() {
		// 优先使用 ES 8.x ElasticsearchClient
		if (es8Client != null) {
			return createWithES8Client();
		} else if (client != null) {
			return createWithES7Client();
		} else {
			throw new IllegalStateException("No client available");
		}
	}
	
	/**
	 * 使用 ES 8.x ElasticsearchClient 创建索引模板
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
		
		return IndicesRestSupport.putIndexTemplate(es8Client, name, patterns, order, version, settingsMap, mappings);
	}
	
	/**
	 * 使用 ES 7.x RestHighLevelClient 创建索引模板（已废弃）
	 */
	@Deprecated
	private boolean createWithES7Client() {
		PutIndexTemplateRequest request = new PutIndexTemplateRequest(name);
		request.patterns(patterns);
		request.order(order);
		if (version != null) {
			request.version(version);
		}
		if (mappingBuilder != null) {
			// ES 8.x: 将 mapping 转为 JSON 字符串
			String mappingJson = JacksonUtils.toJson(mappingBuilder.build());
			request.mapping(mappingJson, org.elasticsearch.xcontent.XContentType.JSON);
		}
		if (settings != null) {
			request.settings((org.elasticsearch.common.settings.Settings) ReflectionUtils.invokeMethod("build", settings));
		}
		return IndicesRestSupport.putIndexTemplate(client, request);
	}
}