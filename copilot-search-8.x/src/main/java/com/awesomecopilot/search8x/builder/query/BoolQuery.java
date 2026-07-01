package com.awesomecopilot.search8x.builder.query;

/**
 * Bool查询链式调用接口, 子查询builder实现此接口后可以通过 must()/mustNot()/should()/filter() 
 * 将查询条件添加到BoolQueryBuilder并返回BoolQueryBuilder
 * <p>
 * Copyright: (C), 2021-06-13 15:35
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public interface BoolQuery {

    /**
     * 条件必须满足, 且计入算分
     * @return ElasticBoolQueryBuilder
     */
    ElasticBoolQueryBuilder must();

    /**
     * 条件必须不满足, 不计入算分
     * @return ElasticBoolQueryBuilder
     */
    ElasticBoolQueryBuilder mustNot();

    /**
     * 应该满足, 计入算分
     * @return ElasticBoolQueryBuilder
     */
    ElasticBoolQueryBuilder should();

    /**
     * 条件必须满足, 不计入算分
     * @return ElasticBoolQueryBuilder
     */
    ElasticBoolQueryBuilder filter();
}
