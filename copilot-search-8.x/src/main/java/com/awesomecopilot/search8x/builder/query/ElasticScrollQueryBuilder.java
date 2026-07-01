package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.ElasticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Scroll Query Builder for ES 8.x
 * <p>
 * 用于深度遍历大量数据, 使用scroll上下文保持查询结果一致性
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticScrollQueryBuilder extends BaseQueryBuilder {

    private static final Logger log = LoggerFactory.getLogger(ElasticScrollQueryBuilder.class);

    /**
     * scroll上下文保持时间, 默认5分钟
     */
    private String scrollTime = "5m";

    /**
     * 上一次scroll返回的scrollId, 用于获取下一批数据
     */
    private String scrollId;

    public ElasticScrollQueryBuilder(String... indices) {
        super(indices);
    }

    /**
     * 设置scrollId, 用于获取下一批scroll结果
     *
     * @param scrollId 上一次scroll返回的scrollId
     * @return ElasticScrollQueryBuilder
     */
    public ElasticScrollQueryBuilder scrollId(String scrollId) {
        this.scrollId = scrollId;
        return this;
    }

    /**
     * 设置scroll上下文保持时间, 默认"5m"
     *
     * @param scrollTime 如 "5m", "1m" 等
     * @return ElasticScrollQueryBuilder
     */
    public ElasticScrollQueryBuilder scrollTime(String scrollTime) {
        this.scrollTime = scrollTime;
        return this;
    }

    public ElasticScrollQueryBuilder resultType(Class resultType) {
        this.resultType = resultType;
        return this;
    }

    public ElasticScrollQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticScrollQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    /**
     * 返回当前scrollId, 用于下一次scroll请求
     *
     * @return scrollId
     */
    public String getScrollId() {
        return this.scrollId;
    }

    @Override
    protected Query buildQuery() {
        return null;
    }

    /**
     * 执行scroll查询, 返回结果列表
     * 如果有scrollId, 则使用scroll API获取下一批数据
     * 如果没有scrollId, 则执行初始查询
     *
     * @param <T> 结果类型
     * @return List<T>
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList() {
        try {
            if (scrollId != null) {
                return doScroll();
            }
            return doInitialSearch();
        } catch (IOException e) {
            throw new RuntimeException("Scroll query failed", e);
        }
    }

    private <T> List<T> doInitialSearch() throws IOException {
        Query query = buildQuery();

        SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
        if (indices != null && indices.length > 0) {
            requestBuilder.index(java.util.Arrays.asList(indices));
        }
        if (query != null) {
            requestBuilder.query(query);
        }
        if (size != null) {
            requestBuilder.size(size);
        }

        // source filtering
        if (includeSource != null && includeSource.length > 0) {
            requestBuilder.source(src -> src.filter(f -> f.includes(java.util.Arrays.asList(includeSource))));
        } else if (excludeSource != null && excludeSource.length > 0) {
            requestBuilder.source(src -> src.filter(f -> f.excludes(java.util.Arrays.asList(excludeSource))));
        }

        final String st = this.scrollTime;
        SearchRequest request = requestBuilder.scroll(s -> s.time(st)).build();

        if (log.isDebugEnabled()) {
            log.debug("Scroll initial query DSL:\n{}", request.toString());
        }

        SearchResponse<Map> response = ElasticUtils.QUERY_CLIENT.search(request, Map.class);
        this.scrollId = response.scrollId();

        return parseHits(response);
    }

    private <T> List<T> doScroll() throws IOException {
        final String sid = this.scrollId;
        final String st = this.scrollTime;

        co.elastic.clients.elasticsearch.core.ScrollRequest scrollRequest =
                co.elastic.clients.elasticsearch.core.ScrollRequest.of(
                        s -> s.scrollId(sid).scroll(t -> t.time(st))
                );

        ScrollResponse<Map> response = ElasticUtils.QUERY_CLIENT.scroll(scrollRequest, Map.class);
        this.scrollId = response.scrollId();

        return parseScrollHits(response);
    }

    private <T> List<T> parseScrollHits(ScrollResponse<Map> response) {
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

    private <T> List<T> parseHits(SearchResponse<Map> response) {
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
}
