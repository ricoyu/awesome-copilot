package com.awesomecopilot.search8x.builder.query;

import java.util.List;

/**
 * Bool查询中Range查询的链式调用接口
 */
public interface BoolRangeQuery extends BoolQuery {
    BoolRangeQuery field(String field);
    BoolRangeQuery gte(Object gte);
    BoolRangeQuery lte(Object lte);
    BoolRangeQuery gt(Object gt);
    BoolRangeQuery lt(Object lt);
    
    // 执行方法
    <T> List<T> queryForList();
    <T> T queryForObject();
}
