package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * 通用查询构建器 for ES 8.x
 * <p>
 * 可以传入任意的Query对象
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticQueryBuilder extends BaseQueryBuilder {

    private Query query;

    private ElasticQueryBuilder(String... indices) {
        super(indices);
    }

    public static ElasticQueryBuilder instance(String... indices) {
        return new ElasticQueryBuilder(indices);
    }

    /**
     * 设置Query对象
     */
    public ElasticQueryBuilder query(Query query) {
        this.query = query;
        return this;
    }

    /**
     * 设置Query对象(与query()相同, 为兼容原版API)
     * <p>
     * 允许用户直接传入已构建好的Query对象, 而不是通过子类实现buildQuery()
     *
     * @param query ES 8.x Query对象
     * @return ElasticQueryBuilder
     */
    public ElasticQueryBuilder queryBuilder(Query query) {
        return query(query);
    }

    public ElasticQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    @Override
    protected Query buildQuery() {
        return query;
    }
}
