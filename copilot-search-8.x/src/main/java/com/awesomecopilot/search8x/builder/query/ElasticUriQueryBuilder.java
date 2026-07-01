package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * URI Query Builder for ES 8.x
 * <p>
 * 在ES 8.x中, URI query被映射为query_string查询
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticUriQueryBuilder extends BaseQueryBuilder {

    private String queryString;

    public ElasticUriQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticUriQueryBuilder query(String queryString) {
        this.queryString = queryString;
        return this;
    }

    public ElasticUriQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    @Override
    protected Query buildQuery() {
        return Query.of(q -> q.queryString(qs -> qs.query(queryString)));
    }
}
