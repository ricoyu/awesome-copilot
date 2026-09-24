package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import jakarta.persistence.NonUniqueResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * findUnique 行为回归（评审报告 M-4, 第二片: 唯一性显式校验）。
 * <p>
 * findOne 保留"多条取第一条"的旧兼容语义(M-4 第一片已给它加了 1 行 SQL 限制),
 * 本类验证新增的 findUnique 变体: 命中多条抛 NonUniqueResultException,
 * 把"唯一约束失效"从'不出声地取第一条'改成显式报错, 供业务上"按唯一键查"场景使用。
 * <p/>
 * Copyright: Copyright (c) 2026-09-24
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
class FindUniqueTest extends AbstractOrmIntegrationTest {

	private void seedBooks() {
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
	@DisplayName("CriteriaQueryBuilder.findUnique: 命中多条抛 NonUniqueResultException")
	void findUniqueThrowsWhenMultiple() {
		seedBooks();
		assertThrows(NonUniqueResultException.class,
				() -> jpaDao.query(Book.class).eq("name", "重复书名").findUnique(),
				"条件不唯一时必须报错, 不许悄悄取第一条");
	}

	@Test
	@DisplayName("CriteriaQueryBuilder.findUnique: 恰好一条返回实体, 零条返回 null")
	void findUniqueReturnsSingleOrNull() {
		seedBooks();
		Book only = jpaDao.query(Book.class).eq("name", "唯一书名").findUnique();
		assertNotNull(only);
		assertEquals("唯一书名", only.getName());
		assertNull(jpaDao.query(Book.class).eq("name", "不存在").findUnique());
	}

	@Test
	@DisplayName("JpaDao.findUnique(Class,prop,value): 多条抛异常, 一条返回")
	void jpaDaoFindUnique() {
		seedBooks();
		assertThrows(NonUniqueResultException.class,
				() -> jpaDao.findUnique(Book.class, "name", "重复书名"));
		Book only = jpaDao.findUnique(Book.class, "name", "唯一书名");
		assertNotNull(only);
	}

	@Test
	@DisplayName("findUnique 的 SQL 最多取 2 行(判断唯一不需要全量)")
	void findUniqueLimitsRows() {
		seedBooks();
		SqlProbe.reset();
		SqlProbe.enabled = true;
		try {
			jpaDao.query(Book.class).eq("name", "重复书名").findUnique();
		} catch (NonUniqueResultException expected) {
			// 预期报错
		} finally {
			SqlProbe.enabled = false;
		}
		assertTrue(SqlProbe.matches("fetch first"),
				"findUnique 应带行数限制, 实际 SQL:\n" + SqlProbe.dump());
	}
}
