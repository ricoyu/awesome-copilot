package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.BookStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * merge/save 批量路径的 SELECT 次数与持久化上下文回归（大数据量报告 P1-9）。
 * <p>
 * merge 语义要求"按主键读一次再拷贝状态"，单条恰好 1 次 SELECT 不可避免；
 * 旧实现的缺陷在批量路径：merge(List) 逐条 em.merge()，200 条 = 200 次
 * 单行 SELECT(N+1)，且全程不 detach；save(List) 新增路径每批只 flush 不
 * detach——受管实例堆满持久化上下文。
 * <p>
 * 目标行为：
 * ① 单条 merge = 恰好 1 次 SELECT（守护用例，行为不变）；
 * ② merge(List) 按 batchSize 先用一条 IN 查询预热主键对应的受管实体,
 *    使 200 条的 SELECT 次数从 200 降到批数级别(≤4)；
 * ③ merge(List)/save(List) 每批 flush 后 detach 本批实体(与 persist(List)
 *    的 I-12 修复同族, 防持久化上下文线性膨胀)。
 * <p/>
 * Copyright: Copyright (c) 2026-09-24
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
class MergeSelectCountTest extends AbstractOrmIntegrationTest {

	/** 造 N 条已落库的书, 返回"游离态"副本(带 id, 不在任何持久化上下文中) */
	private List<Book> seedDetachedBooks(int n) {
		seedEm.getTransaction().begin();
		for (int i = 0; i < n; i++) {
			insertBook("批量merge书" + i, "张三", "50.00", 10, true, LocalDateTime.now(), "PUBLISHED");
		}
		seedEm.getTransaction().commit();

		// 原生 SQL 读出 id(不产生 seedEm 的受管实体), 手工组装游离对象
		@SuppressWarnings("unchecked")
		List<Object[]> rows = seedEm.createNativeQuery(
				"select id, name from book where author = '张三' order by id").getResultList();
		assertEquals(n, rows.size(), "数据准备应为 " + n + " 条");

		List<Book> detached = new ArrayList<>(rows.size());
		for (Object[] row : rows) {
			Book b = new Book();
			b.setId(((Number) row[0]).longValue());
			b.setName((String) row[1]);
			b.setAuthor("张三");
			b.setPrice(new BigDecimal("50.00"));
			b.setStock(10);
			b.setPublished(true);
			b.setPublishDate(LocalDateTime.now());
			b.setStatus(BookStatus.PUBLISHED);
			b.setCreateTime(LocalDateTime.now());
			b.setUpdateTime(LocalDateTime.now());
			detached.add(b);
		}
		return detached;
	}

	@Test
	@DisplayName("单条 merge: 恰好一次 SELECT(merge 语义下限, 守护不放大)")
	void mergeSingleDoesExactlyOneSelect() {
		List<Book> one = seedDetachedBooks(1);
		one.get(0).setStock(99);

		SqlProbe.reset();
		SqlProbe.enabled = true;
		jpaDao.begin();
		try {
			Book merged = jpaDao.merge(one.get(0));
			assertNotNull(merged.getId());
			jpaDao.commit();
			long selects = SqlProbe.countMatching(" from book ");
			assertEquals(1, selects,
					"单条 merge 恰好 SELECT 一次(merge 语义下限), 实际 SQL:\n" + SqlProbe.dump());
		} finally {
			SqlProbe.enabled = false;
		}
	}

	@Test
	@DisplayName("merge(List) 200 条: SELECT 次数是批数级别(≤4), 不再是逐条 N+1")
	void mergeBatchSelectsAreBatched() {
		List<Book> books = seedDetachedBooks(200);
		books.forEach(b -> b.setStock(b.getStock() + 1));

		SqlProbe.reset();
		SqlProbe.enabled = true;
		jpaDao.begin();
		try {
			jpaDao.merge(books);
			jpaDao.commit();
		} finally {
			SqlProbe.enabled = false;
		}

		long selects = SqlProbe.countMatching(" from book ");
		// 修复前实测 200(每条 merge 各发一条单行 SELECT); 目标 ≤ 4(每批一条 IN 预热)
		assertTrue(selects <= 4,
				"200 条批量 merge 的 SELECT 应通过 IN 预热带式完成(≤4 次), 实际 " + selects
						+ " 次——仍在逐条 SELECT(N+1)");

		// 更新确实生效(抽查第 1 条)
		Book check = seedEm.find(Book.class, books.get(0).getId());
		assertEquals(11, check.getStock(), "merge 的字段更新必须真实落库");
	}

