package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Match Phrase Prefix Query Builder for ES 8.x
 * <p>
 * 匹配一个短语前缀, 比如查description包含"高性能智"这个短语前缀,
 * 那么description是"高性能智能手机"可以查到, "高性能智慧设备"也可以查到
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticMatchPhrasePrefixQueryBuilder extends BaseQueryBuilder {

    /**
     * 限制 最后一个词的前缀可扩展成多少个实际词项(token)
     */
    private Integer maxExpansions;

    public ElasticMatchPhrasePrefixQueryBuilder(String... indices) {
        super(indices);
    }

    /**
     * 设置查询字段和值
     *
     * @param field 字段名
     * @param value 查询值
     * @return ElasticMatchPhrasePrefixQueryBuilder
     */
    public ElasticMatchPhrasePrefixQueryBuilder query(String field, Object value) {
        this.field = field;
        this.value = value;
        return this;
    }

    /**
     * 限制 最后一个词的前缀可扩展成多少个实际词项(token)
     * 默认值是 50, 可以设置为更小的值来控制开销
     *
     * @param maxExpansions
     * @return ElasticMatchPhrasePrefixQueryBuilder
     */
    public ElasticMatchPhrasePrefixQueryBuilder maxExpansions(Integer maxExpansions) {
        this.maxExpansions = maxExpansions;
        return this;
    }

    public ElasticMatchPhrasePrefixQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    public ElasticMatchPhrasePrefixQueryBuilder constantScore(boolean constantScore) {
        this.constantScore = constantScore;
        return this;
    }

    public ElasticMatchPhrasePrefixQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticMatchPhrasePrefixQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    public ElasticMatchPhrasePrefixQueryBuilder resultType(Class resultType) {
        this.resultType = resultType;
        return this;
    }

    @Override
    protected Query buildQuery() {
        return Query.of(q -> q.matchPhrasePrefix(mpp -> {
            mpp.field(field).query(value.toString());
            if (maxExpansions != null) {
                mpp.maxExpansions(maxExpansions);
            }
            return mpp;
        }));
    }
}
