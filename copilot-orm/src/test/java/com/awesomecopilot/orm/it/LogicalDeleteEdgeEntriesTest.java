package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import com.awesomecopilot.orm.it.entity.UserOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 独立评审（deleg_344d236e，P0-7 修复审查）发现项的行为回归：
 * F2 —— 实体没有逻辑删除字段时，开启全局开关也不参与过滤（修复后从"抛
 * PathElementException"恢复为"正常查询"）；
 * F6 —— ifExists 与 findList 用同一组过滤条件，已删除记录不再让存在性判断为 true；
 * F7 —— findIn / findBetween 补上 includeDeleted 逃生门后行为可逆。
 *
 * @author Rico Yu
 */
class LogicalDeleteEdgeEntriesTest extends AbstractOrmIntegrationTest {
	
	@BeforeEach
	void enableLogicalDelete() {
		inject(jpaDao, "logicalDeleteEnabled", true);
		inject(jpaDao, "logicalDeleteField", "deleted");
	}
	
	private void insertBook(String name, BigDecimal price, boolean deleted) {
		seedEm.getTransaction().begin();
		seedEm.createNativeQuery(
				"insert into book (name, author, price, stock, published, status, deleted, create_time, update_time) "
						+ "values (?,?,?,?,?,?,?,?,?)")
				.setParameter(1, name)
				.setParameter(2, "作者")
				.setParameter(3, price)
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
		insertBook("正常书A", new BigDecimal("10.0"), false);
		insertBook("正常书B", new BigDecimal("20.0"), false);
		insertBook("已删除书", new BigDecimal("30.0"), true);
	}
	
	@Test
	@DisplayName("F2: 实体无 deleted 字段(UserOrder)时开启开关, 各读取入口正常返回而不是抛异常")
	void entityWithoutDeleteFieldIsNotFiltered() {
		// UserOrder 继承 BaseEntityShowflake, 没有 deleted 属性
		seedEm.getTransaction().begin();
		seedEm.createNativeQuery("insert into user_order (id, order_no, amount, create_time, update_time) "
				+ "values (?,?,?,?,?)")
				.setParameter(1, 9001L)
				.setParameter(2, "NO-1")
				.setParameter(3, null)
				.setParameter(4, java.time.LocalDateTime.now())
				.setParameter(5, java.time.LocalDateTime.now())
				.executeUpdate();
		seedEm.getTransaction().commit();
		assertDoesNotThrow(() -> {
			// user_order 表可能被同 JVM 其他测试共用, 断言全部按本用例插入的唯一 orderNo 收敛
			assertEquals(1, jpaDao.findList(UserOrder.class, "orderNo", "NO-1").size());
			assertFalse(jpaDao.findAll(UserOrder.class).isEmpty(), "不参与过滤的实体应能正常查询");
			assertFalse(jpaDao.findIsNull(UserOrder.class, "amount").isEmpty(),
					"无 deleted 字段的实体 findIsNull 也不该抛异常");
			assertTrue(jpaDao.ifExists(UserOrder.class, "orderNo", "NO-1"));
			assertNotNull(jpaDao.findOne(UserOrder.class, "orderNo", "NO-1"));
		});
	}
	
	@Test
	@DisplayName("F6: ifExists 与 findList 同一组条件——已删除记录两边都不算存在")
	void ifExistsConsistentWithFindList() {
		seedBooks();
		assertEquals(0, jpaDao.findList(Book.class, "name", "已删除书").size());
		assertFalse(jpaDao.ifExists(Book.class, "name", "已删除书"),
				"列表查不到的已删记录, ifExists 也不该说存在");
		assertTrue(jpaDao.ifExists(Book.class, "name", "正常书A"));
	}
	
	@Test
	@DisplayName("F7: findIn / findBetween 的 includeDeleted 逃生门放行已删除记录")
	void findInAndBetweenHaveEscapeHatch() {
		seedBooks();
		// 默认: 价格 in (10,20,30) 只有 2 条(30 的已删)
		assertEquals(2, jpaDao.findIn(Book.class, "price",
				List.of(new BigDecimal("10.0"), new BigDecimal("20.0"), new BigDecimal("30.0"))).size());
		// 逃生门: 3 条
		assertEquals(3, jpaDao.findIn(Book.class, "price",
				List.of(new BigDecimal("10.0"), new BigDecimal("20.0"), new BigDecimal("30.0")),
				true).size(), "includeDeleted=true 应放行已删除记录");
		
		LocalDateTime begin = LocalDateTime.now().minusDays(1);
		LocalDateTime end = LocalDateTime.now().plusDays(1);
		assertEquals(2, jpaDao.findBetween(Book.class, "createTime", begin, end).size());
		assertEquals(3, jpaDao.findBetween(Book.class, "createTime", begin, end, true).size());
	}
}
