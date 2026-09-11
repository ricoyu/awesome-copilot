package com.awesomecopilot.orm.it.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 逻辑删除字段为 int 类型的实体, 用于验证 deleteByPKBulk 写"已删除"标记时
 * 按字段实际类型取值(int → 1), 而不是旧代码硬编码的 true(类型错配)
 *
 * @author Rico Yu
 */
@Entity
@Table(name = "product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "NAME")
	private String name;

	/** 0=正常, 1=已删除 */
	@Column(name = "DELETED")
	private Integer deleted;

	public Product() {
	}

	public Product(String name, Integer deleted) {
		this.name = name;
		this.deleted = deleted;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getDeleted() {
		return deleted;
	}

	public void setDeleted(Integer deleted) {
		this.deleted = deleted;
	}
}
