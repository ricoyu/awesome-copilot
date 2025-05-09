package com.awesomecopilot.cache;

import com.awesomecopilot.cache.concurrent.Lock;
import com.awesomecopilot.cache.pojo.User;
import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * <p>
 * Copyright: (C), 2019/10/25 17:47
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class JedisUtilsTests {
	
	@Test
	public void testWarmUp() {
		try {
			Class.forName("com.awesomecopilot.cache.JedisUtils");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testLPushBRpop() {
		JedisUtils.LIST.lpush("list:aa", "a");
	}
	
	@Test
	public void testLPush() {
		JedisUtils.LIST.lpush("ids-alert", IOUtils.readFileAsString("D:\\Work\\观安信息上海有限公司\\NTA资料\\测试数据\\ids-alert-http-post.json"));
	}
	
	@Test
	public void testLPushIdsMetadata() {
		for (int i = 0; i < 3; i++) {
			JedisUtils.LIST.lpush("ids-metadata", IOUtils.readFileAsString("D:\\Work\\观安信息上海有限公司\\NTA资料\\测试数据\\ids-metadata-pop3.json"));
		}
	}
	
	@Test
	public void testSendDgaMetadata() {
		//JedisUtils.LIST.lpush("dga-metadata", IOUtils.readFileAsString("D:\\Work\\观安信息上海有限公司\\NTA资料\\测试数据\\dga-metadata-request.json"));
		//JedisUtils.LIST.lpush("dga-metadata", IOUtils.readFileAsString("D:\\Work\\观安信息上海有限公司\\NTA资料\\测试数据\\dga-metadata-response.json"));
		JedisUtils.LIST.lpush("dga-metadata", IOUtils.readFileAsString("D:\\Work\\观安信息上海有限公司\\NTA资料\\测试数据\\dga-metadata-response - failed.json"));
	}
	
	@Test
	public void testPubSub() {
		//JedisUtils.publish("websocket:msg", "{\"command\": \"沙箱启动成功\"}");
		JedisUtils.publish("copilot-channel", "Hi");
	}
	
	@SneakyThrows
	@Test
	public void testIdsTraffic() {
		int i = 0;
		while (i < 10) {
			NetFlowBean netFlowBean = new NetFlowBean();
			netFlowBean.setBytesRecvd(ThreadLocalRandom.current().nextLong(1000));
			netFlowBean.setMeshPort("ens224");
			netFlowBean.setPktsDrooped(ThreadLocalRandom.current().nextLong(100));
			netFlowBean.setPktsRecvd(ThreadLocalRandom.current().nextLong(1000));
			netFlowBean.setTs(new Date().getTime());
			//netFlowBean.setTs(DateUtils.toEpochMilis(LocalDateTime.of(2021, 1, 1, ThreadLocalRandom.current().nextInt(24), ThreadLocalRandom.current().nextInt(60))));
			
			JedisUtils.LIST.lpush("ids-traffic", netFlowBean);
			//TimeUnit.SECONDS.sleep(1L);
			i++;
		}
	}
	
	@Test
	public void testRpopIdsTraffic() {
		String brpop = JedisUtils.LIST.rpop("ids-traffic");
		System.out.println(brpop);
	}
	
	@Test
	public void testSet() {
		JedisUtils.set("k1", "aaa");
		assertEquals("aaa", JedisUtils.get("k1"));
		System.out.println(JedisUtils.get("k1"));
	}
	
	@Test
	public void testSetWithExpire() {
		Boolean success = JedisUtils.set("k1", "v1", 1, TimeUnit.MINUTES);
		System.out.println(success);
	}
	
	@Test
	public void testSetNX() {
		Boolean success = JedisUtils.setnx("k2", "v2", 1, TimeUnit.MINUTES);
		System.out.println(success);
	}
	
	@SneakyThrows
	@Test
	public void testIncrWithExpire() {
		Long value = JedisUtils.incr("retryCount", 1, TimeUnit.MINUTES);
		System.out.println(value);
		SECONDS.sleep(20);
		System.out.println(JedisUtils.incr("retryCount", 1, TimeUnit.MINUTES));
	}
	
	@Test
	public void testPipelined() {
		/*List<String> users = JedisUtils.pipeline((pipeline) -> {
			for (int i = 0; i < 100; i++) {
				pipeline.lpop("ids-traffic");
			}
		});
		users.forEach(System.out::println);*/
		while (true) {
			List<String> users = JedisUtils.pipeline((pipeline) -> {
				for (int i = 0; i < 100; i++) {
					pipeline.lpop("ids-traffic");
				}
			});
			users.forEach(System.out::println);
			try {
				SECONDS.sleep(3);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	/*@SneakyThrows
	public static void main(String[] args) {
		JedisPubSub jedisPubSub = JedisUtils.subscribe("channel:test", (channel, message) -> {
			log.info(message);
		});
		TimeUnit.SECONDS.sleep(10);
		JedisUtils.unsubscribe(jedisPubSub, "channel:test");
		log.info("UnSubscribed");
	}*/
	
	public static void main(String[] args) {
		System.out.println(new Date(1615290356000L));
		/*Runnable task = () -> {
			Lock lock = JedisUtils.blockingLock("lock1");
			try {
				lock.lock();
				log.info(Thread.currentThread().getName() + " locked");
				try {
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			} finally {
				if (lock.locked()) {
					lock.unlock();
				}
				System.out.println("任务完成");
				
			}
		};
		
		Thread t1 = new Thread(task, "t1");
		Thread t2 = new Thread(task, "t2");
		
		t1.start();
		t2.start();*/
		
	}
	
/*	public static void main(String[] args) {
		Lock lock = JedisUtils.blockingLock("lock1");
		try {
			lock.lock();
			log.info(Thread.currentThread().getName() + " locked");
			
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (true) {
				throw new RuntimeException();
			}
		} finally {
			if (lock.locked()) {
				lock.unlock();
			}
			System.out.println("任务完成");
			
		}
	}*/
	
	@Test
	public void testDelGet() {
		JedisUtils.set("k2", "三少爷");
		String value = JedisUtils.get("k2");
		System.out.println("value " + value);
		assertThat(value, value.equals("三少爷"));
		
		String value2 = JedisUtils.delGet("k2");
		System.out.println("value2 " + value2);
		assertThat(value2, value2.equals(value));
		
		String value3 = JedisUtils.get("k2");
		System.out.println("value3 " + value3);
		assertTrue(value3 == null);
	}
	
	@Test
	public void testHashLen() {
		JedisUtils.del("hash-len");
		JedisUtils.HASH.hset("hash-len", "f1", "v1");
		assertEquals(1, JedisUtils.HASH.hlen("hash-len"));
		JedisUtils.HASH.hset("hash-len", "f2", "v2");
		assertEquals(2, JedisUtils.HASH.hlen("hash-len"));
		JedisUtils.HASH.hdel("hash-len", "f1");
		assertEquals(1, JedisUtils.HASH.hlen("hash-len"));
	}
	
	@Test
	public void testHDelGet() {
		JedisUtils.del("hash-delget");
		JedisUtils.HASH.hset("hash-delget", "f1", "v1");
		assertEquals("v1", JedisUtils.HASH.hdelGet("hash-delget", "f1"));
	}
	
	@Test
	public void testLPushLimit() {
		/*JedisUtils.del("jobs");
		long size = LIST.lpushLimit("jobs", 6, "a", "b");
		assertEquals(2, size);
		
		size = JedisUtils.LIST.lpushLimit("jobs", 6, "c", "d", "e", "f", "g");
		assertEquals(6, size);
		
		JedisUtils.del("jobs");
		size = LIST.lpushLimit("jobs", 6, "a", "b", "v", "aasd", "g", "e", "t");
		assertEquals(6, size);*/
		JedisUtils.del("k1");
		JedisUtils.HASH.hget("k1", "k1");
		JedisUtils.LIST.lpushLimit("gen_rule_file_tasks-ssy1", 1, "新增了开启状态规则文件");
		JedisUtils.LIST.lpushLimit("gen_rule_file_tasks-ssy1", 1, "新增了开启状态规则文件");
	}
	
	@Test
	public void testQueueListener() {
		JedisUtils.LIST.brpop("jobs_list", (key, message) -> {
			System.out.println(message);
		});
	}
	
	@SneakyThrows
	@Test
	public void testHsetTtl() {
		//JedisUtils.HASH.hset("testMap", "testField", "123", 5);
		//String value = JedisUtils.HASH.hget("testMap", "testField");
		//assertEquals("123", value);
		//TimeUnit.SECONDS.sleep(3333);
		String value = JedisUtils.HASH.hget("testMap", "testField");
		assertNull(value);
	}
	
	@Test
	public void testPubSub2() {
		JedisUtils.publish("websocket:msg", "aaa");
	}
	
	@Test
	public void testSetGetBoolean() {
		JedisUtils.set("k1", true);
		Boolean v1 = JedisUtils.get("k1", Boolean.class);
		assertTrue(v1.booleanValue());
	}
	
	@Test
	public void testHIncrBY() {
		JedisUtils.del("cart");
		long currentCount = JedisUtils.HASH.hincrby("cart", "close:1", 10);
		assertEquals(10, currentCount);
		currentCount = JedisUtils.HASH.hincrby("cart", "close:1", -10);
		assertEquals(0, currentCount);
	}
	
	@Test
	public void testBRpop() {
		System.out.println("开始阻塞获取");
		String value = JedisUtils.LIST.brpop(20, "tuling");
		System.out.println(value);
	}
	
	@Test
	public void testSrandMember() {
		List members = JedisUtils.SET.srandmember("act:1001", 1);
		members.forEach(System.out::println);
	}
	
	@Test
	public void testSpop() {
		Set<String> members = JedisUtils.SET.spop("act:1001", 1);
		members.forEach(System.out::println);
	}
	
	@SneakyThrows
	@Test
	public void testNonblockingLock() {
		new Thread(() -> {
			Lock lock = JedisUtils.nonBlockingLock("stock");
			lock.lock();
			if (lock.locked()) {
				log.info("线程{}加锁成功", Thread.currentThread().getName());
			} else {
				log.info("线程{}加锁失败", Thread.currentThread().getName());
			}
		}, "线程1").start();
		new Thread(() -> {
			Lock lock = JedisUtils.nonBlockingLock("stock");
			lock.lock();
			if (lock.locked()) {
				log.info("线程{}加锁成功", Thread.currentThread().getName());
			} else {
				log.info("线程{}加锁失败", Thread.currentThread().getName());
			}
		}, "线程2").start();
		Thread.currentThread().join();
	}
	
	@SneakyThrows
	@Test
	public void testGetAndSetExpire() {
		JedisUtils.set("product:101:stock", 100, 3, SECONDS);
		Integer count = JedisUtils.get("product:101:stock", Integer.class);
		while (count != null) {
			Thread.sleep(1000);
			count = JedisUtils.get("product:101:stock", Integer.class, 3, SECONDS);
			System.out.println(count);
		}
		System.out.println("key过期了");
	}
	
	/**
	 * foobar 对应的ASCII码如下
	 * 	    二进制	    bit=1个数
	 * f	01100110	4
	 * o	01101111	6
	 * o	01101111	6
	 * b	01100010	3
	 * a	01100001	3
	 * r	01110010	4
	 *  	 	        26
	 *  BITCOUNT mykey 0 0，0 0 代表开始和结束的Byte位置数。0 0 取的是f，所以结果是4。
	 *  BITCOUNT mykey 1 1，1 1指从第一个Byte开始到下标为1结束，即o，结果为6。
	 *  BITCOUNT mykey 1 3, 指取下标1到3，即oob，结果为15。
	 */
	@Test
	public void testBitCount() {
		JedisUtils.set("mykey", "foobar");
		long count = JedisUtils.Bitmap.bitCount("mykey", 0, -1);
		System.out.println(count);
		long count1 = JedisUtils.Bitmap.bitCount("mykey", 0, 0);
		System.out.println("0 0: "+count1);
		long count2 = JedisUtils.Bitmap.bitCount("mykey", 1, 1);
		System.out.println("1 1: "+count2);
	}
	
	@Test
	public void testBitOP() {
		JedisUtils.Bitmap.setbit("rico", 9, 1);
		JedisUtils.Bitmap.setbit("rico", 0, 1);
		JedisUtils.Bitmap.bitOr("{rico}dest", "rico");
		long count = JedisUtils.Bitmap.bitCount("{rico}dest", 0, 9);
		assertEquals(count, 2);
	}
	
	@Test
	public void testRedisTemplateJSON() {
		//JedisUtils.set("k1", "v1");
		User user = new User();
		user.setName("三少爷");
		user.setAge(18);
		String json = JacksonUtils.toJson(user);
		JedisUtils.set("k1", json);
	}

	public static class HyperLoglogTest {

		@Test
		public void testPfAdd() {
			Long pfadd = JedisUtils.HyperLogLog.pfadd("dummy-key", "123", "456", "789");
			assertTrue(pfadd == 1);
		}
	}

	public static class BitMapTest {

		@Test
		public void testSetBit() {
			JedisUtils.del("rico");
			Boolean result = JedisUtils.Bitmap.setbit("rico", 8, 1);
			long count = JedisUtils.Bitmap.bitCount("rico", 0, 0);
			assertEquals(0, count);
			count = JedisUtils.Bitmap.bitCount("rico", 1, 1);
			assertEquals(1, count);
			result = JedisUtils.Bitmap.setbit("rico", 8, 0);
			assertTrue(result);
			result = JedisUtils.Bitmap.setbit("rico", 0, 1);
			assertFalse(result);
			JedisUtils.Bitmap.bitOr("dest", "rico");
			count = JedisUtils.Bitmap.bitCount("dest", 0, 9);
			assertEquals(count, 2);
		}
	}
}
