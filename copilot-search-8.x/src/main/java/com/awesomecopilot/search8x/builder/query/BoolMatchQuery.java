package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;

import java.util.List;

/**
 * Bool查询中Match查询的链式调用接口
 */
public interface BoolMatchQuery extends BoolQuery {
    BoolMatchQuery query(String field, Object value);
    BoolMatchQuery operator(Operator operator);
    BoolMatchQuery minimumShouldMatch(int minimumShouldMatch);
    BoolMatchQuery minimumShouldMatch(String minimumShouldMatch);
    BoolMatchQuery boost(float boost);
    BoolMatchQuery nestedPath(String path);
    
    // 执行方法
    <T> List<T> queryForList();
    <T> T queryForObject();
}
