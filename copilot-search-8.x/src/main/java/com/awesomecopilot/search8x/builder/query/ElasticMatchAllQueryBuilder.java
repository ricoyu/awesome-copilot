package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Match All Query Builder for ES 8.x
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticMatchAllQueryBuilder extends BaseQueryBuilder implements BoolQuery {

    private ElasticBoolQueryBuilder boolQueryBuilder;

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
    public ElasticBoolQueryBuilder must() {
        boolQueryBuilder.must(buildQuery());
        return boolQueryBuilder;
    }

    @Override
    public ElasticBoolQueryBuilder mustNot() {
        boolQueryBuilder.mustNot(buildQuery());
        return boolQueryBuilder;
    }

    @Override
    public ElasticBoolQueryBuilder should() {
        boolQueryBuilder.should(buildQuery());
        return boolQueryBuilder;
    }

    @Override
    public ElasticBoolQueryBuilder filter() {
        boolQueryBuilder.filter(buildQuery());
        return boolQueryBuilder;
    }

    void setBoolQueryBuilder(ElasticBoolQueryBuilder boolQueryBuilder) {
        this.boolQueryBuilder = boolQueryBuilder;
    }

    @Override
    protected Query buildQuery() {
        return Query.of(q -> q.matchAll(ma -> ma));
    }
}
