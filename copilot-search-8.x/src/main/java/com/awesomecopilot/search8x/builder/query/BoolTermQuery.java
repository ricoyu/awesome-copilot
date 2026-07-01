package com.awesomecopilot.search8x.builder.query;

import java.util.List;

/**
 * Bool查询中Term查询的链式调用接口
 */
public interface BoolTermQuery extends BoolQuery {
    BoolTermQuery query(String field, Object value);
    BoolTermQuery boost(float boost);
    BoolTermQuery constantScore(boolean constantScore);
    BoolTermQuery nestedPath(String path);
    
    // 执行方法
    <T> List<T> queryForList();
    <T> T queryForObject();
}
