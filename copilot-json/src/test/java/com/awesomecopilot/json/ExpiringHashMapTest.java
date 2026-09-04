package com.awesomecopilot.json;

import com.awesomecopilot.json.collections.ExpiringHashMap;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExpiringHashMapTest {

	@Test
	void defaultConstructorDoesNotExpireImmediately() {
		ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
		map.put("key", "value");
		assertEquals("value", map.get("key"));
		assertTrue(map.containsKey("key"));
	}

	@Test
	void entryExpiresAfterTtl() throws InterruptedException {
		ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
		map.put("key", "value", 200, TimeUnit.MILLISECONDS);
		assertEquals("value", map.get("key"));
		Thread.sleep(300);
		assertNull(map.get("key"));
		assertFalse(map.containsKey("key"));
	}

	@Test
	void getRenewsEntry() throws InterruptedException {
		ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
		map.put("key", "value", 300, TimeUnit.MILLISECONDS);
		for (int i = 0; i < 5; i++) {
			Thread.sleep(150);
			assertEquals("value", map.get("key"), "iteration " + i);
		}
		Thread.sleep(400);
		assertNull(map.get("key"));
	}

	@Test
	void computeIfAbsentComputesOnlyOnceForLiveEntry() {
		ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
		AtomicInteger count = new AtomicInteger();
		String first = map.computeIfAbsent("key", k -> "value-" + count.incrementAndGet(), 5, TimeUnit.SECONDS);
		String second = map.computeIfAbsent("key", k -> "value-" + count.incrementAndGet(), 5, TimeUnit.SECONDS);
		assertEquals("value-1", first);
		assertEquals("value-1", second);
		assertEquals(1, count.get());
	}

	@Test
	void computeIfAbsentRecomputesAfterExpiry() throws InterruptedException {
		ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
		AtomicInteger count = new AtomicInteger();
		map.computeIfAbsent("key", k -> "value-" + count.incrementAndGet(), 150, TimeUnit.MILLISECONDS);
		Thread.sleep(250);
		String value = map.computeIfAbsent("key", k -> "value-" + count.incrementAndGet(), 150, TimeUnit.MILLISECONDS);
		assertEquals("value-2", value);
		assertEquals(2, count.get());
	}

	@Test
	void removeAndSize() {
		ExpiringHashMap<String, String> map = new ExpiringHashMap<>();
		map.put("a", "1");
		map.put("b", "2", 100, TimeUnit.MILLISECONDS);
		assertEquals("1", map.remove("a"));
		assertNull(map.get("a"));
		assertEquals(1, map.size());
	}
}
