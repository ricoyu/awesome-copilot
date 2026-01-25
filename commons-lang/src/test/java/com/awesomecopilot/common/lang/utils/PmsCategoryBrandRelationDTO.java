package com.awesomecopilot.common.lang.utils;

import lombok.Data;

@Data
public class PmsCategoryBrandRelationDTO {

	private Long id;

	private Long brandId;

	private Long catelogId;

	private String brandName;

	private String catelogName;
}