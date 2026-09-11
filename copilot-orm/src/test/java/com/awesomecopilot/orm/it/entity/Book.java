package com.awesomecopilot.orm.it.entity;

import com.awesomecopilot.orm.converter.StringListConverter;
import com.awesomecopilot.orm.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 集成测试用实体, 字段类型尽量齐全, 用来覆盖 DAO 各查询场景:
 * 字符串 / 数值 / 布尔 / 时间 / 枚举 / 列表(带 AttributeConverter)
 *
 * @author Rico Yu
 */
@Entity
@Table(name = "book")
@NamedNativeQueries({
		@NamedNativeQuery(name = "Book.findByExactName",
				query = "select * from book where name = :name"),
		@NamedNativeQuery(name = "Book.findCheapOrPopular",
				query = "select * from book where price < :maxPrice or stock > :minStock")
})
public class Book extends BaseEntity {

	@Column(name = "NAME")
	private String name;

	@Column(name = "AUTHOR")
	private String author;

	@Column(name = "PRICE")
	private BigDecimal price;

	@Column(name = "STOCK")
	private Integer stock;

	@Column(name = "PUBLISHED")
	private Boolean published;

	@Column(name = "PUBLISH_DATE")
	private LocalDateTime publishDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS")
	private BookStatus status;

	@Convert(converter = StringListConverter.class)
	@Column(name = "TAGS")
	private List<String> tags;

	@Column(name = "DELETED", columnDefinition = "BOOLEAN DEFAULT false")
	private Boolean deleted;

	@Column(name = "ORDER_NO")
	private String orderNo;

	public Book() {
	}

	public Book(String name, String author, BigDecimal price, Integer stock,
	            Boolean published, LocalDateTime publishDate, BookStatus status) {
		this.name = name;
		this.author = author;
		this.price = price;
		this.stock = stock;
		this.published = published;
		this.publishDate = publishDate;
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public Boolean getPublished() {
		return published;
	}

	public void setPublished(Boolean published) {
		this.published = published;
	}

	public LocalDateTime getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(LocalDateTime publishDate) {
		this.publishDate = publishDate;
	}

	public BookStatus getStatus() {
		return status;
	}

	public void setStatus(BookStatus status) {
		this.status = status;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}
}
