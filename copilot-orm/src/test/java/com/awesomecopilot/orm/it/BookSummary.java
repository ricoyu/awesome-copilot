package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.BookStatus;

import java.math.BigDecimal;

/**
 * 原生 SQL 结果映射测试用的 POJO(不是 JPA 实体)。
 * <p>
 * 字段命名与 book 表列名故意不完全一致, 用来验证:
 * <ul>
 *   <li>snake_case 列名(book_name)到驼峰属性(bookName)的自动映射</li>
 *   <li>BigDecimal 列(price)到 int 属性的类型转换</li>
 *   <li>字符串状态列(status)到枚举属性(status)的按 name 映射</li>
 * </ul>
 *
 * @author Rico Yu
 */
public class BookSummary {

	/** 对应 book_name 列, 验证下划线命名自动转驼峰 */
	private String bookName;

	private String author;

	/** 对应 price 列(BigDecimal), 验证到 int 的类型转换 */
	private int price;

	/** 对应 status 列(字符串 "PUBLISHED" 等), 验证到枚举的映射 */
	private BookStatus status;

	public String getBookName() {
		return bookName;
	}

	public void setBookName(String bookName) {
		this.bookName = bookName;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public BookStatus getStatus() {
		return status;
	}

	public void setStatus(BookStatus status) {
		this.status = status;
	}
}
