package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P2-38(CODE_REVIEW_REPORT) 三处雪花ID设计风险的回归测试:
 * <ol>
 * <li>AUTO_SLOTS 只登记不回收 → 实例被回收后槽位应随之释放, 自动推导构造不再受
 *     "同JVM累计第33个必失败"的上限约束;
 * <li>轻微回拨(≤50ms)在 nextId 的 synchronized 临界区内 sleep 等待 → 去掉等待分支,
 *     回拨≤60s 一律立即虚拟时间续发, 取号线程不再在同一把锁上排队最多1秒;
 * <li>自动推导只登记 JVM 内槽位, 同机另一进程(重启后会拿同一推导值)无检测 → 槽位认领前
 *     先申请跨进程租约(默认实现为 tmp 目录文件锁, protected 接缝供测试注入),
 *     租约被占则顺延, 32 槽全被占则构造直接失败, 不再不带任何提示地带病发号。
 * </ol>
 * <p>
 * Copyright: Copyright (c) 2026-09-27
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SnowflakeP238FixTest {

	/**
	 * 风险1: 修复前构造第33个自动推导实例必抛 IllegalStateException(登记表只进不出,
	 * 旧实例被回收也不释放槽位)。修复后每轮实例断开引用并回收, 槽位被 Cleaner 释放,
	 * 循环 40 轮(>32)不再失败。
	 */
	@Test
	public void testAutoSlotReleasedAfterInstanceGc() throws InterruptedException {
		System.clearProperty("copilot.snowflake.worker-id");
		System.clearProperty("copilot.snowflake.datacenter-id");
		SnowflakeId holder = null;
		for (int round = 0; round < 40; round++) {
			holder = new SnowflakeId();
			holder = null;
			System.gc();
			// Cleaner 的释放动作在后台线程执行, 给一个短窗口; 偶有不及由下一轮顺延槽位兜住
			Thread.sleep(5);
		}
	}

	/**
	 * 风险2: 挂钟停走 + 回拨30ms(≤50ms档)。修复前在临界区内 sleep(1) 循环等待,
	 * 首调用约1秒才降级返回; 修复后应立即虚拟时间续发(纯内存), 且不产生重复ID。
	 */
	@Test
	public void testMinorBackwardNeverWaitsInsideLock() {
		SnowflakeIdTest.ControlledClockId id = new SnowflakeIdTest.ControlledClockId(1_789_308_343_000L);
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 5; i++) ids.add(id.nextId());
		// 模拟回拨30ms: 把 lastTimestamp 推高30ms, 挂钟保持不动
		id.lastTimestamp += 30;

		long start = System.nanoTime();
		for (int i = 0; i < 10; i++) ids.add(id.nextId());
		long costMs = (System.nanoTime() - start) / 1_000_000;

		assertThat(ids).hasSize(15); // 立即续发也不允许重复
		assertThat(costMs).isLessThan(200L); // 修复前这里约1000ms
	}

	/**
	 * 风险2不误伤: 正常模式(无回拨)下序列溢出仍应阻塞到下一毫秒, 5000 个连发(>4096)
	 * 不重复、不丢号——既有正确行为在改动后保持。
	 */
	@Test
	public void testNormalOverflowStillUnique() {
		SnowflakeId id = new SnowflakeId(5, 5);
		Set<Long> ids = new HashSet<>();
		long start = System.nanoTime();
		for (int i = 0; i < 5000; i++) ids.add(id.nextId());
		long costMs = (System.nanoTime() - start) / 1_000_000;
		assertThat(ids).hasSize(5000);
		assertThat(costMs).isLessThan(5000L); // 每毫秒4096个, 5000个最多阻塞2毫秒量级
	}

	/**
	 * 风险3接缝: 租约被同机其它进程占用的槽位应跳过, 顺延到第一个可租槽位。
	 * 模拟"推导值和顺延第1个槽位都被别的进程租走"。
	 */
	@Test
	public void testLeaseHeldByOtherProcessDefersToFreeSlot() throws Exception {
		System.clearProperty("copilot.snowflake.worker-id");
		System.clearProperty("copilot.snowflake.datacenter-id");
		// 评审修复(2026-09-28): 清掉前面 GC 测试可能滞留的登记槽, 让"顺延到 derived+2"
		// 的断言不依赖 Cleaner 后台线程的时序(CI 上 GC 行为不同, 滞留槽若恰是 derived+2 本断言会崩)
		Field autoSlots = SnowflakeId.class.getDeclaredField("AUTO_SLOTS");
		autoSlots.setAccessible(true);
		((Set<?>) autoSlots.get(null)).clear();
		long derived = WorkerIdGenerator.generateWorkerIdFromIp() & 31L;
		long dc = 1L; // 无参构造器 datacenterId 缺省为 1
		try {
			LeaseControlledId.triedSlots.clear();
			LeaseControlledId.leasedByOthers = new HashSet<>();
			LeaseControlledId.leasedByOthers.add(dc * 32 + derived);
			LeaseControlledId.leasedByOthers.add(dc * 32 + ((derived + 1) & 31));

			LeaseControlledId a = new LeaseControlledId();

			assertThat(LeaseControlledId.triedSlots).containsExactly(
					dc * 32 + derived,
					dc * 32 + ((derived + 1) & 31),
					dc * 32 + ((derived + 2) & 31));
			assertThat(a.workerId).isEqualTo((derived + 2) & 31);
		} finally {
			LeaseControlledId.leasedByOthers = null;
		}
	}

	/**
	 * 风险3接缝: 32 个槽位的租约全被同机其它进程持有时, 构造直接抛异常,
	 * 不允许不带任何提示地共用槽位导致跨进程重复ID。
	 */
	@Test
	public void testAllSlotsLeasedByOthersFailsFast() {
		try {
			Set<Long> all = new HashSet<>();
			for (long w = 0; w <= 31; w++) all.add(1L * 32 + w);
			LeaseControlledId.leasedByOthers = all;
			assertThatThrownBy(LeaseControlledId::new)
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("copilot.snowflake.worker-id");
		} finally {
			LeaseControlledId.leasedByOthers = null;
		}
	}

	/**
	 * 评审补漏(2026-09-28): 显式配置实例也要申请槽位租约——修复前租约只在自动推导分支
	 * 申请, 同机"显式配了 worker-id=N 的进程"与"自动推导恰好撞到 N 的进程"并行发号无人
	 * 检测, 跨进程重复 ID 风险仍漏掉这一半。显式实例租不到时不顺延、不抛异常(编号由配置者
	 * 指定, 擅自改动违反配置), 只 WARN 可能重号。
	 */
	@Test
	public void testExplicitWorkerIdAlsoAcquiresLeaseButNeverDefers() {
		try {
			LeaseControlledId.triedSlots.clear();
			LeaseControlledId.leasedByOthers = new HashSet<>();
			LeaseControlledId.leasedByOthers.add(1L * 32 + 3); // 模拟 workerId=3, datacenterId=1 已被别的进程租走

			LeaseControlledId explicit = new LeaseControlledId(3L, 1L);

			assertThat(LeaseControlledId.triedSlots).contains(1L * 32 + 3); // 显式路径也探测了租约
			assertThat(explicit.workerId).isEqualTo(3L);                    // 不顺延: 编号保持配置值
			assertThat(explicit.datacenterId).isEqualTo(1L);
		} finally {
			LeaseControlledId.leasedByOthers = null;
		}
	}

	/**
	 * 租约可控的子类: LEASED 里的槽位模拟"被同机其它进程占用"
	 * (acquireSlotLease 返回 null 表示租不到)。
	 * 注意: 认领发生在父类构造器里, 子类实例字段那时还未赋值, 所以注入状态必须是 static 的。
	 */
	static class LeaseControlledId extends SnowflakeId {
		static Set<Long> leasedByOthers;
		static List<Long> triedSlots = new ArrayList<>();

		LeaseControlledId() {
			super();
		}

		LeaseControlledId(long workerId, long datacenterId) {
			super(workerId, datacenterId);
		}

		@Override
		protected AutoCloseable acquireSlotLease(long slot) {
			triedSlots.add(slot);
			if (leasedByOthers != null && leasedByOthers.contains(slot)) {
				return null;
			}
			return () -> {
			};
		}
	}
}
