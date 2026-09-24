package com.awesomecopilot.cache;


import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Copyright: (C), 2022-01-26 14:13
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class JedisUtilsHashTest {
	
	@Test
	public void testDelete() throws InterruptedException {
		// field 过期时间 3 秒; 睡 5 秒后字段已过期, hdel 命中不到任何东西, 返回 0
		// (旧版本写 6 秒过期只睡 5 秒就断言 0, 字段根本没到期, 断言必红——评审发现 F8)
		JedisUtils.HASH.hset("idempotent-token", "token001", "somevalue", 3);
		String token = JedisUtils.HASH.hget("idempotent-token", "token001");
		assertEquals("somevalue", token);
		
		TimeUnit.SECONDS.sleep(5);

		Long deletedCount = JedisUtils.HASH.hdel("idempotent-token", "token001");
		System.out.println(deletedCount);
		assertTrue(deletedCount == 0);
	}
}