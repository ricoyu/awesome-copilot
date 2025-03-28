package com.awesomecopilot.cache;

import org.junit.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * <p>
 * Copyright: (C), 2019/11/9 21:10
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class JedisUitlsClusterTest {
	
	@Test
	public void testSetGet() {
		System.out.println(JedisUtils.set("name", "俞雪华"));
		assertEquals("俞雪华", JedisUtils.get("name"));
	}
	
	@Test
	public void testSetNx() {
		System.out.println(JedisUtils.setnx("aaa", "111", 1000, TimeUnit.SECONDS));
		System.out.println(JedisUtils.setnx("aaa", "111", 1000, TimeUnit.SECONDS));
		System.out.println(JedisUtils.setnx("yyy", "111", 1000, TimeUnit.SECONDS));
		System.out.println(JedisUtils.setnx("xxx", "111", 1000, TimeUnit.SECONDS));
		System.out.println(JedisUtils.setnx("年好吗奥术大师多", "111", 1000, TimeUnit.SECONDS));
		System.out.println(JedisUtils.setnx("123123年好吗asdsaddasd师多", "111", 1000, TimeUnit.SECONDS));
		System.out.println(JedisUtils.setnx("aaaa年好sadsdf吗奥术dsfg大师多", "111", 1000, TimeUnit.SECONDS));
		
		System.out.println(JedisUtils.expire("aaa", 10, TimeUnit.SECONDS));
		System.out.println(JedisUtils.expire("aaa", 10, TimeUnit.SECONDS));
		System.out.println(JedisUtils.expire("yyy", 10, TimeUnit.SECONDS));
		System.out.println(JedisUtils.expire("xxx", 10, TimeUnit.SECONDS));
		System.out.println(JedisUtils.expire("年好吗奥术大师多", 10, TimeUnit.SECONDS));
		System.out.println(JedisUtils.expire("123123年好吗asdsaddasd师多", 10, TimeUnit.SECONDS));
		System.out.println(JedisUtils.expire("aaaa年好sadsdf吗奥术dsfg大师多", 10, TimeUnit.SECONDS));
	}
	
	@Test
	public void testSinter() {
		JedisUtils.SET.sadd("key1", "a");
		JedisUtils.SET.sadd("key1", "b");
		JedisUtils.SET.sadd("key1", "c");
		JedisUtils.SET.sadd("key1", "d");
		JedisUtils.SET.sadd("key2", "c");
		JedisUtils.SET.sadd("key2", "d");
		JedisUtils.SET.sadd("key2", "e");
		JedisUtils.SET.sadd("key2", "f");
		Set<String> sinters = JedisUtils.SET.sinter("key1", "key2");
		sinters.forEach(System.out::println);
	}
}
