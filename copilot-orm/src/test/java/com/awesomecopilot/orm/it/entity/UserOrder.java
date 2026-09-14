package com.awesomecopilot.orm.it.entity;

import com.awesomecopilot.orm.entity.BaseEntityShowflake;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 集成测试用实体, 继承 BaseEntityShowflake(主键用 CopilotSnowflakeIdGenerator 生成),
 * 用来验证雪花ID主键生成器在真实 Hibernate persist 链路里工作正常。
 * 表名避开 H2 的保留字 order, 用 user_order。
 *
 * @author Rico Yu
 */
@Entity
@Table(name = "user_order")
public class UserOrder extends BaseEntityShowflake {

	@Column(name = "ORDER_NO")
	private String orderNo;

	@Column(name = "AMOUNT")
	private BigDecimal amount;

	public UserOrder() {
	}

	public UserOrder(String orderNo, BigDecimal amount) {
		this.orderNo = orderNo;
		this.amount = amount;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
}
