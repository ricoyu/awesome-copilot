package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.BookStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批量写入场景(P2-12 回归): persist(List) 每批 flush 后逐个 detach 释放一级缓存,
 * 验证大数据量批量写入正确、且持久化上下文不随条数线性膨胀。
 *
 * @author Rico Yu
 */
class BatchPersistTest extends AbstractOrmIntegrationTest {

	private Book newBook(int i) {
		return new Book("批量书" + i, "作者" + (i % 10), new BigDecimal(i), i, true,
				LocalDateTime.now(), BookStatus.PUBLISHED);
	}

	@Test
	@DisplayName("persist(List) 跨多个批次(超过 batchSize=100): 全部落库且 id 回填")
	void persistListAcrossBatches() {
		int total = 250; // 跨 3 个批次(100+100+50), 覆盖批内 detach 和尾批 detach 两条路径
		List<Book> books = new ArrayList<>(total);
		for (int i = 0; i < total; i++) {
			books.add(newBook(i));
		}

		jpaDao.begin();
		jpaDao.persist(books);
		jpaDao.commit();

		Object count = jpaDao.findOne("select count(*) from book");
		assertEquals(total, ((Number) count).intValue(), "250 条应全部落库");
		// id 全部回填
		for (Book b : books) {
			assertNotNull(b.getId(), "persist 后每条都应回填 id");
		}
	}

	@Test
	@DisplayName("批量写入后一级缓存被释放: 持久化上下文不随条数膨胀")
	void persistenceContextReleasedAfterBatch() {
		int total = 300;
		List<Book> books = new ArrayList<>(total);
		for (int i = 0; i < total; i++) {
			books.add(newBook(i));
		}

		jpaDao.begin();
		jpaDao.persist(books);
		// flush 之后、commit 之前检查: 已 detach 的实体不再受管
		EntityManager em = jpaDao.em();
		int managed = 0;
		for (Book b : books) {
			if (em.contains(b)) {
				managed++;
			}
		}
		jpaDao.commit();

		// 每批 flush 后都 detach 了, 持久化上下文里不应还挂着这 300 个实体
		// (允许极少量尾批未 detach 的, 但绝不应是全部 300 个都还受管)
		assertTrue(managed < total / 2,
				"批量写入后大部分实体应已 detach 释放, 实际仍受管: " + managed + "/" + total);
	}

	@Test
	@DisplayName("persist(List) 空列表不报错")
	void persistEmptyList() {
		jpaDao.begin();
		assertDoesNotThrow(() -> jpaDao.persist(new ArrayList<Book>()));
		jpaDao.commit();
		assertEquals(0, ((Number) jpaDao.findOne("select count(*) from book")).intValue());
	}

	@Test
	@DisplayName("persist(List) 恰好等于 batchSize 的整数倍")
	void persistExactBatchMultiple() {
		int total = 200; // 恰好 2 个整批, 无尾批零头
		List<Book> books = new ArrayList<>(total);
		for (int i = 0; i < total; i++) {
			books.add(newBook(i));
		}
		jpaDao.begin();
		jpaDao.persist(books);
		jpaDao.commit();
		assertEquals(total, ((Number) jpaDao.findOne("select count(*) from book")).intValue());
	}
}
