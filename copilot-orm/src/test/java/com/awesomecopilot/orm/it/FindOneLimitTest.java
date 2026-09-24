package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * findOne 行为回归（评审报告 M-4, 第一片: SQL 必须带行数限制）。
 * <p>
 * 原缺陷：CriteriaQueryBuilder.findOne / JpaDao.findOne(Class,prop,value) 都是
 * 全量 list() 再 get(0)——命中 N 条就把 N 条全部加载进内存再丢弃。
 * 本类断言 findOne 生成的 SQL 带 "fetch first" 行数限制(Hibernate 对 maxResults=1
 * 的 H2 方言渲染), 且取第一条的旧语义不变。
 * findUnique(多条抛异常)的用例在下一片补充。
 * <p/>
 * Copyright: Copyright (c) 2026-09-24
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
class FindOneLimitTest extends AbstractOrmIntegrationTest {

	private void seedTwoSameNameBooks() {
		// 走 seedEm 独立事务提交, 保证 JpaDao 另开的 EM 连接能看到数据
		seedEm.getTransaction().begin();
		insertBook("重复书名", "张三", "50.00", 10, true,
				java.time.LocalDateTime.now(), "PUBLISHED");
		insertBook("重复书名", "张三", "50.00", 10, true,
				java.time.LocalDateTime.now(), "PUBLISHED");
		insertBook("唯一书名", "张三", "50.00", 10, true,
				java.time.LocalDateTime.now(), "PUBLISHED");
		seedEm.getTransaction().commit();
	}

	@Test
	@DisplayName("findOne 旧语义: 多条时返回第一条且不报错, 无命中返回 null")
	void findOneTakesFirstRowWithoutError() {
		seedTwoSameNameBooks();
		Book first = jpaDao.query(Book.class).eq("name", "重复书名").findOne();
		assertNotNull(first, "两条同名时应返回第一条(旧语义保留)");
		assertNull(jpaDao.query(Book.class).eq("name", "不存在").findOne(), "无命中返回 null");
	}

	@Test
	@DisplayName("CriteriaQueryBuilder.findOne 的 SQL 必须带 1 行限制, 不再全量拉取")
	void findOneLimitsRowsInSql() {
		seedTwoSameNameBooks();
		SqlProbe.reset();
		SqlProbe.enabled = true;
		try {
			jpaDao.query(Book.class).eq("name", "重复书名").findOne();
		} finally {
			SqlProbe.enabled = false;
		}
		assertEquals(true, SqlProbe.matches("fetch first"),
				"findOne 应生成带行数限制的 SQL(fetch first), 实际 SQL:\n" + SqlProbe.dump());
	}

	@Test
	@DisplayName("JpaDao.findOne(Class,prop,value) 同样带 1 行限制")
	void jpaDaoFindOneLimitsRows() {
		seedTwoSameNameBooks();
		SqlProbe.reset();
		SqlProbe.enabled = true;
		try {
			jpaDao.findOne(Book.class, "name", "重复书名");
		} finally {
			SqlProbe.enabled = false;
		}
		assertEquals(true, SqlProbe.matches("fetch first"),
				"JpaDao.findOne 应生成带行数限制的 SQL, 实际 SQL:\n" + SqlProbe.dump());
	}

	@Test
	@DisplayName("includeDeleted 逃生门版 findOne 也带 1 行限制")
	void includeDeletedFindOneLimitsRows() {
		seedTwoSameNameBooks();
		SqlProbe.reset();
		SqlProbe.enabled = true;
		try {
			jpaDao.findOne(Book.class, "name", "重复书名", true);
		} finally {
			SqlProbe.enabled = false;
		}
		assertEquals(true, SqlProbe.matches("fetch first"),
				"逃生门版 findOne 也应带行数限制, 实际 SQL:\n" + SqlProbe.dump());
	}
}
