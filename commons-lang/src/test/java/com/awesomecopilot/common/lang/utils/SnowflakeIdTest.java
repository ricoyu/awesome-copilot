package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * SnowflakeId 时钟跳变场景测试。
 * 通过覆盖 getTimeMillis() 注入可控时钟, 不需要改系统时间。
 * <p/>
 * Copyright: Copyright (c) 2026-09-13
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class SnowflakeIdTest {
	
	/**
	 * 可控时钟的生成器: 时钟值由外部 AtomicLong 决定, 随时向前/向后拨
	 */
	static class ControlledClockId extends SnowflakeId {
		final AtomicLong clock;
		
		ControlledClockId(long startMillis) {
			super(1, 1);
			this.clock = new AtomicLong(startMillis);
		}
		
		@Override
		protected long getTimeMillis() {
			return clock.get();
		}
	}
	
	@Test
	public void testNormalGeneration() {
		SnowflakeId id = new SnowflakeId(31, 7);
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			assertThat(ids.add(id.nextId())).isTrue();
		}
	}
	
	/**
	 * P0场景: 挂钟被向前跳10秒(手动改表/NTP大步校正), lastTimestamp 被写成未来值;
	 * 挂钟纠正回来后, 旧实现会一直抛"时钟回拨"异常停摆到挂钟重新追上那个未来值,
	 * 新实现应立即用虚拟时间续发、不再停摆, 且ID不重复。
	 */
	@Test
	public void testClockJumpedForwardThenCorrected() {
		long real = 1_789_308_343_000L; // 固定起点, 不依赖真实时间
		ControlledClockId id = new ControlledClockId(real);
		long before = id.nextId();
		
		// 时钟向前跳10秒, 生成几个ID(lastTimestamp 写到未来)
		id.clock.addAndGet(10_000);
		Set<Long> generated = new HashSet<>();
		generated.add(before);
		for (int i = 0; i < 5; i++) generated.add(id.nextId());
		
		// 时钟纠正回真实值(相对lastTimestamp 表现为回拨10秒)
		id.clock.set(real + 20);
		// 修复前: 这里抛 RuntimeException("系统时钟回拨超出容忍范围"); 修复后: 正常出号
		assertThatCodeDoesNotThrow(() -> {
			for (int i = 0; i < 100; i++) generated.add(id.nextId());
		});
		assertThat(generated).hasSize(106); // 5+1+100 全部唯一
		
		// 虚拟时间把 lastTimestamp 继续推高时(序列溢出), 也不能和挂钟追上后混出重复:
		// 把挂钟再往后拨到超过所有已发虚拟时间戳, 继续出号仍唯一
		id.clock.set(real + 60_000);
		for (int i = 0; i < 1000; i++) {
			assertThat(generated.add(id.nextId())).isTrue();
		}
	}
	
	/**
	 * 回拨≤50ms 且挂钟很快回头: 等待后恢复, 不出重复ID (保留旧行为)
	 */
	@Test
	public void testSmallBackwardWaitRecover() {
		ControlledClockId id = new ControlledClockId(System.currentTimeMillis()) {
			@Override
			protected long getTimeMillis() {
				return clock.addAndGet(1); // 每读一次时钟向前走1ms, 模拟挂钟正常走动
			}
		};
		long first = id.nextId();
		long last = id.lastTimestamp;
		// 回拨30ms: 把lastTimestamp推后30ms, 挂钟每取一次走1ms, 约30次读取后追上
		id.lastTimestamp = last + 30;
		Set<Long> ids = new HashSet<>();
		ids.add(first);
		long start = System.nanoTime();
		for (int i = 0; i < 10; i++) ids.add(id.nextId());
		long costMs = (System.nanoTime() - start) / 1_000_000;
		assertThat(ids).hasSize(11);
		assertThat(costMs).isLessThan(900L); // 靠等待恢复, 没走到1秒超时降级
	}
	
	/**
	 * 回拨≤50ms 但挂钟永远不回头: 等待超时后降级虚拟时间续发, 不再抛异常
	 * (旧实现: 1秒后抛"等待超时, 拒绝生成ID")
	 */
	@Test
	public void testSmallBackwardWaitTimeoutDegrades() {
		ControlledClockId id = new ControlledClockId(1_789_308_343_000L);
		long first = id.nextId();
		id.lastTimestamp += 30;
		Set<Long> ids = new HashSet<>();
		ids.add(first);
		long start = System.nanoTime();
		for (int i = 0; i < 5; i++) ids.add(id.nextId()); // 时钟不动, 第一次会等约1秒后降级
		long waitedMs = (System.nanoTime() - start) / 1_000_000;
		assertThat(ids).hasSize(6);
		assertThat(waitedMs).isBetween(900L, 3000L); // 确实经历了等待窗口
	}
	
	/**
	 * 回拨在50ms~60s之间: 不等待、不抛异常, 立即虚拟时间续发
	 */
	@Test
	public void testMediumBackwardVirtualCompensation() {
		ControlledClockId id = new ControlledClockId(1_789_308_343_000L);
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 10; i++) ids.add(id.nextId());
		
		// 一次取号后模拟回拨5秒: 挂钟停在原值, lastTimestamp 人为推后5秒等价于挂钟提前5秒
		long last = id.lastTimestamp;
		id.clock.set(last - 5000);
		long start = System.nanoTime();
		for (int i = 0; i < 5000; i++) { // 5000 > 单毫秒序列容量4096, 触发虚拟时间+1毫秒
			ids.add(id.nextId());
		}
		long costMs = (System.nanoTime() - start) / 1_000_000;
		assertThat(ids).hasSize(5010);
		assertThat(costMs).isLessThan(1000L); // 纯内存操作, 不等挂钟不睡眠
	}
	
	/**
	 * 回拨超过60秒: 可能是虚机快照恢复, 抛异常并说清原因
	 */
	@Test
	public void testHugeBackwardThrows() {
		ControlledClockId id = new ControlledClockId(1_789_308_343_000L);
		id.nextId();
		long last = id.lastTimestamp;
		id.clock.set(last - 120_000); // 回拨2分钟
		assertThatThrownBy(id::nextId)
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("120000");
	}
	
	/**
	 * P1验证1: 同JVM内两个自动推导实例(无配置, 推导值相同)不应共用同一workerId——
	 * 第二个实例应顺延到空闲槽位, 两者同毫秒生成的ID不重复
	 */
	@Test
	public void testAutoWorkerIdCollisionDefersWithinJvm() {
		System.clearProperty("copilot.snowflake.worker-id");
		SnowflakeId a = new SnowflakeId();
		SnowflakeId b = new SnowflakeId();
		assertThat(a.workerId == b.workerId && a.datacenterId == b.datacenterId).isFalse();
		
		// 各自连发2000个, 合并后不允许任何重复(同槽位不互查序列的话同毫秒必撞)
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 2000; i++) {
			ids.add(a.nextId());
			ids.add(b.nextId());
		}
		assertThat(ids).hasSize(4000);
	}
	
	/**
	 * P1验证2: 系统属性优先级最高, 显式配置时不触发自动推导
	 */
	@Test
	public void testSystemPropertyOverridesAutoAssignment() {
		System.setProperty("copilot.snowflake.worker-id", "7");
		System.setProperty("copilot.snowflake.datacenter-id", "3");
		try {
			SnowflakeId id = new SnowflakeId();
			assertThat(id.workerId).isEqualTo(7);
			assertThat(id.datacenterId).isEqualTo(3);
		} finally {
			System.clearProperty("copilot.snowflake.worker-id");
			System.clearProperty("copilot.snowflake.datacenter-id");
		}
	}
	
	private void assertThatCodeDoesNotThrow(ThrowingRunnable r) {
		try {
			r.run();
		} catch (Throwable e) {
			fail("不应抛异常, 但抛了: " + e);
		}
	}
	
	@FunctionalInterface
	interface ThrowingRunnable {
		void run() throws Throwable;
	}
}
