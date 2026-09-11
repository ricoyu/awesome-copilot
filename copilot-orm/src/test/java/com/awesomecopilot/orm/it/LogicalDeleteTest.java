package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 逻辑删除场景: logicalDeleteEnabled=true 时的行为。
 * <p>
 * 注意: 框架的逻辑删除过滤目前只覆盖 find / findIn / findBetween / deleteByPK 几条路径,
 * findAll 和 query(Class) criteria 查询暂未接入过滤(那是已知的不一致, 不是本测试要断言的行为),
 * 所以这里用原生 SQL 直接数物理记录, 绕开 findAll, 聚焦"已实现的逻辑删除行为"是否稳定。
 *
 * @author Rico Yu
 */
class LogicalDeleteTest extends AbstractOrmIntegrationTest {

	@BeforeEach
	void enableLogicalDelete() {
		// 在基类 setup 之后(父类 @BeforeEach 先执行)再开启逻辑删除
		inject(jpaDao, "logicalDeleteEnabled", true);
		inject(jpaDao, "logicalDeleteField", "deleted");
	}

	/** 插入带 deleted 标记的书 */
	private void insertBookWithDeleted(String name, boolean deleted) {
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
	}

	private void seedWithDeleted() {
		seedEm.getTransaction().begin();
		insertBookWithDeleted("正常书A", false);
		insertBookWithDeleted("正常书B", false);
		insertBookWithDeleted("已删除书", true);
		seedEm.getTransaction().commit();
	}

	/** 直接数物理记录数, 绕开 findAll(它不过滤 deleted) */
	private long countPhysical() {
		return ((Number) seedEm.createNativeQuery("select count(*) from book").getSingleResult()).longValue();
	}

	@Test
	@DisplayName("find 按主键查已删除记录返回 null")
	void findFiltersDeletedById() {
		seedWithDeleted();
		List<Number> ids = seedEm.createNativeQuery("select id from book where deleted = true").getResultList();
		assertEquals(1, ids.size());
		Long deletedId = ids.get(0).longValue();

		assertNull(jpaDao.find(Book.class, deletedId), "逻辑删除的记录 find 应返回 null");
		// 正常记录不受影响
		List<Number> normalIds = seedEm.createNativeQuery(
				"select id from book where deleted = false order by id").getResultList();
		assertNotNull(jpaDao.find(Book.class, normalIds.get(0).longValue()));
	}

	@Test
	@DisplayName("findIn 过滤已删除记录")
	void findInFiltersDeleted() {
		seedWithDeleted();
		List<Book> result = jpaDao.findIn(Book.class, "name", List.of("正常书A", "已删除书"));
		assertEquals(1, result.size(), "已删除书应被过滤, 只剩正常书A");
		assertEquals("正常书A", result.get(0).getName());
	}

	@Test
	@DisplayName("deleteByPK 在逻辑删除开启时转成 update 置 deleted=true, 物理记录仍在")
	void deleteByPKSoftDeletes() {
		seedWithDeleted();
		assertEquals(3, countPhysical());

		List<Number> normalIds = seedEm.createNativeQuery(
				"select id from book where deleted = false order by id").getResultList();
		Long id = normalIds.get(0).longValue();

		jpaDao.begin();
		jpaDao.deleteByPK(Book.class, id);
		jpaDao.commit();

		// 物理记录还在(只是 deleted 置 true), 没有被物理删除
		assertEquals(3, countPhysical(), "逻辑删除不应物理删除记录");
		// 查询时 find 不到它了
		assertNull(jpaDao.find(Book.class, id), "删除后 find 应返回 null");
	}

	@Test
	@DisplayName("findBetween(LocalDateTime 重载) 在逻辑删除开启时也过滤已删除记录")
	void findBetweenFiltersDeleted() {
		seedWithDeleted();
		List<Book> result = jpaDao.findBetween(Book.class, "createTime",
				java.time.LocalDateTime.of(2000, 1, 1, 0, 0),
				java.time.LocalDateTime.of(2099, 1, 1, 0, 0));
		assertEquals(2, result.size(), "3 本都在时间区间, 但已删除那本应被过滤");
	}

	@Test
	@DisplayName("findBetween(Long 重载) 在逻辑删除开启时也过滤已删除记录")
	void findBetweenLongFiltersDeleted() {
		seedWithDeleted();
		List<Book> result = jpaDao.findBetween(Book.class, "price", 0L, 100L);
		assertEquals(2, result.size(), "3 本价格都在 0~100 区间, 但已删除那本应被过滤");
	}
}
