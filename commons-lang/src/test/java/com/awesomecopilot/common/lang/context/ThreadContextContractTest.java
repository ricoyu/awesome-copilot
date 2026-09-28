package com.awesomecopilot.common.lang.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-35(CODE_REVIEW_REPORT): ThreadContext 两处语义陷阱以 javadoc 修正方式处置,
 * 本测试把修正后的文档契约固定下来, 防止后续改动无意翻转语义:
 * <ol>
 * <li>{@code getResources()} 返回拷贝——在返回值上 put 不影响真实绑定, 修改必须走 put();</li>
 * <li>{@code setResources(null/emptyMap)} 是空操作——已有绑定保持不变, 清空要用 remove()。</li>
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
public class ThreadContextContractTest {

	@AfterEach
	public void cleanup() {
		ThreadContext.remove();
	}

	@Test
	public void testGetResourcesReturnsCopyPutOnCopyDoesNotBind() {
		Map<Object, Object> emptyView = ThreadContext.getResources();
		// 未绑定时返回不可变空表, 对它 put 直接抛 UnsupportedOperationException
		assertThat(emptyView).isEmpty();
		org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
				() -> emptyView.put("k0", "v0"));

		// 已有绑定时 getResources() 返回可变拷贝——陷阱写法: 在拷贝上 put 编译通过、
		// 值不进入真实绑定(语义陷阱, 文档已按此修正)
		ThreadContext.put("k2", "v2");
		Map<Object, Object> copy = ThreadContext.getResources();
		copy.put("k1", "v1");
		// ThreadContext.get 返回无界泛型 T, 不显式转 Object 会让 assertThat 重载歧义
		assertThat((Object) ThreadContext.get("k1")).isNull();
		assertThat(ThreadContext.getResources()).containsEntry("k2", "v2").doesNotContainKey("k1");
	}

	@Test
	public void testSetResourcesIgnoresNullOrEmptyAndRemoveClears() {
		ThreadContext.put("k", "v");

		ThreadContext.setResources(new HashMap<>());
		assertThat((Object) ThreadContext.get("k")).isEqualTo("v"); // 空 Map 是空操作, 不清空
		ThreadContext.setResources(null);
		assertThat((Object) ThreadContext.get("k")).isEqualTo("v"); // null 同样是空操作

		// 非空 Map 才整体替换
		Map<Object, Object> replacement = new HashMap<>();
		replacement.put("other", 1);
		ThreadContext.setResources(replacement);
		assertThat((Object) ThreadContext.get("k")).isNull();
		assertThat((Object) ThreadContext.get("other")).isEqualTo(1);

		// 清空走 remove()
		ThreadContext.remove();
		assertThat(ThreadContext.getResources()).isEmpty();
	}
}
