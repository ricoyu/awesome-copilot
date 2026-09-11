package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.exception.EntityOperationException;
import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.BookStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实体 CRUD 与主键查询场景: persist/save/merge/delete/get/find/load/getMulti/listByIds/findAll/detach/flush
 *
 * @author Rico Yu
 */
class EntityCrudTest extends AbstractOrmIntegrationTest {

	@Test
	@DisplayName("persist 单个 + get 按主键查回, id 由 IDENTITY 自动生成")
	void persistAndGet() {
		Book book = new Book("测试书", "张三", new BigDecimal("50.00"), 10, true,
				LocalDateTime.now(), BookStatus.PUBLISHED);
		jpaDao.begin();
		jpaDao.persist(book);
		jpaDao.commit();

		assertNotNull(book.getId(), "persist 后应自动生成主键");

		Book loaded = jpaDao.get(Book.class, book.getId());
		assertNotNull(loaded);
		assertEquals("测试书", loaded.getName());
		assertEquals(BookStatus.PUBLISHED, loaded.getStatus());
	}

	@Test
	@DisplayName("persist 批量 + findAll 返回全部")
	void persistBatchAndFindAll() {
		List<Book> books = Arrays.asList(
				new Book("书A", "A", new BigDecimal("1"), 1, true, LocalDateTime.now(), BookStatus.DRAFT),
				new Book("书B", "B", new BigDecimal("2"), 2, true, LocalDateTime.now(), BookStatus.PUBLISHED),
				new Book("书C", "C", new BigDecimal("3"), 3, false, LocalDateTime.now(), BookStatus.ARCHIVED));
		jpaDao.begin();
		jpaDao.persist(books);
		jpaDao.commit();

		List<Book> all = jpaDao.findAll(Book.class);
		assertEquals(3, all.size());
	}

	@Test
	@DisplayName("save: id 为 null 走 persist, 已有 id 走 merge")
	void saveNewAndExisting() {
		Book book = new Book("新书", "作者", new BigDecimal("10"), 5, true,
				LocalDateTime.now(), BookStatus.DRAFT);
		jpaDao.begin();
		Book saved = jpaDao.save(book);
		jpaDao.commit();
		assertNotNull(saved.getId());

		// 已有 id 再 save, 应更新而非插入
		jpaDao.begin();
		saved.setName("改名后的书");
		Book merged = jpaDao.save(saved);
		jpaDao.commit();

		assertEquals(saved.getId(), merged.getId());
		Book reloaded = jpaDao.get(Book.class, saved.getId());
		assertEquals("改名后的书", reloaded.getName());
		assertEquals(1, jpaDao.findAll(Book.class).size(), "save 已有 id 不应新增记录");
	}

	@Test
	@DisplayName("merge 返回受管对象, 与传入对象不是同一个引用")
	void mergeReturnsManagedCopy() {
		Book book = new Book("merge测试", "作者", new BigDecimal("10"), 5, true,
				LocalDateTime.now(), BookStatus.DRAFT);
		jpaDao.begin();
		Book merged = jpaDao.merge(book);
		jpaDao.commit();

		assertNotNull(merged.getId());
		assertNotSame(book, merged, "merge 返回的是受管副本");
	}

	@Test
	@DisplayName("delete 单个 + delete 批量")
	void deleteSingleAndBatch() {
		seedStandardBooks();
		List<Book> all = jpaDao.findAll(Book.class);
		assertEquals(5, all.size());

		// 无 Spring 事务时, get/findAll 返回的对象是脱管的(detached),
		// 直接 delete 会抛 "Removing a detached instance"。正确做法是在事务内 get, 对象受管后可删
		jpaDao.begin();
		Book first = jpaDao.get(Book.class, all.get(0).getId());
		jpaDao.delete(first);
		jpaDao.commit();
		assertEquals(4, jpaDao.findAll(Book.class).size());

		// 批量删剩下的: 事务内 findAll 得到的对象都是受管的
		jpaDao.begin();
		jpaDao.delete(jpaDao.findAll(Book.class));
		jpaDao.commit();
		assertEquals(0, jpaDao.findAll(Book.class).size());
	}

	@Test
	@DisplayName("deleteByPK: 单个 / 数组 / long[] / Collection 四种重载")
	void deleteByPkVariousOverloads() {
		seedStandardBooks();
		List<Book> all = jpaDao.findAll(Book.class);
		assertEquals(5, all.size());

		Long id1 = all.get(0).getId();
		Long id2 = all.get(1).getId();
		Long id3 = all.get(2).getId();
		Long id4 = all.get(3).getId();
		Long id5 = all.get(4).getId();

		jpaDao.begin();
		jpaDao.deleteByPK(Book.class, id1); // 单个
		jpaDao.commit();
		assertEquals(4, jpaDao.findAll(Book.class).size());

		jpaDao.begin();
		jpaDao.deleteByPK(Book.class, new Long[]{id2, id3}); // 数组
		jpaDao.commit();
		assertEquals(2, jpaDao.findAll(Book.class).size());

		jpaDao.begin();
		jpaDao.deleteByPK(Book.class, new long[]{id4}); // long[] 原始数组
		jpaDao.commit();
		assertEquals(1, jpaDao.findAll(Book.class).size());

		jpaDao.begin();
		jpaDao.deleteByPK(Book.class, List.of(id5)); // Collection
		jpaDao.commit();
		assertEquals(0, jpaDao.findAll(Book.class).size());
	}

