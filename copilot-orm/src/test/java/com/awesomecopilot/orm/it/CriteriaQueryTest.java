package com.awesomecopilot.orm.it;

import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.BookStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Criteria 查询场景: query(Class) 链式条件 + CriteriaOperations 便捷方法
 * (findOne/findList/findBetween/findIn/findIsNull/ensureExists/ifExists/deleteIn)
 *
 * @author Rico Yu
 */
class CriteriaQueryTest extends AbstractOrmIntegrationTest {

	@Test
	@DisplayName("query(Class).eq 精确匹配单字段")
	void eqQuery() {
		seedStandardBooks();
		List<Book> result = criteriaOperations.query(Book.class).eq("name", "深入理解JVM").findList();
		assertEquals(1, result.size());
		assertEquals("周志明", result.get(0).getAuthor());
	}

	@Test
	@DisplayName("eq 传入 null 值被忽略, 不产生条件(返回全部)")
	void eqNullIgnored() {
		seedStandardBooks();
		List<Book> result = jpaDao.query(Book.class).eq("name", null).findList();
		//List<Book> result = jpaDao.query(Book.class).isNull("name").findList();
		assertEquals(5, result.size());
	}

	@Test
	@DisplayName("数值范围查询: gt/ge/lt/le 组合")
	void numericRangeQuery() {
		seedStandardBooks();
		// price >= 88 的书
		List<Book> ge88 = jpaDao.query(Book.class).ge("price", 88).findList();
		assertEquals(3, ge88.size(), "88/98.5/108 三本 >= 88");

		// price < 80 的书
		List<Book> lt80 = jpaDao.query(Book.class).lt("price", 80).findList();
		assertEquals(2, lt80.size(), "79/66 两本 < 80");
	}

	@Test
	@DisplayName("like 模糊匹配")
	void likeQuery() {
		seedStandardBooks();
		// 含 "Java" 字样的书名: Java编程思想 / Java并发实战 / Effective Java, 共 3 本
		List<Book> result = jpaDao.query(Book.class).like("name", "%Java%").findList();
		assertEquals(3, result.size());
	}

	@Test
	@DisplayName("in 集合查询 + 空集合被跳过返回全部")
	void inQuery() {
		seedStandardBooks();
		List<Book> result = jpaDao.query(Book.class)
				.in("author", Arrays.asList("Bruce", "周志明"))
				.findList();
		assertEquals(2, result.size());

		// 空集合应被忽略
		List<Book> emptyIn = jpaDao.query(Book.class).in("author", List.of()).findList();
		assertEquals(5, emptyIn.size());
	}

	@Test
	@DisplayName("isNull / isNotNull 查询")
	void nullQuery() {
		seedStandardBooks();
		// book 表没存 null, 用 findIsNull 便捷方法对不存在的 null 列
		List<Book> nulls = jpaDao.findIsNull(Book.class, "author");
		assertEquals(0, nulls.size());
	}

	@Test
	@DisplayName("排序 asc/desc + limit 截取")
	void orderAndLimit() {
		seedStandardBooks();
		List<Book> asc = jpaDao.query(Book.class).asc("price").limit(2).findList();
		assertEquals(2, asc.size());
		assertEquals("66.00", asc.get(0).getPrice().toString(), "最便宜的排最前");

		List<Book> desc = jpaDao.query(Book.class).desc("price").findList();
		assertEquals("108.00", desc.get(0).getPrice().toString(), "最贵的排最前");
	}

	@Test
	@DisplayName("between 时间范围查询")
	void betweenDateTime() {
		seedStandardBooks();
		LocalDateTime begin = LocalDateTime.of(2021, 1, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2022, 12, 31, 23, 59);
		List<Book> result = jpaDao.findBetween(Book.class, "publishDate", begin, end);
		assertEquals(2, result.size(), "2021-03 和 2022-05 两本");
	}

