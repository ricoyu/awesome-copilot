package com.awesomecopilot.orm.it.entity;

import com.awesomecopilot.orm.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * 集成测试用实体: 一对多的"一"方。用来复现"主查询 join 集合产生行倍增、
 * distinct(true) 去重后列表与分页 count 不一致"的场景（评审报告 I-10）。
 *
 * @author Rico Yu
 */
@Entity
@Table(name = "shelf")
public class Shelf extends BaseEntity {

	@Column(name = "NAME")
	private String name;

	@OneToMany(mappedBy = "shelf")
	private List<ShelfItem> items = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ShelfItem> getItems() {
		return items;
	}

	public void setItems(List<ShelfItem> items) {
		this.items = items;
	}
}