	@Test
	@DisplayName("merge(List) 每批 flush 后 detach: 事务内持久化上下文不随批量线性膨胀")
	void mergeBatchDetachesAfterEachBatch() {
		List<Book> books = seedDetachedBooks(200);

		jpaDao.begin();
		try {
			List<Book> merged = jpaDao.merge(books);
			// batchSize=100: 两批共 200 条已被 detach(返回的是游离副本)
			long managed = merged.stream().filter(b -> jpaDao.em().contains(b)).count();
			assertEquals(0, managed,
					"批量 merge 每批 flush 后应 detach 受管副本, 仍有 " + managed + " 条在持久化上下文中");
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) {
				jpaDao.rollback();
			}
		}
	}

	@Test
	@DisplayName("带主键的 save(List)(save-or-update 路径)同样走批量预热, SELECT 批数级别")
	void saveBatchWithIdsIsAlsoPrewarmed() {
		// 评审 deleg_df7e37a8 F4: save 带 id 元素走 merge 分支, 旧实现只有
		// merge(List) 预热, save(List) 更新路径仍逐条 SELECT(N+1)
		List<Book> books = seedDetachedBooks(200);
		books.forEach(b -> b.setStock(b.getStock() + 2));

		SqlProbe.reset();
		SqlProbe.enabled = true;
		jpaDao.begin();
		try {
			jpaDao.save(books);
			jpaDao.commit();
		} finally {
			SqlProbe.enabled = false;
		}
		long selects = SqlProbe.countMatching(" from book ");
		assertTrue(selects <= 4,
				"200 条带主键 save(List) 的 SELECT 应批数级别(≤4), 实际 " + selects + " 次(N+1 未消除)");
		Book check = seedEm.find(Book.class, books.get(0).getId());
		assertEquals(12, check.getStock(), "更新必须真实落库");
	}

	@Test
	@DisplayName("带主键的 save(Set) 同样走批量预热(N3: 转交 List 实现后行为一致)")
	void saveSetWithIdsIsAlsoPrewarmed() {
		// 评审 deleg_d5016c35 N3: 旧 doSave(Set) 自带一份逐条循环, 没有预热,
		// 实测 5 条带主键实体 = 5 次 SELECT; 转交 List 实现后应为 1 次
		seedEm.getTransaction().begin();
		for (int i = 0; i < 5; i++) {
			insertBook("预热Set_" + i, "预热Set作者", "10.00", 1, true,
					LocalDateTime.now(), "DRAFT");
		}
		seedEm.getTransaction().commit();
		List<Book> seeded = seedEm.createQuery(
				"select b from Book b where b.author='预热Set作者'", Book.class).getResultList();
		assertEquals(5, seeded.size());

		SqlProbe.reset();
		SqlProbe.enabled = true;
		jpaDao.begin();
		try {
			jpaDao.save(new java.util.LinkedHashSet<>(seeded));
			jpaDao.commit();
		} finally {
			SqlProbe.enabled = false;
			if (jpaDao.em().getTransaction().isActive()) jpaDao.rollback();
		}
		assertEquals(1, SqlProbe.countMatching(" in ("),
				"5 条带主键 save(Set) 预热 SELECT 应为 1 条 IN(转交 List 实现前是 5 条单行 SELECT)");
	}

	@Test
	@DisplayName("save(List) 批量新增: 每批 flush 后 detach, 上下文不滞留受管实体")
	void saveBatchNewEntitiesDetach() {
		List<Book> fresh = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			Book b = new Book();
			b.setName("批量save书" + i);
			b.setAuthor("李四");
			b.setPrice(new BigDecimal("30.00"));
			b.setStock(5);
			b.setPublished(false);
			b.setPublishDate(LocalDateTime.now());
			b.setStatus(BookStatus.DRAFT);
			fresh.add(b);
		}

		jpaDao.begin();
		try {
			jpaDao.save(fresh);
			// P1-9 的 save 半边: doSave(List) 旧实现每批只 flush 不 detach
			long managed = fresh.stream().filter(b -> jpaDao.em().contains(b)).count();
			assertEquals(0, managed,
					"save(List) 批量新增后应逐批 detach, 仍有 " + managed + " 条滞留持久化上下文");
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) {
				jpaDao.rollback();
			}
		}
		assertEquals(200, seedEm.createQuery(
				"select b from Book b where b.author = '李四'", Book.class).getResultList().size(),
				"数据必须真实落库");
	}
}
