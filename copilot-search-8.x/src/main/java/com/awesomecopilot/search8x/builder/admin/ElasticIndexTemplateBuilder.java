package com.awesomecopilot.search8x.builder.admin;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateResponse;
import co.elastic.clients.elasticsearch.indices.put_index_template.IndexTemplateMapping;
import com.awesomecopilot.search8x.ElasticUtils;
import com.awesomecopilot.search8x.enums.Dynamic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    public static ElasticIndexTemplateBuilder newInstance(String name) {
        ElasticIndexTemplateBuilder builder = new ElasticIndexTemplateBuilder();
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
        try {
            PutIndexTemplateRequest.Builder reqBuilder = new PutIndexTemplateRequest.Builder();
            reqBuilder.name(name);
            reqBuilder.indexPatterns(patterns);
            reqBuilder.priority((long) order);
            if (version != null) {
                reqBuilder.version((long) version);
            }

            IndexTemplateMapping.Builder templateBuilder = new IndexTemplateMapping.Builder();

            if (mappingBuilder != null) {
                Map<String, Object> mappingMap = mappingBuilder.build();
                TypeMapping typeMapping = ElasticIndexBuilder.buildTypeMapping(mappingMap);
                templateBuilder.mappings(typeMapping);
            }

            if (settings != null) {
                Map<String, Object> settingsMap = settings.build();
                IndexSettings indexSettings = ElasticIndexBuilder.buildIndexSettings(settingsMap);
                templateBuilder.settings(indexSettings);
            }

            reqBuilder.template(templateBuilder.build());

            PutIndexTemplateResponse response = ElasticUtils.QUERY_CLIENT.indices().putIndexTemplate(reqBuilder.build());
            return response.acknowledged();
        } catch (IOException e) {
            throw new RuntimeException("Failed to put index template: " + name, e);
        }
    }
}
