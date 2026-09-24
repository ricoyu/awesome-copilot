package com.awesomecopilot.orm.it;

import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.orm.it.entity.Shelf;
import com.awesomecopilot.orm.it.entity.ShelfItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页 count 与主查询 distinct 一致性回归（评审报告 I-10）。
 * <p>
 * 缺陷场景：主查询 join 一对多集合(root.join("items"))并按关联表条件过滤时,
 * 一个主表记录匹配多条子表记录会在 SQL 结果里出现多行, 主查询靠 distinct(true)
 * 去重(列表返回 2 条), 但分页 count 始终 count(countRoot) 按 join 后的物理行
 * 统计(返回 3) —— totalCount 偏大、页数算错。
 * <p>
 * 目标行为：count 与主查询用同一去重策略(主查询 distinct 时 count 用
 * countDistinct(root))。
 * <p/>
 * Copyright: Copyright (c) 2026-09-24
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
class PagingCountDistinctTest extends AbstractOrmIntegrationTest {

	/**
	 * A架3件 2XX、B架1件 2XX、C架 0 件。
	 * 按 item 条件 join 过滤: 匹配行 = A(3行) + B(1行) = 4 行, 去重后主表 2 条。
	 */
	private void seedShelves() {
		seedEm.getTransaction().begin();
		Shelf a = new Shelf();
		a.setName("A架");
		seedEm.persist(a);
		Shelf b = new Shelf();
		b.setName("B架");
		seedEm.persist(b);
		Shelf c = new Shelf();
		c.setName("C架");
		seedEm.persist(c);
		for (int i = 0; i < 3; i++) {
			ShelfItem item = new ShelfItem();
			item.setName("2XX商品" + i);
			item.setShelf(a);
			seedEm.persist(item);
		}
		ShelfItem bItem = new ShelfItem();
		bItem.setName("2XX商品B");
		bItem.setShelf(b);
		seedEm.persist(bItem);
		ShelfItem cItem = new ShelfItem();
		cItem.setName("9XX商品");
		cItem.setShelf(c);
		seedEm.persist(cItem);
		seedEm.getTransaction().commit();
	}

	@Test
	@DisplayName("join+distinct 分页: totalCount 必须等于去重后的主表条数")
	void totalCountMatchesDistinctJoinQuery() {
		seedShelves();
		Page page = new Page();
		page.setPageNum(1);
		page.setPageSize(10);

		// I-10 复现路径(RED 已用手动 join 版跑过: 列表 2 条 vs count 3)。
		// 修复方式: join 条件以"规格"保存, fillUp 时在主查询 root 与 count 查询 countRoot
		// 各自实例化——手写 Predicate 无法跨查询复用(Hibernate 报 Already registered a copy,
		// 见本类字段区注释), 所以框架提供 joinLike 这类一等 API。
		jakarta.persistence.EntityManager em = emf.createEntityManager();
		List<Shelf> results;
		try {
			com.awesomecopilot.orm.criteria.JPACriteriaQuery<Shelf> q =
					com.awesomecopilot.orm.criteria.JPACriteriaQuery.from(Shelf.class, em, false);
			q.joinLike("items", "name", "2XX%");
			q.setPage(page);
			results = q.list();
		} finally {
			em.close();
		}

		assertEquals(2, results.size(), "join 匹配 4 行, 主查询去重后应剩 2 个货架(A、B)");
		// 修复前: 手动 join 版实测 count=3(连条件都没进 count 查询) —— 缺陷复现点
		assertEquals(2, page.getTotalCount(),
				"totalCount 必须与去重后的列表条数一致(评审报告 I-10)");
		assertEquals(1, page.getTotalPages(), "2 条一页 10 条应为 1 页");
		assertTrue(results.stream().noneMatch(s -> s.getName().equals("C架")),
				"C架没有 2XX 商品, 不该出现");
	}

	@Test
	@DisplayName("普通分页(无 join)count 行为不变")
	void plainPagingCountUnchanged() {
		seedShelves();
		Page page = new Page();
		page.setPageNum(1);
		page.setPageSize(2);
		List<Shelf> results = jpaDao.query(Shelf.class).findPage(page);

		assertEquals(2, results.size(), "第一页 2 条");
		assertEquals(3, page.getTotalCount(), "总数仍是行数 3");
		assertEquals(2, page.getTotalPages(), "2 条一页共 2 页");
	}
}
