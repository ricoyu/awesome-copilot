package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Term Query Builder for ES 8.x
 * <p>
 * 在ES中, Term查询, 对输入不做分词. 会将输入作为一个整体, 在倒排索引中查找准确的词项
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public final class ElasticTermQueryBuilder extends BaseQueryBuilder implements BoolTermQuery {

    private String nestedPath;
    private ElasticBoolQueryBuilder boolQueryBuilder;

    /**
     * 默认构造器
     */
    public ElasticTermQueryBuilder() {
        super();
    }

    /**
     * 带索引名的构造器
     *
     * @param indices 索引名数组
     */
    public ElasticTermQueryBuilder(String... indices) {
        super(indices);
    }

    @Override
    public ElasticTermQueryBuilder query(String field, Object value) {
        this.field = field;
        this.value = value;
        return this;
    }

    public ElasticTermQueryBuilder nestedPath(String path) {
        this.nestedPath = path;
        return this;
    }

    @Override
    public ElasticTermQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    @Override
    public ElasticTermQueryBuilder constantScore(boolean constantScore) {
        this.constantScore = constantScore;
        return this;
    }

    public ElasticTermQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticTermQueryBuilder excludeSources(String... fields) {
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
        Query query = Query.of(q -> q.term(t -> {
            t.field(field);
            if (value instanceof String) {
                t.value((String) value);
            } else if (value instanceof Integer) {
                t.value((Integer) value);
            } else if (value instanceof Long) {
                t.value((Long) value);
            } else if (value instanceof Float) {
                t.value((Float) value);
            } else if (value instanceof Double) {
                t.value((Double) value);
            } else if (value instanceof Boolean) {
                t.value((Boolean) value);
            } else {
                t.value(value.toString());
            }
            if (boost != 1f) {
                t.boost(boost);
            }
            return t;
        }));

        if (isNotBlank(nestedPath)) {
            final Query innerQuery = query;
            query = Query.of(q -> q.nested(n -> n.path(nestedPath).query(innerQuery).scoreMode(ChildScoreMode.Avg)));
        }
        return query;
    }
}
