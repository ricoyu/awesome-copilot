package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.Shipment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 复合主键实体的批量 merge 回归（独立评审 deleg_df7e37a8 发现 F1）。
 * <p>
 * 缺陷：merge(List) 的"IN 预热"要解析单列主键名(resolveIdAttributeName)，
 * 该方法对复合主键(多 @Id)故意抛 IllegalArgumentException(为 deleteByPK 设计的
 * 守卫)——但预热只是优化，它把复合主键实体整批 merge 从"可用"变成了"必抛
 * PersistenceException"。em.merge 本身对复合主键完全合法(JPA 标准)。
 * <p>
 * 期望：复合主键实体的 merge(List)/save(List) 正常工作；预热对它自动跳过
 * (退回逐条 merge 的原生行为)。
 *
 * @author Rico Yu
 */
class CompositeKeyMergeTest extends AbstractOrmIntegrationTest {

	private Shipment shipment(long partNo, int seqNo, String name) {
		return new Shipment(partNo, seqNo, name);
	}

	@Test
	@DisplayName("复合主键实体 merge(List) 不抛异常且真实落库")
	void mergeListOnCompositeKeyEntityWorks() {
		// 先落两条, 再改名字批量 merge
		jpaDao.begin();
		try {
			jpaDao.merge(shipment(100L, 1, "原A"));
			jpaDao.merge(shipment(100L, 2, "原B"));
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) {
				jpaDao.rollback();
			}
		}

		List<Shipment> updates = new ArrayList<>();
		updates.add(shipment(100L, 1, "改A"));
		updates.add(shipment(100L, 2, "改B"));

		jpaDao.begin();
		try {
			assertDoesNotThrow(() -> jpaDao.merge(updates),
					"复合主键批量 merge 不应被预热守卫误伤(评审 F1)");
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) {
				jpaDao.rollback();
			}
		}

		Shipment check = seedEm.find(Shipment.class, new Shipment.ShipmentKey(100L, 1));
		assertNotNull(check, "merge 后记录应存在");
		assertEquals("改A", check.getName(), "字段更新必须真实落库");
	}

	@Test
	@DisplayName("复合主键实体 save(List)(带主键=更新路径)不抛异常")
	void saveListOnCompositeKeyEntityWorks() {
		List<Shipment> fresh = new ArrayList<>();
		fresh.add(shipment(200L, 1, "新1"));
		fresh.add(shipment(200L, 2, "新2"));

		jpaDao.begin();
		try {
			assertDoesNotThrow(() -> jpaDao.save(fresh),
					"复合主键批量 save 不应抛异常");
			jpaDao.commit();
		} finally {
			if (jpaDao.em().getTransaction().isActive()) {
				jpaDao.rollback();
			}
		}
		// doSave 对"主键字段非 null 但记录不存在"走 merge: Hibernate 会 SELECT→INSERT, 也应成功
		Shipment check = seedEm.find(Shipment.class, new Shipment.ShipmentKey(200L, 1));
		assertNotNull(check, "save 后记录应存在");
	}
}
