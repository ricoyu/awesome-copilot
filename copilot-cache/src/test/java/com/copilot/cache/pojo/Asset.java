package com.copilot.cache.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Copyright: (C), 2021-08-31 16:52
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
	
	private String ip;
	
	/**
	 * 资产组
	 */
	private String group;
	
	/**
	 * 资产名称
	 */
	private String name;
	
	/**
	 * 资产负责人
	 */
	private String owner;
	
	/**
	 * 资产业务系统
	 */
	@JsonProperty("business_system")
	private String businessSystem;
	
	/**
	 * 资产所属部门
	 */
	private String dapartment;
	
	/**
	 * 逻辑地址
	 */
	@JsonProperty("logical_address")
	private String logicalAddress;
	
	/**
	 * 物理地址
	 */
	@JsonProperty("physical_address")
	private String physicalAddress;
	
	/**
	 * 操作系统
	 */
	@JsonProperty("operating_system")
	private String operatingSystem;
	
	/**
	 * 厂家
	 */
	private String manufacture;
	
	/**
	 * 设备型号
	 */
	@JsonProperty("device_model")
	private String deviceModel;
	
	/**
	 * 资产注册时间
	 */
	@JsonProperty("register_date")
	private String registerDate;

	/**
	 * 资产编号
	 */
	private String label;
	
	/**
	 * 备注
	 */
	private String remark;
}
