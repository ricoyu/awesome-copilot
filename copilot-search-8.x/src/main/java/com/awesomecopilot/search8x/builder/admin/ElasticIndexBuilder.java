package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonData;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.Dynamic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
	
	private String index;
	
	private AbstractMappingBuilder mappingBuilder;
	
	private ElasticSettingsBuilder settings;
	
	public ElasticIndexBuilder(String index) {
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
		try {
			CreateIndexRequest.Builder reqBuilder = new CreateIndexRequest.Builder();
			reqBuilder.index(index);
			
			if (mappingBuilder != null) {
				Map<String, Object> mappingMap = mappingBuilder.build();
				TypeMapping typeMapping = buildTypeMapping(mappingMap);
				reqBuilder.mappings(typeMapping);
			}
			
			if (settings != null) {
				Map<String, Object> settingsMap = settings.build();
				IndexSettings indexSettings = buildIndexSettings(settingsMap);
				reqBuilder.settings(indexSettings);
			}
			
			CreateIndexRequest request = reqBuilder.build();
			CreateIndexResponse response = ElasticUtils.QUERY_CLIENT.indices().create(request);
			return response.acknowledged();
		} catch (IOException e) {
			throw new RuntimeException("Failed to create index: " + index, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	static TypeMapping buildTypeMapping(Map<String, Object> mappingMap) {
		return TypeMapping.of(b -> {
			if (mappingMap == null) {
				return b;
			}
			String dynamic = (String) mappingMap.get("dynamic");
			if ("true".equals(dynamic)) {
				b.dynamic(DynamicMapping.True);
			} else if ("strict".equals(dynamic)) {
				b.dynamic(DynamicMapping.Strict);
			} else {
				b.dynamic(DynamicMapping.False);
			}
			
			Map<String, Object> source = (Map<String, Object>) mappingMap.get("_source");
			if (source != null && Boolean.FALSE.equals(source.get("enabled"))) {
				b.source(s -> s.enabled(false));
			}
			
			Map<String, Object> propsMap = (Map<String, Object>) mappingMap.get("properties");
			if (propsMap != null) {
				for (Map.Entry<String, Object> entry : propsMap.entrySet()) {
					Map<String, Object> fieldMapping = (Map<String, Object>) entry.getValue();
					b.properties(entry.getKey(), buildProperty(fieldMapping));
				}
			}
			return b;
		});
	}
	
	@SuppressWarnings("unchecked")
	static Property buildProperty(Map<String, Object> fieldMapping) {
		String type = (String) fieldMapping.get("type");
		if (type == null) {
			type = "keyword";
		}
		switch (type) {
			case "text":
				return Property.of(p -> p.text(t -> {
					if (fieldMapping.containsKey("analyzer")) {
						t.analyzer(fieldMapping.get("analyzer").toString());
					}
					if (fieldMapping.containsKey("search_analyzer")) {
						t.searchAnalyzer(fieldMapping.get("search_analyzer").toString());
					}
					if (fieldMapping.containsKey("copy_to")) {
						t.copyTo(Collections.singletonList(fieldMapping.get("copy_to").toString()));
					}
					if (Boolean.TRUE.equals(fieldMapping.get("eager_global_ordinals"))) {
						t.eagerGlobalOrdinals(true);
					}
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) {
						t.index(false);
					}
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) {
						t.store(true);
					}
					return t;
				}));
			case "keyword":
				return Property.of(p -> p.keyword(k -> {
					if (fieldMapping.containsKey("null_value")) {
						k.nullValue(fieldMapping.get("null_value").toString());
					}
					if (Boolean.TRUE.equals(fieldMapping.get("eager_global_ordinals"))) {
						k.eagerGlobalOrdinals(true);
					}
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) {
						k.index(false);
					}
					if (fieldMapping.containsKey("copy_to")) {
						k.copyTo(Collections.singletonList(fieldMapping.get("copy_to").toString()));
					}
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) {
						k.store(true);
					}
					return k;
				}));
			case "long":
				return Property.of(p -> p.long_(l -> {
					if (fieldMapping.containsKey("null_value")) {
						l.nullValue(Long.parseLong(fieldMapping.get("null_value").toString()));
					}
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) l.index(false);
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) l.store(true);
					return l;
				}));
			case "integer":
				return Property.of(p -> p.integer(i -> {
					if (fieldMapping.containsKey("null_value")) {
						i.nullValue(Integer.parseInt(fieldMapping.get("null_value").toString()));
					}
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) i.index(false);
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) i.store(true);
					return i;
				}));
			case "double":
				return Property.of(p -> p.double_(d -> {
					if (fieldMapping.containsKey("null_value")) {
						d.nullValue(Double.parseDouble(fieldMapping.get("null_value").toString()));
					}
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) d.index(false);
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) d.store(true);
					return d;
				}));
			case "float":
				return Property.of(p -> p.float_(f -> {
					if (fieldMapping.containsKey("null_value")) {
						f.nullValue(Float.parseFloat(fieldMapping.get("null_value").toString()));
					}
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) f.index(false);
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) f.store(true);
					return f;
				}));
			case "date":
				return Property.of(p -> p.date(d -> {
					if (fieldMapping.containsKey("format")) {
						d.format(fieldMapping.get("format").toString());
					}
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) d.index(false);
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) d.store(true);
					return d;
				}));
			case "boolean":
				return Property.of(p -> p.boolean_(b -> {
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) b.index(false);
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) b.store(true);
					return b;
				}));
			case "object":
				return Property.of(p -> p.object(o -> {
					if (fieldMapping.containsKey("enabled")) {
						o.enabled(Boolean.TRUE.equals(fieldMapping.get("enabled")));
					}
					return o;
				}));
			case "nested":
				return Property.of(p -> p.nested(n -> n));
			case "ip":
				return Property.of(p -> p.ip(i -> {
					if (Boolean.FALSE.equals(fieldMapping.get("index"))) i.index(false);
					if (Boolean.TRUE.equals(fieldMapping.get("store"))) i.store(true);
					return i;
				}));
			case "geo_point":
				return Property.of(p -> p.geoPoint(g -> g));
			default:
				return Property.of(p -> p.keyword(k -> k));
		}
	}
	
	static IndexSettings buildIndexSettings(Map<String, Object> settingsMap) {
		return IndexSettings.of(b -> {
			if (settingsMap != null) {
				if (settingsMap.containsKey("number_of_shards")) {
					b.numberOfShards(String.valueOf(settingsMap.get("number_of_shards")));
				}
				if (settingsMap.containsKey("number_of_replicas")) {
					b.numberOfReplicas(String.valueOf(settingsMap.get("number_of_replicas")));
				}
				if (settingsMap.containsKey("index.default_pipeline")) {
					b.defaultPipeline(settingsMap.get("index.default_pipeline").toString());
				}
				if (Boolean.TRUE.equals(settingsMap.get("index.blocks.write"))) {
					b.blocks(bl -> bl.write(true));
				}
				// routing allocation require settings
				Map<String, JsonData> otherSettings = new HashMap<>();
				for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
					if (entry.getKey().startsWith("index.routing.allocation.require.")) {
						otherSettings.put(entry.getKey(), JsonData.of(entry.getValue().toString()));
					}
				}
				if (!otherSettings.isEmpty()) {
					b.otherSettings(otherSettings);
				}
			}
			return b;
		});
	}
}
