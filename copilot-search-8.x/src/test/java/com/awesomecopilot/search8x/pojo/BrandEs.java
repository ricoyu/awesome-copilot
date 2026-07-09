package com.awesomecopilot.search8x.pojo;

import lombok.Data;

/**
 * 品牌子对象，对应mapping中brand(object)
 */
@Data
public class BrandEs {

    /** 品牌ID，keyword */
    private String brandId;

    /** 品牌名称，ik_smart分词 */
    private String brandName;

    /** 品牌地址，keyword精确匹配 */
    private String brandAddr;
}