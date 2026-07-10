package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Exists Query Builder for ES 8.x
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticExistsQueryBuilder extends BaseQueryBuilder implements BoolQuery {

    private String nestedPath;
    private ElasticBoolQueryBuilder boolQueryBuilder;

    public ElasticExistsQueryBuilder() {
    }

    public ElasticExistsQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticExistsQueryBuilder field(String field) {
        this.field = field;
        return this;
    }

    public ElasticExistsQueryBuilder nestedPath(String path) {
        this.nestedPath = path;
        return this;
    }

    public ElasticExistsQueryBuilder constantScore(boolean constantScore) {
        this.constantScore = constantScore;
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
        Query query = Query.of(q -> q.exists(e -> e.field(field)));

        if (isNotBlank(nestedPath)) {
            final Query innerQuery = query;
            query = Query.of(q -> q.nested(n -> n.path(nestedPath).query(innerQuery).scoreMode(ChildScoreMode.Avg)));
        }
        return query;
    }
}
