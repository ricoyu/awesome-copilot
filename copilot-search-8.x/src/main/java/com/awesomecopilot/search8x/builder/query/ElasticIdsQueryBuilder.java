package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * IDs Query Builder for ES 8.x
 * <p>
 * 基于文档ID列表查询, 相当于 SQL 中的 WHERE id IN (...)
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticIdsQueryBuilder extends BaseQueryBuilder {

    private String[] ids;

    public ElasticIdsQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticIdsQueryBuilder ids(Long... ids) {
        Objects.requireNonNull(ids, "ids cannot be null!");
        this.ids = Arrays.stream(ids).map(String::valueOf).toArray(String[]::new);
        this.size = ids.length;
        return this;
    }

    public ElasticIdsQueryBuilder ids(Integer... ids) {
        Objects.requireNonNull(ids, "ids cannot be null!");
        this.ids = Arrays.stream(ids).map(String::valueOf).toArray(String[]::new);
        this.size = ids.length;
        return this;
    }

    public ElasticIdsQueryBuilder ids(String... ids) {
        Objects.requireNonNull(ids, "ids cannot be null!");
        this.ids = ids;
        this.size = ids.length;
        return this;
    }

    public ElasticIdsQueryBuilder ids(List<String> ids) {
        Objects.requireNonNull(ids, "ids cannot be null!");
        this.ids = ids.stream().toArray(String[]::new);
        this.size = ids.size();
        return this;
    }

    public ElasticIdsQueryBuilder ids(Set<String> ids) {
        Objects.requireNonNull(ids, "ids cannot be null!");
        this.ids = ids.stream().toArray(String[]::new);
        this.size = ids.size();
        return this;
    }

    public ElasticIdsQueryBuilder resultType(Class resultType) {
        this.resultType = resultType;
        return this;
    }

    /**
     * 控制返回自己想要的字段, 而不是整个_source
     *
     * @param fields
     * @return ElasticIdsQueryBuilder
     */
    public ElasticIdsQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    /**
     * 控制要排除哪些返回的字段, 而不是整个_source
     *
     * @param fields
     * @return ElasticIdsQueryBuilder
     */
    public ElasticIdsQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    /**
     * 控制返回自己想要的字段, 而不是整个_source
     *
     * @param fields
     * @return ElasticIdsQueryBuilder
     */
    public ElasticIdsQueryBuilder includeSources(List<String> fields) {
        String[] sources = fields.stream().toArray(String[]::new);
        this.includeSource = sources;
        return this;
    }

    /**
     * 控制要排除哪些返回的字段, 而不是整个_source
     *
     * @param fields
     * @return ElasticIdsQueryBuilder
     */
    public ElasticIdsQueryBuilder excludeSources(List<String> fields) {
        String[] sources = fields.stream().toArray(String[]::new);
        this.excludeSource = sources;
        return this;
    }

    @Override
    protected Query buildQuery() {
        final String[] docIds = this.ids;
        return Query.of(q -> q.ids(idsQuery -> idsQuery.values(Arrays.asList(docIds))));
    }
}
