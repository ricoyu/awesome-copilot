package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-10 回归测试（CODE_REVIEW_REPORT 2026-09-22）：时钟回拨的"虚拟时间"续发有跨进程重复 ID 的风险。
 * <p>
 * 场景：本进程经历回拨后进入虚拟时间模式，把时间戳推向未来发号。同 workerId 的另一进程
 * 从未经历回拨，挂钟真正走到那些未来毫秒时从 sequence=0 起步——两边在同一毫秒的
 * sequence 空间完全重叠，理论上可产出完全相同的 ID（单进程内探针无法复现，属逻辑推演）。
 * <p>
 * 修法：虚拟推进出来的毫秒，sequence 不从 0 起步而从 2048（sequenceMask/2）起步，
 * 与对端拉开半个 sequence 空间的距离。本用例把挂钟固定在回拨点：此后任何高于回拨前
 * 最大时间戳的毫秒都只能是虚拟推进出来的，其全部 sequence 必须 ≥2048。
 * 修复前实测：虚拟毫秒从 0 起步，断言必挂。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class SnowflakeIdVirtualSequenceTest {

	private static class ControlledClockId extends SnowflakeId {
		final AtomicLong clock;

		ControlledClockId(long startMillis) {
			super(2, 2);
			this.clock = new AtomicLong(startMillis);
		}

		@Override
		protected long getTimeMillis() {
			return clock.get();
		}
	}

	private static long timestampOf(long id) {
		return id >>> 22; //高 41 位是 (timestamp - twepoch)，低 22 位是 datacenter+worker+sequence
	}

	private static long sequenceOf(long id) {
		return id & 4095L;
	}

	@Test
	public void testVirtualMillisecondNeverUsesLowSequenceHalf() {
		long real = 1_789_308_343_000L;
		ControlledClockId id = new ControlledClockId(real);

		Set<Long> all = new HashSet<>();
		long maxNormalTs = -1;
		for (int i = 0; i < 10; i++) {
			long next = id.nextId();
			all.add(next);
			maxNormalTs = Math.max(maxNormalTs, timestampOf(next));
		} //ID 里存的是 (millis - twepoch), 直接以已发号的最大 ts 为"正常毫秒水位"基准

		//回拨5秒(>50ms 不等待, 直接虚拟时间续发), 之后挂钟固定不动
		id.clock.set(id.lastTimestamp - 5000);

		int virtualSamples = 0;
		for (int i = 0; i < 5000; i++) { //5000 > 单毫秒容量4096, 必触发虚拟毫秒推进
			long next = id.nextId();
			all.add(next);
			if (timestampOf(next) > maxNormalTs) {
				virtualSamples++;
				assertThat(sequenceOf(next))
						.as("虚拟毫秒 %d 的 sequence 必须从 2048 起步, 与未经历回拨的同 workerId 进程(从0起步)拉开距离",
								timestampOf(next))
						.isGreaterThanOrEqualTo(2048);
			}
		}
		assertThat(virtualSamples).as("5000 次发号必然覆盖到虚拟推进的毫秒").isPositive();
		assertThat(all).hasSize(5010); //单进程内唯一性不回退
	}

	@Test
	public void testNormalModeStillStartsSequenceAtZero() {
		//正常模式(无回拨)的毫秒序列仍从 0 起步, 收紧只作用于虚拟推进
		long real = 1_789_308_343_000L;
		ControlledClockId id = new ControlledClockId(real);
		long first = id.nextId();
		assertThat(sequenceOf(first)).isZero();
	}
}
