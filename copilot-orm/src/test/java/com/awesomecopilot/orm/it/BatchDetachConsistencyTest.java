package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.BookStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批量入口的返回对象语义一致性（独立评审 deleg_df7e37a8 发现 F2）。
 * <p>
 * 背景：persist(List) 从 P0-7 起就是"每批 flush 后逐个 detach"(BatchPersistTest
 * 断言锁定，防十万级批量写持久化上下文线性膨胀)；评审发现旧 save(List)/merge(List)
 * 不 detach，导致"单条 save 后改字段能落库、批量 save 后改字段无声丢失"的
 * 同名动词两种行为。本轮把 doSave(List)/merge(List) 补齐成与 persist(List)
 * 一致——代价是调用方在批量入口之后不能再靠脏检查落库，改数据必须重新
 * merge/update。本类锁定这个统一语义:
 * ① persist(List)/save(List)/merge(List) 返回后实体一律不受管;
 * ② 单条 persist/save/merge 保持受管语义(行为不变);
 * ③ 批量入口之后改字段不会落库(与"已 detach"一致, 防止实现回退时悄悄变化)。
 *
 * @author Rico Yu
 */
class BatchDetachConsistencyTest extends AbstractOrmIntegrationTest {

	private Book detachedBook(String name) {
		Book b = new Book();
		b.setName(name);
		b.setAuthor("王五");
		b.setPrice(new BigDecimal("20.00"));
		b.setStock(3);
		b.setPublished(true);
		b.setPublishDate(LocalDateTime.now());
		b.setStatus(BookStatus.PUBLISHED);
		b.setCreateTime(LocalDateTime.now());
		b.setUpdateTime(LocalDateTime.now());
		return b;
	}

	@Test
	@DisplayName("persist(List)/save(List)/merge(List) 三种批量入口返回后实体都不受管(行为统一)")
	void allBatchEntriesReturnDetachedEntities() {
		// persist(List)
		Book p = detachedBook("persist批");
		jpaDao.begin();
		try {
			jpaDao.persist(List.of(p));
			assertFalse(jpaDao.em().contains(p), "persist(List) 返回后入参实体应已 detach");
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) jpaDao.rollback();
		}

		// save(List) 新增路径
		Book s = detachedBook("save批");
		jpaDao.begin();
		try {
			jpaDao.save(List.of(s));
			assertFalse(jpaDao.em().contains(s), "save(List) 返回后入参实体应已 detach(与 persist 统一)");
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) jpaDao.rollback();
		}

		// merge(List) 更新路径
		Book found = seedEm.createQuery("select b from Book b where b.name='save批'", Book.class)
				.getSingleResult();
		seedEm.detach(found);
		found.setStock(7);
		jpaDao.begin();
		try {
			List<Book> merged = jpaDao.merge(List.of(found));
			assertFalse(jpaDao.em().contains(merged.get(0)),
					"merge(List) 返回副本也应已 detach(与批量语义统一)");
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) jpaDao.rollback();
		}
	}

	@Test
	@DisplayName("批量入口之后改字段不落库(游离对象失去脏检查); 想改必须重新 merge")
	void dirtyCheckAfterBatchEntryDoesNotPersist() {
		Book single = detachedBook("单条保存");
		jpaDao.begin();
		try {
			jpaDao.save(single);
			single.setStock(50);
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) jpaDao.rollback();
		}
		Book reread = seedEm.createQuery("select b from Book b where b.name='单条保存'", Book.class)
				.getSingleResult();
		assertEquals(50, reread.getStock(), "单条 save 后同事务改字段应被脏检查落库(旧语义不变)");
		seedEm.detach(reread);

		Book batch = detachedBook("批量保存");
		jpaDao.begin();
		try {
			jpaDao.save(List.of(batch));
			batch.setStock(99);   // 已被 detach, 不会被脏检查
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) jpaDao.rollback();
		}
		Book reread2 = seedEm.createQuery("select b from Book b where b.name='批量保存'", Book.class)
				.getSingleResult();
		assertEquals(3, reread2.getStock(),
				"save(List) 之后改字段不落库——返回对象已游离, 与实现文档一致(评审 F2 统一语义)");

		// 正确姿势: 改完再 merge
		reread2.setStock(88);
		jpaDao.begin();
		try {
			jpaDao.merge(reread2);
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) jpaDao.rollback();
		}
		seedEm.clear();
		Book reread3 = seedEm.createQuery("select b from Book b where b.name='批量保存'", Book.class)
				.getSingleResult();
		assertEquals(88, reread3.getStock(), "重新 merge 才能更新游离对象");
	}

	@Test
	@DisplayName("save(Set) 批量入口与 save(List) 语义统一(也 detach)")
	void saveSetDetachesLikeSaveList() {
		Book b1 = detachedBook("set书1");
		Book b2 = detachedBook("set书2");
		jpaDao.begin();
		try {
			jpaDao.save(new java.util.HashSet<>(List.of(b1, b2)));
			long managed = java.util.stream.Stream.of(b1, b2)
					.filter(x -> jpaDao.em().contains(x)).count();
			assertEquals(0, managed,
					"save(Set) 应与 save(List)/persist(List) 同语义: 批后 detach, 不滞留持久化上下文");
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) jpaDao.rollback();
		}
		assertTrue(seedEm.createQuery("select b from Book b where b.author='王五'", Book.class)
				.getResultList().size() >= 2);
	}
}
