package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Query String Query Builder for ES 8.x
 * <p>
 * 支持 Lucene 查询语法
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticQueryStringBuilder extends BaseQueryBuilder implements BoolQuery {

    private String queryString;
    private String defaultField;
    private List<String> fields = new ArrayList<>();
    private Operator defaultOperator;
    private Boolean allowLeadingWildcard;
    private String fuzziness;
    private ElasticBoolQueryBuilder boolQueryBuilder;

    public ElasticQueryStringBuilder(String... indices) {
        super(indices);
    }

    public ElasticQueryStringBuilder query(String queryString) {
        this.queryString = queryString;
        return this;
    }

    public ElasticQueryStringBuilder defaultField(String defaultField) {
        this.defaultField = defaultField;
        return this;
    }

    public ElasticQueryStringBuilder fields(String... fields) {
        this.fields.addAll(Arrays.asList(fields));
        return this;
    }

    public ElasticQueryStringBuilder defaultOperator(Operator operator) {
        this.defaultOperator = operator;
        return this;
    }

    /**
     * 设置是否允许通配符出现在查询词的开头
     * 对应ES query_string中的 allow_leading_wildcard 参数
     * 默认true, 生产建议关闭
     *
     * @param allowLeadingWildcard 是否允许前置通配符
     * @return this
     */
    public ElasticQueryStringBuilder allowLeadingWildcard(Boolean allowLeadingWildcard) {
        this.allowLeadingWildcard = allowLeadingWildcard;
        return this;
    }

    /**
     * 设置模糊匹配的编辑距离
     * 对应ES query_string中的 fuzziness 参数
     *
     * @param fuzziness 模糊匹配度，如 "1", "2", "AUTO"
     * @return this
     */
    public ElasticQueryStringBuilder fuzziness(String fuzziness) {
        this.fuzziness = fuzziness;
        return this;
    }

    /**
     * 设置模糊匹配的编辑距离
     * 对应ES query_string中的 fuzziness 参数
     *
     * @param fuzziness 模糊匹配度，如 "1", "2", "AUTO"
     * @return this
     */
    public ElasticQueryStringBuilder fuzziness(int fuzziness) {
        return this.fuzziness(String.valueOf(fuzziness));
    }

    public ElasticQueryStringBuilder includeSources(String... fields) {
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
        return Query.of(q -> q.queryString(qs -> {
            qs.query(queryString);
            if (defaultField != null) {
                qs.defaultField(defaultField);
            }
            if (!fields.isEmpty()) {
                qs.fields(fields);
            }
            if (defaultOperator != null) {
                qs.defaultOperator(defaultOperator);
            }
            if (allowLeadingWildcard != null) {
                qs.allowLeadingWildcard(allowLeadingWildcard);
            }
            if (fuzziness != null) {
                qs.fuzziness(fuzziness);
            }
            return qs;
        }));
    }
}
