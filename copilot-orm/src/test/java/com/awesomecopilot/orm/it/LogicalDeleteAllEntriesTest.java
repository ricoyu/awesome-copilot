package com.awesomecopilot.orm.it;

import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 逻辑删除过滤的入口全覆盖回归（评估报告 P0-7 主因）。
 * <p>
 * 原缺陷：applyLogicalDeleteFilter 只接在 find/findIn/findBetween 上，
 * findAll / findList / findOne(Class,PK) / findIsNull / query(Class) criteria builder
 * 都不过滤——同一实体"列表接口返回已删除数据、明细接口不返回"。
 * <p>
 * 本类对每个入口断言<b>默认即过滤</b>（已删除记录不出现）；
 * includeDeleted 逃生门开关的用例在实现提交后补充。
 * 实体覆盖 Boolean 型（Book.deleted）与 Integer 型（Product.deleted）两种标记类型，
 * Integer 型是历史上"查询按 0 过滤、删除写 true 类型错配"的同族隐患。
 *
 * @author Rico Yu
 */
class LogicalDeleteAllEntriesTest extends AbstractOrmIntegrationTest {
	
	@BeforeEach
	void enableLogicalDelete() {
		// 基类 @BeforeEach 先跑, 这里在其后开启逻辑删除
		inject(jpaDao, "logicalDeleteEnabled", true);
		inject(jpaDao, "logicalDeleteField", "deleted");
	}
	
	private void insertBook(String name, boolean deleted) {
		seedEm.getTransaction().begin();
		seedEm.createNativeQuery(
				"insert into book (name, author, price, stock, published, status, deleted, create_time, update_time) "
						+ "values (?,?,?,?,?,?,?,?,?)")
				.setParameter(1, name)
				.setParameter(2, "作者")
				.setParameter(3, "50.00")
				.setParameter(4, 10)
				.setParameter(5, true)
				.setParameter(6, "PUBLISHED")
				.setParameter(7, deleted)
				.setParameter(8, java.time.LocalDateTime.now())
				.setParameter(9, java.time.LocalDateTime.now())
				.executeUpdate();
		seedEm.getTransaction().commit();
	}
	
	private void seedBooks() {
		insertBook("正常书A", false);
		insertBook("正常书B", false);
		insertBook("已删除书", true);
	}
	
	/** Product.deleted 是 Integer: 0=未删, 1=已删 */
	private void insertProduct(String name, int deleted) {
		seedEm.getTransaction().begin();
		seedEm.createNativeQuery("insert into product (name, deleted) values (?,?)")
				.setParameter(1, name)
				.setParameter(2, deleted)
				.executeUpdate();
		seedEm.getTransaction().commit();
	}
	
	private List<String> names(List<Book> books) {
		return books.stream().map(Book::getName).sorted().toList();
	}
	
	// ==================== findAll ====================
	
	@Test
	@DisplayName("findAll 过滤已删除记录")
	void findAllFiltersDeleted() {
		seedBooks();
		assertEquals(List.of("正常书A", "正常书B"), names(jpaDao.findAll(Book.class)));
	}
	
	@Test
	@DisplayName("findAll 对 Integer 型删除标记(Product)同样过滤")
	void findAllFiltersIntegerFlagProduct() {
		insertProduct("在售", 0);
		insertProduct("下架已删", 1);
		List<Product> all = jpaDao.findAll(Product.class);
		assertEquals(1, all.size(), "Integer 标记=1 的行应被过滤");
		assertEquals("在售", all.get(0).getName());
	}
	
	// ==================== findList(Class, prop, value) ====================
	
	@Test
	@DisplayName("findList 过滤已删除记录")
	void findListFiltersDeleted() {
		seedBooks();
		// 已删除书按条件查也不该出现
		assertEquals(0, jpaDao.findList(Book.class, "name", "已删除书").size(),
				"默认过滤下按名字查已删除书应返回空");
		assertEquals(1, jpaDao.findList(Book.class, "name", "正常书A").size());
	}
	
	// ==================== findOne(Class, prop, value) ====================
	
	@Test
	@DisplayName("findOne 按属性查不到已删除记录")
	void findOneByPropertyRespectsFilter() {
		seedBooks();
		assertNull(jpaDao.findOne(Book.class, "name", "已删除书"), "默认应查不到已删除记录");
		assertNotNull(jpaDao.findOne(Book.class, "name", "正常书A"));
	}
	
	// ==================== findOne(Class, id) ====================
	
	@Test
	@DisplayName("findOne(Class,PK) 查已删除记录返回 empty")
	void findOneByIdFiltersDeleted() {
		seedBooks();
		seedEm.getTransaction().begin();
		Long deletedId = ((Number) seedEm.createNativeQuery(
				"select id from book where deleted = true").getSingleResult()).longValue();
		seedEm.getTransaction().commit();
		
		Optional<Book> found = jpaDao.findOne(Book.class, deletedId);
		assertEquals(Optional.empty(), found, "按主键也应过滤已删除记录");
	}
	
	// ==================== findIsNull ====================
	
	@Test
	@DisplayName("findIsNull 过滤已删除记录")
	void findIsNullFiltersDeleted() {
		seedBooks();
		// publishDate 全为 null: 默认只返回 2 本正常书
		assertEquals(2, jpaDao.findIsNull(Book.class, "publishDate").size());
	}
	
	// ==================== query(Class) criteria builder ====================
	
	@Test
	@DisplayName("query(Class) 构建器查询默认过滤已删除记录")
	void criteriaBuilderFiltersDeleted() {
		seedBooks();
		List<Book> result = jpaDao.query(Book.class).gt("price", BigDecimal.ZERO).findList();
		assertEquals(List.of("正常书A", "正常书B"), names(result));
	}
	
	@Test
	@DisplayName("query(Class) 分页 count 与列表同样过滤(否则总数与明细不一致)")
	void criteriaBuilderPagedCountConsistent() {
		seedBooks();
		Page page = new Page();
		page.setPageNum(1);
		page.setPageSize(10);
		List<Book> result = jpaDao.query(Book.class).gt("price", BigDecimal.ZERO).findPage(page);
		assertEquals(2, result.size());
		assertEquals(2, page.getTotalCount(), "count 查询也必须带 deleted 过滤");
	}
}
