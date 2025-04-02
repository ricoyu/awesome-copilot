package com.awesomecopilot.common.lang.vo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * 排序
 * <p>
 * Copyright: Copyright (c) 2019-10-14 15:59
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class OrderBean implements Serializable {

	public OrderBean(){}

	public OrderBean(String orderBy, DIRECTION direction) {
		this.orderBy = orderBy;
		this.direction = direction;
	}

	private static final Logger log = LoggerFactory.getLogger(OrderBean.class);

	private static final long serialVersionUID = 1L;

	/**
	 * 默认的排序的数据库字段名
	 */
	private String orderBy = "create_time";

	/**
	 * 正序还是倒序
	 */
	private DIRECTION direction = DIRECTION.DESC;

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public DIRECTION getDirection() {
		return direction;
	}

	public void setDirection(DIRECTION direction) {
		this.direction = direction;
	}

	public enum DIRECTION {
		/**
		 * 升序
		 */
		ASC,

		/**
		 * 降序
		 */
		DESC;

		public static DIRECTION of(String direction) {
			try {
				return DIRECTION.valueOf(direction.toUpperCase());
			} catch (IllegalArgumentException e) {
				log.error("将 {} 转成DIRECTION枚举报错", direction, e);
				return DIRECTION.ASC;
			}
		}
	}
}
