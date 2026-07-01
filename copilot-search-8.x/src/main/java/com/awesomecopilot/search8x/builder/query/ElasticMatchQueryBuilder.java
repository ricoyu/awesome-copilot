package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Match Query Builder for ES 8.x
 * <p>
 * Match Query是会对搜索的内容做分词后再去ES中查询的
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticMatchQueryBuilder extends BaseQueryBuilder implements BoolMatchQuery {

    private String nestedPath;
    private Operator operator;
    private Object minimumShouldMatch;
    private ElasticBoolQueryBuilder boolQueryBuilder;

    public ElasticMatchQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticMatchQueryBuilder nestedPath(String path) {
        this.nestedPath = path;
        return this;
    }

    @Override
    public ElasticMatchQueryBuilder query(String field, Object value) {
        this.field = field;
        this.value = value;
        return this;
    }

    @Override
    public ElasticMatchQueryBuilder operator(Operator operator) {
        this.operator = operator;
        return this;
    }

    @Override
    public ElasticMatchQueryBuilder minimumShouldMatch(int minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }

    @Override
    public ElasticMatchQueryBuilder minimumShouldMatch(String minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }

    public ElasticMatchQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    public ElasticMatchQueryBuilder constantScore(boolean constantScore) {
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
        Query query;
        if (isNotBlank(field)) {
            query = Query.of(q -> q.match(m -> {
                m.field(field).query(value.toString());
                if (operator != null) {
                    m.operator(operator);
                }
                if (minimumShouldMatch != null) {
                    m.minimumShouldMatch(minimumShouldMatch.toString());
                }
                if (boost != 1f) {
                    m.boost(boost);
                }
                return m;
            }));
        } else {
            query = Query.of(q -> q.matchAll(ma -> ma));
        }

        if (isNotBlank(nestedPath)) {
            final Query innerQuery = query;
            query = Query.of(q -> q.nested(n -> n.path(nestedPath).query(innerQuery).scoreMode(ChildScoreMode.Avg)));
        }
        return query;
    }
}
