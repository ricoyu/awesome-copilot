package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Match All Query Builder for ES 8.x
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticMatchAllQueryBuilder extends BaseQueryBuilder {

    public ElasticMatchAllQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticMatchAllQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticMatchAllQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    @Override
    protected Query buildQuery() {
        return Query.of(q -> q.matchAll(ma -> ma));
    }
}
