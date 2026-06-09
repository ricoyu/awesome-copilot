package com.awesomecopilot.search8x;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>
 * Copyright: (C), 2021-08-16 13:39
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class DeleteTest {
	
	@Test
	public void testDeleteByQuery() {
		List<Object> objects = ElasticUtils.Query.termsQuery("event_2021-08-11")
				.query("attack_direction", "in_in", "in_out", "out_in")
				.queryForList();
		
		objects.forEach(System.out::println);
		
		long delete = ElasticUtils.Query.termsQuery("event_*")
				.query("attack_direction", "in_in", "in_out", "out_in")
				.delete();
		System.out.println(delete);
	}
	
	@Test
	public void test2() {
		String ids = "10,11,12,13";
		int[] skuIds = Arrays.stream(ids.trim().split(",")).mapToInt(Integer::parseInt).toArray();
		long count = ElasticUtils.Query.termsQuery("product")
				//.query("skuId", skuIds)
				.query("skuId", skuIds)
				.delete();
		assertEquals(4,count );
	}
	
	public static void main(String[] args) {
		System.out.println(new Date().getTime());
	}
}