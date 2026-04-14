package com.awesomecopilot.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.Data;
import org.junit.jupiter.api.Test;

/**
 * <p>
 * Copyright: (C), 2021-07-01 17:01
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class CaffeineTest2 {
	
	@Test
	public void testLoadFromDatabase() {
		LoadingCache<Object, String> cache = Caffeine.newBuilder()
				.maximumSize(100)
				.build((k) -> {
					System.out.println("从数据库加载: " + k);
					return "666";
				});
		
		String value = cache.get("rico");
		System.out.println(value);
	}
	@Data
	static class DataObject {
		
		private final String data;
		
		private static int objectCounter = 0;
		
		
		public DataObject(String data) {
			this.data = data;
		}
		
		public static DataObject get(String data) {
			objectCounter++;
			return new DataObject(data);
		}
	}
}