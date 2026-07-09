package com.awesomecopilot.search8x.pojo;

import com.awesomecopilot.search8x.annotation.DocId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品ES文档实体
 * 对应索引mapping根结构
 */
@Data
@ToString
public class GoodsEs {
    
    @DocId
    private String id;
    
    /** 商品ID，keyword精确检索 */
    private String goodsId;

    /** 商品名称，ik_max分词存储，ik_smart检索 */
    private String goodsName;

    /** 商品描述，ik_max分词存储，ik_smart检索 */
    private String goodsDesc;

    /** 商品分类，keyword */
    private String category;

    /** 商品售价，double类型 */
    private Double price;

    /** 商品总库存，整型 */
    private Integer stock;

    /** 商品销量，整型 */
    private Integer sales;

    /** 是否上架，布尔值 */
    private Boolean online;

    /**
     * 发布时间
     * ES格式：yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishingTime;

    /** 商品标签数组，keyword多值字段 */
    private List<String> tags;

    /** 品牌信息，普通object对象 */
    private BrandEs brand;

    /** SKU规格列表，nested嵌套类型 */
    private List<SkuEs> skuList;
}