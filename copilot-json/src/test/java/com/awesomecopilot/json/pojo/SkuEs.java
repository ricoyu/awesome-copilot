package com.awesomecopilot.json.pojo;

import lombok.Data;

/**
 * SKU嵌套子文档，对应mapping sku_list(nested)
 */
@Data
public class SkuEs {

    /** SKU唯一ID，keyword */
    private String skuId;

    /** 规格名称，ik_smart分词 */
    private String specName;

    /** SKU单品价格，double */
    private Double skuPrice;

    /** SKU单品库存，整型 */
    private Integer skuStock;

    /** 颜色属性，keyword */
    private String color;
}