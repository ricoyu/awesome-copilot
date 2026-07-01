package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Match Phrase Query Builder for ES 8.x
 * <p>
 * 匹配一个短语, 比如查title="one love", 那么title是"the one love"可以搜到, "one I love"搜不到
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticMatchPhraseQueryBuilder extends BaseQueryBuilder {

    private Integer slop;

    public ElasticMatchPhraseQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticMatchPhraseQueryBuilder query(String field, Object value) {
        this.field = field;
        this.value = value;
        return this;
    }

    public ElasticMatchPhraseQueryBuilder slop(Integer slop) {
        this.slop = slop;
        return this;
    }

    public ElasticMatchPhraseQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    public ElasticMatchPhraseQueryBuilder constantScore(boolean constantScore) {
        this.constantScore = constantScore;
        return this;
    }

    public ElasticMatchPhraseQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    public ElasticMatchPhraseQueryBuilder excludeSources(String... fields) {
        this.excludeSource = fields;
        return this;
    }

    @Override
    protected Query buildQuery() {
        return Query.of(q -> q.matchPhrase(mp -> {
            mp.field(field).query(value.toString());
            if (slop != null) {
                mp.slop(slop);
            }
            return mp;
        }));
    }
}
