package com.awesomecopilot.common.lang.vo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Locale;

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

		/**
		 * 字符串转排序方向枚举, 大小写不敏感。
		 * <p>
		 * P2-31(CODE_REVIEW_REPORT): 修复前 of("xyz") 只打一条 ERROR 就返回 ASC
		 * (不抛), 而 of(null) 抛 NullPointerException——两种非法输入两种相反策略,
		 * 前端参数拼错时查询顺序被悄悄反转且调用方无从感知。现统一: 非法输入一律抛
		 * IllegalArgumentException(PageDTO 的转换失败路径本来就在按这个契约用它)。
		 */
		public static DIRECTION of(String direction) {
			if (direction == null) {
				throw new IllegalArgumentException("direction 不能为 null, 期望值: ASC / DESC");
			}
			try {
				return DIRECTION.valueOf(direction.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("无法将 " + direction + " 转成 DIRECTION 枚举, 期望值: ASC / DESC", e);
			}
		}
	}
}
