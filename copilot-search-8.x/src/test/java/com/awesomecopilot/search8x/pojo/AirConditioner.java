package com.awesomecopilot.search8x.pojo;

import com.awesomecopilot.search8x.annotation.DocId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class AirConditioner {
    @DocId
    private Long id;
    
    @JsonProperty("goods_name")
    private String goodsName;
    
    @JsonProperty("goods_desc")
    private String goodsDesc;
    private String brand;
    private List<SpecNested> specs;

    @Data
    public static class SpecNested {
        private String color;
        
        @JsonProperty("horse_power")
        private String horsePower;
        
        @JsonProperty("sale_price")
        private Double salePrice;
        private Integer stock;
        
        @JsonProperty("energy_level")
        private String energyLevel;
    }
}