	@Test
	@DisplayName("findOne/findList 按属性值查询")
	void findOneAndListByProperty() {
		seedStandardBooks();
		Book one = jpaDao.findOne(Book.class, "author", "周志明");
		assertNotNull(one);
		assertEquals("深入理解JVM", one.getName());

		List<Book> list = jpaDao.findList(Book.class, "published", true);
		assertEquals(4, list.size(), "只有 Java并发实战 published=false");
	}

	@Test
	@DisplayName("ifExists 判断记录是否存在")
	void ifExists() {
		seedStandardBooks();
		assertTrue(jpaDao.ifExists(Book.class, "name", "Clean Code"));
		assertFalse(jpaDao.ifExists(Book.class, "name", "不存在的书"));
	}

	@Test
	@DisplayName("ensureExists 按属性查询, 不存在抛异常")
	void ensureExistsByProperty() {
		seedStandardBooks();
		Book book = jpaDao.ensureExists(Book.class, "name", "Effective Java");
		assertNotNull(book);
		assertThrows(EntityNotFoundException.class,
				() -> jpaDao.ensureExists(Book.class, "name", "不存在"));
	}

	@Test
	@DisplayName("findIn 按集合查询")
	void findInByProperty() {
		seedStandardBooks();
		List<Book> result = jpaDao.findIn(Book.class, "name",
				Arrays.asList("Clean Code", "Effective Java"));
		assertEquals(2, result.size());
	}

	@Test
	@DisplayName("deleteIn 按属性集合批量删除")
	void deleteInByProperty() {
		seedStandardBooks();
		jpaDao.begin();
		int deleted = jpaDao.deleteIn(Book.class, "name", Arrays.asList("Clean Code", "Effective Java"));
		jpaDao.commit();
		assertEquals(2, deleted);
		assertEquals(3, jpaDao.findAll(Book.class).size());
	}

	@Test
	@DisplayName("findPage 分页 + 总记录数回填 Page")
	void findPage() {
		seedStandardBooks();
		Page page = new Page();
		page.setPageNum(1);
		page.setPageSize(2);
		List<Book> firstPage = jpaDao.query(Book.class).findPage(page);
		assertEquals(2, firstPage.size());
		assertEquals(5, page.getTotalCount(), "Page.totalCount 应回填总记录数");
	}

	@Test
	@DisplayName("notEq 不等于查询")
	void notEqQuery() {
		seedStandardBooks();
		List<Book> result = jpaDao.query(Book.class).notEq("status", BookStatus.PUBLISHED).findList();
		assertEquals(2, result.size(), "DRAFT 和 ARCHIVED 两本非 PUBLISHED");
	}

	@Test
	@DisplayName("deleteBy: 条件删除(CriteriaDelete)")
	void deleteByCriteria() {
		seedStandardBooks();
		// deleteBy 返回独立的 CriteriaDeleteBuilder, 用的是自己的 EM holder,
		// 不像 deleteIn 那样复用 JpaDao.begin() 的事务, 所以它依赖 Spring 事务。
		// 这里用 TransactionSynchronizationManager 模拟一个 Spring 事务环境来验证它
		EntityManager txEm = emf.createEntityManager();
		inject(jpaDao, "entityManager", txEm);
		txEm.getTransaction().begin();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			int deleted = jpaDao.deleteBy(Book.class).eq("author", "Bruce").execute();
			assertEquals(1, deleted);
			txEm.getTransaction().commit();
		} finally {
			TransactionSynchronizationManager.setActualTransactionActive(false);
			txEm.close();
		}
		assertEquals(4, jpaDao.findAll(Book.class).size());
	}

	@Test
	@DisplayName("组合条件: eq + ge 同时生效")
	void combinedConditions() {
		seedStandardBooks();
		List<Book> result = jpaDao.query(Book.class)
				.eq("published", true)
				.ge("price", 88)
				.findList();
		assertEquals(3, result.size(), "published 且 price>=88: 98.5/88/108 三本");
	}
}
