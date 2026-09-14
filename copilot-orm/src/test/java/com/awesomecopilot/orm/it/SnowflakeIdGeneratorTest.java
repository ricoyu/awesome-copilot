package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.it.entity.UserOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 雪花ID主键生成器集成测试: UserOrder 继承 BaseEntityShowflake,
 * 主键注解为 @GeneratedValue(generator="snowflake-id") +
 * {@code @GenericGenerator(type = CopilotSnowflakeIdGenerator.class)},
 * 走真实 Hibernate persist 链路, 验证生成器与 JPA 的装配是否工作。
 *
 * @author Rico Yu
 */
class SnowflakeIdGeneratorTest extends AbstractOrmIntegrationTest {

	/** SnowflakeId 的 twepoch(2015-01-01) 与位结构: 41位时间戳 << 22 | 5位datacenter << 17 | 5位worker << 12 | 12位序列 */
	private static final long TWEPOCH = 1420041600000L;

	@Test
	@DisplayName("persist 后主键由雪花生成器赋值为正长整数, 且不必 flush 就已有值")
	void persistAssignsSnowflakeId() {
		UserOrder order = new UserOrder("NO-001", new BigDecimal("99.90"));
		jpaDao.begin();
		jpaDao.persist(order);
		jpaDao.commit();

		assertNotNull(order.getId(), "persist 后应自动赋值主键");
		assertTrue(order.getId() > 0, "最高位符号位应为0, ID是正数");

		// 解码时间戳段: 应接近当前时间(误差容忍10秒)
		long ts = (order.getId() >> 22) + TWEPOCH;
		long skew = Math.abs(System.currentTimeMillis() - ts);
		assertTrue(skew < 10_000, "ID内嵌时间戳应接近当前时间, 实际偏差" + skew + "ms");

		// 落库后按主键查回
		UserOrder loaded = jpaDao.get(UserOrder.class, order.getId());
		assertNotNull(loaded);
		assertEquals("NO-001", loaded.getOrderNo());
	}

	@Test
	@DisplayName("批量 persist 500 条, 雪花主键互不重复且随时间单调不减")
	void batchPersistGeneratesUniqueIds() {
		List<UserOrder> orders = new ArrayList<>();
		for (int i = 0; i < 500; i++) {
			orders.add(new UserOrder("NO-" + i, BigDecimal.ONE));
		}
		jpaDao.begin();
		jpaDao.persist(orders);
		jpaDao.commit();

		Set<Long> ids = new HashSet<>();
		Long prev = null;
		for (UserOrder o : orders) {
			assertNotNull(o.getId(), "每条都应被赋值主键: " + o.getOrderNo());
			assertTrue(ids.add(o.getId()), "出现重复主键: " + o.getId());
			if (prev != null) {
				assertTrue(o.getId() >= prev, "同一生成器连续发号应单调不减");
			}
			prev = o.getId();
		}
		assertEquals(500, ids.size());
		assertEquals(500, jpaDao.findAll(UserOrder.class).size(), "500条应全部落库");
	}

	@Test
	@DisplayName("save: id 为 null 走 persist(生成雪花号); 已有 id 再 save 走 merge 不新增记录")
	void saveWithAndWithoutId() {
		UserOrder order = new UserOrder("NO-SAVE", BigDecimal.TEN);
		jpaDao.begin();
		UserOrder saved = jpaDao.save(order);
		jpaDao.commit();
		assertNotNull(saved.getId(), "save 新实体应生成雪花主键");

		jpaDao.begin();
		saved.setAmount(new BigDecimal("20"));
		UserOrder merged = jpaDao.save(saved);
		jpaDao.commit();

		assertEquals(saved.getId(), merged.getId(), "已有id时save应更新而非换号");
		List<UserOrder> all = jpaDao.findAll(UserOrder.class);
		assertEquals(1, all.size(), "已有id时save不应新增记录");
	}

	@Test
	@DisplayName("机器位解码: datacenterId 与 workerId 应落在合法范围 0~31")
	void machineBitsInRange() {
		UserOrder order = new UserOrder("NO-BITS", BigDecimal.ONE);
		jpaDao.begin();
		jpaDao.persist(order);
		jpaDao.commit();

		long workerId = (order.getId() >> 12) & 31;
		long datacenterId = (order.getId() >> 17) & 31;
		assertTrue(workerId >= 0 && workerId <= 31);
		assertTrue(datacenterId >= 0 && datacenterId <= 31);
	}
}
