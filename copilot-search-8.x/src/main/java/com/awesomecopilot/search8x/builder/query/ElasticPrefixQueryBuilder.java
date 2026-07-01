package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Prefix Query Builder for ES 8.x
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public final class ElasticPrefixQueryBuilder extends BaseQueryBuilder {

    public ElasticPrefixQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticPrefixQueryBuilder query(String field, String value) {
        this.field = field;
        this.value = value;
        return this;
    }

    public ElasticPrefixQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    public ElasticPrefixQueryBuilder constantScore(boolean constantScore) {
        this.constantScore = constantScore;
        return this;
    }

    public ElasticPrefixQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticPrefixQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    @Override
    protected Query buildQuery() {
        return Query.of(q -> q.prefix(p -> {
            p.field(field).value(value == null ? "" : value.toString());
            if (boost != 1f) {
                p.boost(boost);
            }
            return p;
        }));
    }
}
