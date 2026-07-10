package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import java.util.Arrays;

/**
 * Multi Match Query Builder for ES 8.x
 * <p>
 * Multi Match Query 跨字段搜索
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticMultiMatchQueryBuilder extends BaseQueryBuilder {

    private String[] fields;
    private TextQueryType type;
    private Double tieBreaker;
    private Operator operator;
    private Object minimumShouldMatch;

    public ElasticMultiMatchQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticMultiMatchQueryBuilder query(String queryText, String... fields) {
        this.value = queryText;
        this.fields = fields;
        return this;
    }

    public ElasticMultiMatchQueryBuilder type(TextQueryType type) {
        this.type = type;
        return this;
    }

    public ElasticMultiMatchQueryBuilder tieBreaker(double tieBreaker) {
        this.tieBreaker = tieBreaker;
        return this;
    }

    public ElasticMultiMatchQueryBuilder operator(Operator operator) {
        this.operator = operator;
        return this;
    }

    public ElasticMultiMatchQueryBuilder minimumShouldMatch(String minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }

    public ElasticMultiMatchQueryBuilder minimumShouldMatch(int minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }

    public ElasticMultiMatchQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    @Override
    protected Query buildQuery() {
        return Query.of(q -> q.multiMatch(mm -> {
            mm.query(value.toString());
            if (fields != null && fields.length > 0) {
                mm.fields(Arrays.asList(fields));
            }
            if (type != null) {
                mm.type(type);
            }
            if (tieBreaker != null) {
                mm.tieBreaker(tieBreaker);
            }
            if (operator != null) {
                mm.operator(operator);
            }
            if (minimumShouldMatch != null) {
                mm.minimumShouldMatch(minimumShouldMatch.toString());
            }
            return mm;
        }));
    }
}
