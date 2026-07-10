package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Terms Query Builder for ES 8.x
 * <p>
 * Terms查询用于匹配多个精确值中的任意一个, 相当于 SQL 中的 IN 操作
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticTermsQueryBuilder extends BaseQueryBuilder implements BoolQuery {

    private String nestedPath;
    private Object[] values;
    private ElasticBoolQueryBuilder boolQueryBuilder;

    public ElasticTermsQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticTermsQueryBuilder() {
    }

    public ElasticTermsQueryBuilder query(String field, Object... values) {
        this.field = field;
        this.values = values;
        return this;
    }

    public ElasticTermsQueryBuilder nestedPath(String path) {
        this.nestedPath = path;
        return this;
    }

    public ElasticTermsQueryBuilder includeSources(String... fields) {
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
        List<FieldValue> fieldValues = new ArrayList<>();
        if (values != null) {
            for (Object v : values) {
                if (v instanceof String) {
                    fieldValues.add(FieldValue.of((String) v));
                } else if (v instanceof Integer) {
                    fieldValues.add(FieldValue.of((Integer) v));
                } else if (v instanceof Long) {
                    fieldValues.add(FieldValue.of((Long) v));
                } else if (v instanceof Double) {
                    fieldValues.add(FieldValue.of((Double) v));
                } else if (v instanceof Float) {
                    fieldValues.add(FieldValue.of((Float) v));
                } else if (v instanceof Boolean) {
                    fieldValues.add(FieldValue.of((Boolean) v));
                } else {
                    fieldValues.add(FieldValue.of(v.toString()));
                }
            }
        }
        Query query = Query.of(q -> q.terms(t -> t.field(field).terms(ts -> ts.value(fieldValues))));

        if (isNotBlank(nestedPath)) {
            final Query innerQuery = query;
            query = Query.of(q -> q.nested(n -> n.path(nestedPath).query(innerQuery).scoreMode(ChildScoreMode.Avg)));
        }
        return query;
    }
}