	@Test
	@DisplayName("find 按主键查, 不存在返回 null; findOne 返回 Optional")
	void findAndFindOne() {
		seedStandardBooks();
		Long existId = jpaDao.findAll(Book.class).get(0).getId();

		Book found = jpaDao.find(Book.class, existId);
		assertNotNull(found);

		assertNull(jpaDao.find(Book.class, 999999L), "find 不存在的 id 应返回 null");

		Optional<Book> one = jpaDao.findOne(Book.class, existId);
		assertTrue(one.isPresent());
		assertEquals(existId, one.get().getId());
	}

	@Test
	@DisplayName("load 无事务时返回已初始化的实体, 访问属性不抛懒加载异常")
	void loadInitializesOutsideTransaction() {
		seedStandardBooks();
		Long existId = jpaDao.findAll(Book.class).get(0).getId();

		// load 无 Spring 事务且无手动事务时, 会立即初始化, 返回脱管可用对象
		Book loaded = jpaDao.load(Book.class, existId);
		assertNotNull(loaded);
		assertDoesNotThrow(() -> loaded.getName(), "load 无事务应已初始化, 访问属性不该抛 LazyInitializationException");
	}

	@Test
	@DisplayName("ensureEntityExists: 存在返回实体, 不存在抛 EntityNotFoundException")
	void ensureEntityExists() {
		seedStandardBooks();
		Long existId = jpaDao.findAll(Book.class).get(0).getId();

		assertNotNull(jpaDao.ensureEntityExists(Book.class, existId));

		assertThrows(EntityNotFoundException.class,
				() -> jpaDao.ensureEntityExists(Book.class, 999999L));
	}

	@Test
	@DisplayName("getMulti: 可变参数 / long[] 原始数组 / List 三种重载")
	void getMultiVariousOverloads() {
		seedStandardBooks();
		List<Book> all = jpaDao.findAll(Book.class);
		Long id1 = all.get(0).getId();
		Long id2 = all.get(1).getId();
		Long id3 = all.get(2).getId();

		// 可变参数
		List<Book> byVarargs = jpaDao.getMulti(Book.class, id1, id2);
		assertEquals(2, byVarargs.size());

		// long[] 原始数组(曾经会被封装成单个元素导致只查一个)
		List<Book> byPrimitiveArray = jpaDao.getMulti(Book.class, new long[]{id1, id3});
		assertEquals(2, byPrimitiveArray.size(), "long[] 应展开为多个 id 而不是单个元素");

		// List
		List<Book> byList = jpaDao.getMulti(Book.class, List.of(id2, id3));
		assertEquals(2, byList.size());
	}

	@Test
	@DisplayName("listByIds 过滤掉 null(不存在的 id 位置), 去重")
	void listByIdsFiltersNullAndDistinct() {
		seedStandardBooks();
		List<Book> all = jpaDao.findAll(Book.class);
		Long id1 = all.get(0).getId();
		Long id2 = all.get(1).getId();

		List<Book> result = jpaDao.listByIds(Book.class, id1, id2, 999999L, id1);
		assertEquals(2, result.size(), "应去重并过滤不存在的 id");
	}

	@Test
	@DisplayName("detach 后实体脱离持久化上下文")
	void detach() {
		seedStandardBooks();
		Book book = jpaDao.get(Book.class, jpaDao.findAll(Book.class).get(0).getId());
		assertDoesNotThrow(() -> jpaDao.detach(book));
	}

	@Test
	@DisplayName("delete 一个已存在的 id 之外的游离对象(未受管)会抛异常")
	void deleteDetachedEntityThrows() {
		seedStandardBooks();
		Book book = jpaDao.findAll(Book.class).get(0);
		// get 返回的对象在无事务下是脱管的, 直接 remove 会抛 EntityNotFoundException
		Book detached = new Book("游离对象", "x", BigDecimal.ONE, 1, true,
				LocalDateTime.now(), BookStatus.DRAFT);
		detached.setId(999999L);
		assertThrows(Exception.class, () -> jpaDao.delete(detached));
	}

	@Test
	@DisplayName("persist null 会抛异常而不是空指针吞掉")
	void persistNullThrows() {
		assertThrows(Exception.class, () -> jpaDao.persist((Book) null));
	}

	@Test
	@DisplayName("手动事务 rollback 回滚未提交的写入")
	void rollbackUndoesWrites() {
		seedStandardBooks();
		assertEquals(5, jpaDao.findAll(Book.class).size());

		jpaDao.begin();
		jpaDao.persist(new Book("回滚书", "作者", new BigDecimal("1"), 1, true,
				LocalDateTime.now(), BookStatus.DRAFT));
		jpaDao.rollback();

		assertEquals(5, jpaDao.findAll(Book.class).size(), "rollback 后新增的记录应不存在");
	}

	@Test
	@DisplayName("begin 在无 Spring 事务时开启本地事务, commit 落库")
	void beginCommitPersists() {
		Book book = new Book("事务书", "作者", new BigDecimal("9.90"), 1, true,
				LocalDateTime.now(), BookStatus.PUBLISHED);
		jpaDao.begin();
		jpaDao.persist(book);
		jpaDao.commit();

		assertNotNull(book.getId());
		Book loaded = jpaDao.get(Book.class, book.getId());
		assertEquals("事务书", loaded.getName());
	}
}
