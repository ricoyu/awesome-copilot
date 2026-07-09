package com.awesomecopilot.search8x.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * SKU嵌套子文档，对应mapping sku_list(nested)
 */
@Data
public class SkuEs {

    /** SKU唯一ID，keyword */
    @JsonProperty("sku_id")
    private String skuId;

    /** 规格名称，ik_smart分词 */
    @JsonProperty("spec_name")
    private String specName;

    /** SKU单品价格，double */
    @JsonProperty("sku_price")
    private Double skuPrice;

    /** SKU单品库存，整型 */
    @JsonProperty("sku_stock")
    private Integer skuStock;

    /** 颜色属性，keyword */
    private String color;
}