package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.SearchTemplateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Search Template Query Builder for ES 8.x
 * <p>
 * 基于 Search Template 查询, 使用预定义的Mustache模板来执行查询
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticTemplateQueryBuilder {

    private static final Logger log = LoggerFactory.getLogger(ElasticTemplateQueryBuilder.class);

    private String[] indices;

    private String templateName;

    private Map<String, Object> params = new HashMap<>();

    private Class resultType;

    public ElasticTemplateQueryBuilder(String... indices) {
        this.indices = indices;
    }

    /**
     * 设置存储的模板名称
     *
     * @param templateName 模板名称
     * @return ElasticTemplateQueryBuilder
     */
    public ElasticTemplateQueryBuilder templateName(String templateName) {
        this.templateName = templateName;
        return this;
    }

    /**
     * 批量设置模板参数
     *
     * @param params 参数Map
     * @return ElasticTemplateQueryBuilder
     */
    public ElasticTemplateQueryBuilder params(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

    /**
     * 设置单个模板参数
     *
     * @param paramName  参数名
     * @param paramValue 参数值
     * @return ElasticTemplateQueryBuilder
     */
    public ElasticTemplateQueryBuilder param(String paramName, Object paramValue) {
        this.params.put(paramName, paramValue);
        return this;
    }

    /**
     * 设置结果类型
     *
     * @param resultType 结果类型
     * @return ElasticTemplateQueryBuilder
     */
    public ElasticTemplateQueryBuilder resultType(Class resultType) {
        this.resultType = resultType;
        return this;
    }

    /**
     * 执行查询, 返回结果列表
     *
     * @param <T> 结果类型
     * @return List<T>
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList() {
        SearchTemplateResponse<Map> response = doSearchTemplate();
        List<Hit<Map>> hits = response.hits().hits();

        if (hits.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> results = new ArrayList<>();
        for (Hit<Map> hit : hits) {
            Map<String, Object> source = hit.source();
            if (source == null) {
                continue;
            }
            if (resultType == null || resultType == Object.class || resultType == String.class) {
                results.add((T) JacksonUtils.toJson(source));
            } else {
                results.add((T) JacksonUtils.toObject(JacksonUtils.toJson(source), resultType));
            }
        }
        return results;
    }

    /**
     * 执行查询, 返回一条记录
     *
     * @param <T> 结果类型
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T queryForOne() {
        SearchTemplateResponse<Map> response = doSearchTemplate();
        List<Hit<Map>> hits = response.hits().hits();

        if (hits.isEmpty()) {
            return null;
        }

        Hit<Map> hit = hits.get(0);
        Map<String, Object> source = hit.source();
        if (source == null) {
            return null;
        }

        if (resultType == null || resultType == Object.class || resultType == String.class) {
            return (T) JacksonUtils.toJson(source);
        }

        return (T) JacksonUtils.toObject(JacksonUtils.toJson(source), resultType);
    }

    /**
     * 执行查询, 返回一条记录（与queryForOne相同）
     *
     * @param <T> 结果类型
     * @return T
     */
    public <T> T queryForObject() {
        return queryForOne();
    }

    /**
     * 执行Search Template查询
     *
     * @return SearchTemplateResponse
     */
    private SearchTemplateResponse<Map> doSearchTemplate() {
        try {
            // 将 Object params 转为 JsonData params
            Map<String, JsonData> jsonDataParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                jsonDataParams.put(entry.getKey(), JsonData.of(entry.getValue()));
            }

            final String tplName = this.templateName;
            final String[] idx = this.indices;

            co.elastic.clients.elasticsearch.core.SearchTemplateRequest request =
                    co.elastic.clients.elasticsearch.core.SearchTemplateRequest.of(b -> {
                        b.index(java.util.Arrays.asList(idx))
                         .id(tplName)
                         .params(jsonDataParams);
                        return b;
                    });

            if (log.isDebugEnabled()) {
                log.debug("Search Template: {} with params: {}", templateName, params);
            }

            return ElasticUtils.QUERY_CLIENT.searchTemplate(request, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Search template query failed", e);
        }
    }
}
