package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 逻辑删除字段为 int 类型(0/1)的场景, 验证 P2-12 修复:
 * deleteByPK 写"已删除"标记时按字段实际类型取值(int → 1),
 * 旧代码硬编码 set(true) 会造成类型错配、查询过滤失效。
 *
 * @author Rico Yu
 */
class IntLogicalDeleteTest extends AbstractOrmIntegrationTest {

	@BeforeEach
	void enableLogicalDelete() {
		inject(jpaDao, "logicalDeleteEnabled", true);
		inject(jpaDao, "logicalDeleteField", "deleted");
	}

	private void seedProducts() {
		seedEm.getTransaction().begin();
		seedEm.createNativeQuery(
				"insert into product(name, deleted) values ('正常商品', 0), ('已删商品', 1)")
				.executeUpdate();
		seedEm.getTransaction().commit();
	}

	@Test
	@DisplayName("int 型 deleted: deleteByPK 写入 1 而不是 true, 类型不错配")
	void deleteByPkWritesIntOne() {
		seedProducts();
		// 找到正常商品的 id
		List<Number> ids = seedEm.createNativeQuery(
				"select id from product where name = '正常商品'").getResultList();
		Long id = ids.get(0).longValue();

		jpaDao.begin();
		jpaDao.deleteByPK(Product.class, id);
		jpaDao.commit();

		// 物理记录还在, deleted 被置成整数 1(不是 true)
		Object deletedVal = seedEm.createNativeQuery(
				"select deleted from product where id = " + id).getSingleResult();
		assertEquals(1, ((Number) deletedVal).intValue(),
				"int 型逻辑删除字段应写入整数 1, 旧代码硬编码 true 会类型错配");
	}

	@Test
	@DisplayName("int 型 deleted: find 按 0 过滤, 已删(1)的查不到")
	void findFiltersIntDeleted() {
		seedProducts();
		List<Number> deletedIds = seedEm.createNativeQuery(
				"select id from product where deleted = 1").getResultList();
		Long deletedId = deletedIds.get(0).longValue();
		List<Number> normalIds = seedEm.createNativeQuery(
				"select id from product where deleted = 0").getResultList();
		Long normalId = normalIds.get(0).longValue();

		// 已删除(deleted=1)的 find 不到
		assertNull(jpaDao.find(Product.class, deletedId), "deleted=1 应被过滤");
		// 正常(deleted=0)的能查到
		assertNotNull(jpaDao.find(Product.class, normalId), "deleted=0 应正常返回");
	}

	@Test
	@DisplayName("int 型 deleted: findIn 过滤已删除记录")
	void findInFiltersIntDeleted() {
		seedProducts();
		List<Product> result = jpaDao.findIn(Product.class, "name",
				List.of("正常商品", "已删商品"));
		assertEquals(1, result.size(), "已删商品(deleted=1)应被过滤");
		assertEquals("正常商品", result.get(0).getName());
	}
}
