package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Range Query Builder for ES 8.x
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticRangeQueryBuilder extends BaseQueryBuilder implements BoolRangeQuery {

    private String nestedPath;
    private Object gte;
    private Object lte;
    private Object gt;
    private Object lt;
    private ElasticBoolQueryBuilder boolQueryBuilder;

    public ElasticRangeQueryBuilder() {
    }

    public ElasticRangeQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticRangeQueryBuilder nestedPath(String path) {
        this.nestedPath = path;
        return this;
    }

    @Override
    public ElasticRangeQueryBuilder field(String field) {
        this.field = field;
        return this;
    }

    @Override
    public ElasticRangeQueryBuilder gte(Object gte) {
        this.gte = gte;
        return this;
    }

    @Override
    public ElasticRangeQueryBuilder lte(Object lte) {
        this.lte = lte;
        return this;
    }

    @Override
    public ElasticRangeQueryBuilder gt(Object gt) {
        this.gt = gt;
        return this;
    }

    @Override
    public ElasticRangeQueryBuilder lt(Object lt) {
        this.lt = lt;
        return this;
    }

    public ElasticRangeQueryBuilder constantScore(boolean constantScore) {
        this.constantScore = constantScore;
        return this;
    }

    public ElasticRangeQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
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
        Query query = Query.of(q -> q.range(r -> {
            if (gte instanceof Number || lte instanceof Number || gt instanceof Number || lt instanceof Number) {
                r.number(n -> {
                    n.field(field);
                    if (gte != null) n.gte(((Number) gte).doubleValue());
                    if (gt != null) n.gt(((Number) gt).doubleValue());
                    if (lte != null) n.lte(((Number) lte).doubleValue());
                    if (lt != null) n.lt(((Number) lt).doubleValue());
                    return n;
                });
            } else {
                r.untyped(u -> {
                    u.field(field);
                    if (gte != null) u.gte(co.elastic.clients.json.JsonData.of(gte));
                    if (gt != null) u.gt(co.elastic.clients.json.JsonData.of(gt));
                    if (lte != null) u.lte(co.elastic.clients.json.JsonData.of(lte));
                    if (lt != null) u.lt(co.elastic.clients.json.JsonData.of(lt));
                    return u;
                });
            }
            return r;
        }));

        if (isNotBlank(nestedPath)) {
            final Query innerQuery = query;
            query = Query.of(q -> q.nested(n -> n.path(nestedPath).query(innerQuery).scoreMode(ChildScoreMode.Avg)));
        }
        return query;
    }
}
