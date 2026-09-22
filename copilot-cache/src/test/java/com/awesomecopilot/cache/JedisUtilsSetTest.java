package com.awesomecopilot.cache;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Copyright: (C), 2023-02-09 11:40
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class JedisUtilsSetTest {

	@Test
	public void testSetAdd() {
		//先清场: 本测试断言的是"全新添加返回3", 如果 msg-ids 里有历史残留成员, sadd 会返回 0 导致假失败
		JedisUtils.del("msg-ids");
		long addedCount = JedisUtils.SET.sadd("msg-ids", "101", "102", "103");
		assertTrue(addedCount == 3);
		addedCount = JedisUtils.SET.sadd("msg-ids", "101", "102", "103");
		assertTrue(addedCount == 0);
		JedisUtils.del("msg-ids");
	}
}