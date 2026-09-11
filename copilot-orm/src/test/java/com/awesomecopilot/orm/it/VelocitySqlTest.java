package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Velocity 动态 SQL 场景: 命名 SQL 模板里的 #if / #ifNotNull / #ifPresent / #between 指令,
 * 配合 SQL 自动修复(sqlAutofix), 验证条件拼装是否按参数动态生效。
 *
 * @author Rico Yu
 */
class VelocitySqlTest extends AbstractOrmIntegrationTest {

	@Test
	@DisplayName("#if($name) 参数有值时拼条件, 无值时条件被删掉")
	void ifDirective() {
		seedStandardBooks();
		// 有 name 参数: 只查这本
		List<Book> withName = jpaDao.query(
				"select * from book where 1=1 #if($name) and name = :name #end")
				.addParam("name", "深入理解JVM")
				.resultClass(Book.class)
				.findList();
		assertEquals(1, withName.size());

		// 没传 name 参数: #if 条件整段被删, 查到全部
		List<Book> withoutName = jpaDao.query(
				"select * from book where 1=1 #if($name) and name = :name #end")
				.resultClass(Book.class)
				.findList();
		assertEquals(5, withoutName.size());
	}

	@Test
	@DisplayName("#if 参数为 null 时, 悬空的 where 被 SQL 自动修复掉")
	void ifNullWithAutoFix() {
		seedStandardBooks();
		// 模板里 where 后面直接跟 #if, name 为 null 时渲染成 "where " 悬空,
		// sqlAutofix=true 应把它修成不带 where 的合法 SQL
		List<Book> result = jpaDao.query(
				"select * from book where #if($name) name = :name #end")
				.addParam("name", null)
				.resultClass(Book.class)
				.findList();
		assertEquals(5, result.size(), "name 为 null 时悬空 where 应被修复, 返回全部");
	}

	@Test
	@DisplayName("#ifNotNull 块指令: 值非 null 才渲染块内 SQL")
	void ifNotNullDirective() {
		seedStandardBooks();
		List<Book> withAuthor = jpaDao.query(
				"select * from book where 1=1 #ifNotNull($author) and author = :author #end")
				.addParam("author", "Robert")
				.resultClass(Book.class)
				.findList();
		assertEquals(1, withAuthor.size());
		assertEquals("Clean Code", withAuthor.get(0).getName());

		List<Book> withoutAuthor = jpaDao.query(
				"select * from book where 1=1 #ifNotNull($author) and author = :author #end")
				.resultClass(Book.class)
				.findList();
		assertEquals(5, withoutAuthor.size());
	}

	@Test
	@DisplayName("#ifPresent 行指令: 值非 null 输出指定片段")
	void ifPresentDirective() {
		seedStandardBooks();
		List<Book> result = jpaDao.query(
				"select * from book where 1=1 #ifPresent($minPrice, 'and price >= :minPrice')")
				.addParam("minPrice", 80)
				.resultClass(Book.class)
				.findList();
		assertEquals(3, result.size(), "price>=80 的有 88/98.5/108 三本");
	}

	@Test
	@DisplayName("#between 行指令: 生成区间条件, 参数走命名绑定")
	void betweenDirective() {
		seedStandardBooks();
		List<Book> result = jpaDao.query(
				"select * from book where 1=1 #between('price', $low, $high)")
				.addParam("low", 70)
				.addParam("high", 100)
				.resultClass(Book.class)
				.findList();
		assertEquals(3, result.size(), "price 在 [70,100] 的: 98.5/88/79 三本");
	}

	@Test
	@DisplayName("#between 只给下界时生成 >= 条件")
	void betweenDirectiveLowerOnly() {
		seedStandardBooks();
		List<Book> result = jpaDao.query(
				"select * from book where 1=1 #between('price', $low, $high)")
				.addParam("low", 88)
				.addParam("high", null)
				.resultClass(Book.class)
				.findList();
		assertEquals(3, result.size(), "price>=88 的: 98.5/88/108 三本");
	}

	@Test
	@DisplayName("多个动态条件组合: 传几个拼几个")
	void combinedDynamicConditions() {
		seedStandardBooks();
		Map<String, Object> params = new HashMap<>();
		params.put("minPrice", 80);
		params.put("published", true);
		List<Book> result = jpaDao.query(
				"select * from book where 1=1 "
						+ "#if($minPrice) and price >= :minPrice #end "
						+ "#if($published) and published = :published #end")
				.addParams(params)
				.resultClass(Book.class)
				.findList();
		assertEquals(3, result.size(), "published 且 price>=80: 98.5/88/108 三本");
	}
}
