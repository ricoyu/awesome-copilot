package com.awesomecopilot.orm.it.entity;

import com.awesomecopilot.orm.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 集成测试用实体: 一对多的"多"方，挂在 {@link Shelf} 下。
 *
 * @author Rico Yu
 */
@Entity
@Table(name = "shelf_item")
public class ShelfItem extends BaseEntity {

	@Column(name = "NAME")
	private String name;

	@ManyToOne
	@JoinColumn(name = "SHELF_ID")
	private Shelf shelf;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Shelf getShelf() {
		return shelf;
	}

	public void setShelf(Shelf shelf) {
		this.shelf = shelf;
	}
}
