package com.awesomecopilot.common.lang.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-3 回归测试（CODE_REVIEW_REPORT 2026-09-22）：CopilotLinkedBlockingQueue 的
 * Spliterator 缺少 JDK 原版的"自指节点"判断（dequeue 会把旧节点 next 指向自己）。
 * <p>
 * 修复前实测：spliterator 消费一部分后，其它线程把队列取空，节点变成自指——
 * tryAdvance/forEachRemaining/trySplit 的循环 current = current.next 在自己身上
 * 绕圈永不退出，且该线程持有 fullyLock（putLock+takeLock），整个队列读写全部阻塞。
 * <p>
 * 本测试用守护线程 + 限时等待来复现：修复前线程 3 秒不结束（判失败），
 * 修复后应立即返回。
 *
 * <p>
 * Copyright: (C), 2026-09-23
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class CopilotLinkedBlockingQueueSpliteratorTest {

	/**
	 * 在独立线程里跑 action，3 秒内没结束即视为挂死。
	 * 挂死线程做成 daemon，测试结束后不会拖住 JVM。
	 */
	private boolean completesWithin3s(Runnable action) throws InterruptedException {
		AtomicBoolean finished = new AtomicBoolean(false);
		Thread t = new Thread(() -> {
			action.run();
			finished.set(true);
		}, "spliterator-probe");
		t.setDaemon(true);
		t.start();
		t.join(3000);
		return finished.get();
	}

	@Test
	public void testTryAdvanceAfterConcurrentDrainTerminates() throws InterruptedException {
		CopilotLinkedBlockingQueue<Integer> queue = new CopilotLinkedBlockingQueue<>();
		for (int i = 1; i <= 3; i++) queue.add(i);

		Spliterator<Integer> spliterator = queue.spliterator();
		AtomicInteger first = new AtomicInteger();
		spliterator.tryAdvance(first::set); //消费 1，游标停在节点 n2
		assertThat(first.get()).isEqualTo(1);

		//三次 poll 后：n2 被消费，dequeue 把 n2.next 指向 n2 自己（自指）
		queue.poll();
		queue.poll();
		queue.poll();

		//修复前实测：这次 tryAdvance 在 n2 上绕圈 3 秒不返回，且持有 putLock+takeLock
		AtomicBoolean completed = new AtomicBoolean();
		boolean ok = completesWithin3s(() -> completed.set(spliterator.tryAdvance(x -> {
		})));
		assertThat(ok).as("tryAdvance 遇到自指节点应结束而不是死循环").isTrue();
		assertThat(completed.get()).isFalse(); //队列已空，应返回 false

		//死循环线程若还持有两把锁，这里 put 会阻塞——验证队列读写恢复正常
		boolean putOk = completesWithin3s(() -> {
			try {
				queue.put(9);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		assertThat(putOk).as("Spliterator 遍历后队列读写不应被阻塞").isTrue();
	}

	@Test
	public void testForEachRemainingAfterConcurrentDrainTerminates() throws InterruptedException {
		CopilotLinkedBlockingQueue<Integer> queue = new CopilotLinkedBlockingQueue<>();
		for (int i = 1; i <= 3; i++) queue.add(i);

		Spliterator<Integer> spliterator = queue.spliterator();
		AtomicInteger first = new AtomicInteger();
		spliterator.tryAdvance(first::set); //游标停在 n2
		queue.poll();
		queue.poll();
		queue.poll(); //n2 成为自指节点

		boolean ok = completesWithin3s(() -> spliterator.forEachRemaining(x -> {
		}));
		assertThat(ok).as("forEachRemaining 遇到自指节点应结束而不是死循环").isTrue();
	}

	@Test
	public void testTrySplitAfterConcurrentDrainTerminates() throws InterruptedException {
		CopilotLinkedBlockingQueue<Integer> queue = new CopilotLinkedBlockingQueue<>();
		for (int i = 1; i <= 3; i++) queue.add(i);

		Spliterator<Integer> spliterator = queue.spliterator();
		AtomicInteger first = new AtomicInteger();
		spliterator.tryAdvance(first::set); //游标停在 n2
		queue.poll();
		queue.poll();
		queue.poll(); //n2 成为自指节点

		AtomicBoolean completed = new AtomicBoolean();
		boolean ok = completesWithin3s(() -> {
			spliterator.trySplit();
			completed.set(true);
		});
		assertThat(ok).as("trySplit 遇到自指节点应结束而不是死循环").isTrue();
		//挂死场景修复后必然能走到这里
		assertThat(completed.get()).isTrue();
	}

	@Test
	public void testNormalTraversalsStillWork() {
		//合法路径不能被改坏：无竞争下 tryAdvance / forEachRemaining / trySplit 的元素不丢不重
		CopilotLinkedBlockingQueue<Integer> queue = new CopilotLinkedBlockingQueue<>();
		for (int i = 1; i <= 10; i++) queue.add(i);

		Spliterator<Integer> spliterator = queue.spliterator();
		assertThat(spliterator.estimateSize()).isEqualTo(10);
		Spliterator<Integer> prefix = spliterator.trySplit();
		assertThat(prefix).isNotNull();

		AtomicInteger count = new AtomicInteger();
		spliterator.forEachRemaining(x -> count.incrementAndGet());
		prefix.forEachRemaining(x -> count.incrementAndGet());
		assertThat(count.get()).as("两半合起来应恰好覆盖 10 个元素").isEqualTo(10);
	}
}
