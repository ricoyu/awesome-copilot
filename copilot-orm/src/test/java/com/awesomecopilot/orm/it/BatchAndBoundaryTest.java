package com.awesomecopilot.orm.it;

import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.BookStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批量操作变体 + 分页/集合边界场景:
 * save(Set) / merge(List) / ensureMultiEntityExists / deleteByPK(int[]) / 分页越界 / 空集合
 *
 * @author Rico Yu
 */
class BatchAndBoundaryTest extends AbstractOrmIntegrationTest {

	@Test
	@DisplayName("save(Set) 批量保存去重后的集合")
	void saveSet() {
		Set<Book> books = new HashSet<>();
		books.add(new Book("集合书A", "A", new BigDecimal("1"), 1, true, LocalDateTime.now(), BookStatus.DRAFT));
		books.add(new Book("集合书B", "B", new BigDecimal("2"), 2, true, LocalDateTime.now(), BookStatus.PUBLISHED));
		jpaDao.begin();
		List<Book> saved = jpaDao.save(books);
		jpaDao.commit();
		assertEquals(2, saved.size());
		assertEquals(2, jpaDao.findAll(Book.class).size());
	}

	@Test
	@DisplayName("merge(List) 批量合并返回受管对象")
	void mergeList() {
		seedStandardBooks();
		List<Book> all = jpaDao.findAll(Book.class);
		// 改名字后 merge 批量
		all.get(0).setName("改名的书");
		all.get(1).setName("也改名了");
		jpaDao.begin();
		List<Book> merged = jpaDao.merge(all);
		jpaDao.commit();

		assertEquals(5, merged.size());
		// 名字确实更新了
		Book reloaded = jpaDao.get(Book.class, all.get(0).getId());
		assertEquals("改名的书", reloaded.getName());
	}

	@Test
	@DisplayName("ensureMultiEntityExists: 全部存在返回列表, 有缺失抛异常")
	void ensureMultiEntityExists() {
		seedStandardBooks();
		List<Book> all = jpaDao.findAll(Book.class);
		Long id1 = all.get(0).getId();
		Long id2 = all.get(1).getId();

		List<Book> result = jpaDao.ensureMultiEntityExists(Book.class, id1, id2);
		assertEquals(2, result.size());

		assertThrows(EntityNotFoundException.class,
				() -> jpaDao.ensureMultiEntityExists(Book.class, id1, 999999L));
	}

	@Test
	@DisplayName("deleteByPK(int[]) 原始 int 数组重载")
	void deleteByPkIntArray() {
		seedStandardBooks();
		List<Book> all = jpaDao.findAll(Book.class);
		int id1 = all.get(0).getId().intValue();
		int id2 = all.get(1).getId().intValue();

		jpaDao.begin();
		jpaDao.deleteByPK(Book.class, new int[]{id1, id2});
		jpaDao.commit();
		assertEquals(3, jpaDao.findAll(Book.class).size());
	}

	@Test
	@DisplayName("分页越界: 请求的页码超过总页数时返回空列表, 不报错")
	void findPageBeyondRange() {
		seedStandardBooks();
		Page page = new Page();
		page.setPageNum(10); // 总共才 5 条, 第 10 页不存在
		page.setPageSize(2);
		List<Book> result = jpaDao.query(Book.class).findPage(page);
		assertEquals(0, result.size(), "越界页应返回空列表而不是报错");
		assertEquals(5, page.getTotalCount(), "总记录数仍应正确回填");
	}

	@Test
	@DisplayName("分页 pageSize 大于总记录数时返回全部")
	void findPageSizeExceedsTotal() {
		seedStandardBooks();
		Page page = new Page();
		page.setPageNum(1);
		page.setPageSize(100);
		List<Book> result = jpaDao.query(Book.class).findPage(page);
		assertEquals(5, result.size(), "pageSize 大于总数时应返回全部 5 条");
	}

	@Test
	@DisplayName("分页第一页 pageNum=0 视为从第一条开始, 不跳过")
	void findPageNumZero() {
		seedStandardBooks();
		Page page = new Page();
		page.setPageNum(0); // 特殊值, 应等价于第 1 页
		page.setPageSize(2);
		List<Book> result = jpaDao.query(Book.class).findPage(page);
		assertEquals(2, result.size(), "pageNum=0 应等同第 1 页返回前 2 条");
	}

	@Test
	@DisplayName("getMulti 空 id 列表返回空列表")
	void getMultiEmptyIds() {
		seedStandardBooks();
		List<Book> result = jpaDao.getMulti(Book.class, List.of());
		assertEquals(0, result.size());
	}

	@Test
	@DisplayName("findIn 空集合返回空列表(不抛异常)")
	void findInEmptyCollection() {
		seedStandardBooks();
		List<Book> result = jpaDao.findIn(Book.class, "name", List.of());
		assertEquals(0, result.size());
	}

	@Test
	@DisplayName("deleteByPK 空集合不报错, 不删任何记录")
	void deleteByPkEmptyCollection() {
		seedStandardBooks();
		jpaDao.begin();
		jpaDao.deleteByPK(Book.class, List.of());
		jpaDao.commit();
		assertEquals(5, jpaDao.findAll(Book.class).size(), "空集合删除不应影响任何记录");
	}
}
