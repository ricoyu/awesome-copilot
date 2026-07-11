package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import java.util.List;

/**
 * Bool查询中MultiMatch查询的链式调用接口
 */
public interface BoolMultiMatchQuery extends BoolQuery {
    BoolMultiMatchQuery query(String queryText, String... fields);
    BoolMultiMatchQuery type(TextQueryType type);
    BoolMultiMatchQuery tieBreaker(double tieBreaker);
    BoolMultiMatchQuery operator(Operator operator);
    BoolMultiMatchQuery minimumShouldMatch(int minimumShouldMatch);
    BoolMultiMatchQuery minimumShouldMatch(String minimumShouldMatch);
    BoolMultiMatchQuery boost(float boost);
    BoolMultiMatchQuery nestedPath(String path);
    
    // 执行方法
    <T> List<T> queryForList();
    <T> T queryForObject();
}
