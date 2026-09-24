package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 逻辑删除 includeDeleted 逃生门回归（评估报告 P0-7 修复的开关面）。
 * <p>
 * 默认过滤由 {@link LogicalDeleteAllEntriesTest} 覆盖；本类断言每个入口的
 * includeDeleted=true 能放行全部记录（含已删除）、=false/缺省与默认一致，
 * 保证"统一过滤"不牺牲读取历史数据的逃生门。
 *
 * @author Rico Yu
 */
class LogicalDeleteIncludeDeletedTest extends AbstractOrmIntegrationTest {
	
	@BeforeEach
	void enableLogicalDelete() {
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
	
	@Test
	@DisplayName("findAll 带 includeDeleted 开关: true 放行全部, false 与缺省一致")
	void findAllIncludeDeleted() {
		seedBooks();
		assertEquals(3, jpaDao.findAll(Book.class, true).size(), "开关放行时应看到全部 3 本");
		assertEquals(2, jpaDao.findAll(Book.class, false).size());
	}
	
	@Test
	@DisplayName("findList 带 includeDeleted: 开关放行后按名字能查到已删除书")
	void findListIncludeDeleted() {
		seedBooks();
		assertEquals(1, jpaDao.findList(Book.class, "name", "已删除书", true).size());
		assertEquals(0, jpaDao.findList(Book.class, "name", "已删除书", false).size());
	}
	
	@Test
	@DisplayName("findOne(Class,prop,value) 带 includeDeleted: 放行后可查到")
	void findOneByPropertyIncludeDeleted() {
		seedBooks();
		assertNotNull(jpaDao.findOne(Book.class, "name", "已删除书", true), "开关放行后应查到");
	}
	
	@Test
	@DisplayName("findIsNull 带 includeDeleted: true 放行全部 3 本")
	void findIsNullIncludeDeleted() {
		seedBooks();
		assertEquals(3, jpaDao.findIsNull(Book.class, "publishDate", true).size());
	}
	
	@Test
	@DisplayName("query(Class) 构建器 includeDeleted(true) 放行全部")
	void criteriaBuilderIncludeDeleted() {
		seedBooks();
		List<Book> result = jpaDao.query(Book.class)
				.includeDeleted(true).gt("price", BigDecimal.ZERO).findList();
		assertEquals(3, result.size());
		// 开关可逆: 新构建的请求仍默认过滤
		assertEquals(2, jpaDao.query(Book.class).gt("price", BigDecimal.ZERO).findList().size());
	}
}
