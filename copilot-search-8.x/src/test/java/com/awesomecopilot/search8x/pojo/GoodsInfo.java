package com.awesomecopilot.search8x.pojo;

import com.awesomecopilot.search8x.annotation.DocId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * ES goods_info 索引实体
 */
@Data
public class GoodsInfo {

    /**
     * 文档id
     */
    @DocId
    private Long id;

    /**
     * 商品名称
     * analyzer: ik_max_word
     * search_analyzer: ik_smart
     */
    @JsonProperty("goods_name")
    private String goodsName;

    /**
     * 商品描述
     * analyzer: ik_max_word
     * search_analyzer: ik_smart
     */
    @JsonProperty("goods_desc")
    private String goodsDesc;
}