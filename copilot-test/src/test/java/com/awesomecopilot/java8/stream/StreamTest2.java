package com.awesomecopilot.java8.stream;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class StreamTest2 {

	@Test
	public void testCreateStream() {
		// 1. 集合创建（最常用）
		List<String> list = Arrays.asList("a", "b", "c");
		Stream<String> stream = list.stream();

		// 2. 数组创建
		String[] arr = {"x", "y", "z"};
		Stream<String> stream1 = Arrays.stream(arr);

		// 3. 直接创建（of/iterate/generate）
		Stream<Integer> stream2 = Stream.of(1, 2, 3);
		Stream<Integer> stream3 = Stream.iterate(0, n -> n + 2).limit(5);// 0,2,4,6,8
		Stream<Double> stream4 = Stream.generate(Math::random).limit(3);
	}

	@Test
	public void testStreamFilter() {
		List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
		List<Integer> odds = nums.stream()
				.filter(n -> n % 2 == 0)// 筛选偶数
				.collect(toList());
		odds.forEach(System.out::println);
	}

	@Test
	public void testStreamMap() {
		List<List<String>> nestedList = Arrays.asList(
				Arrays.asList("a", "b"),
				Arrays.asList("c", "d")
		);

		List<String> flatList = nestedList.stream()
				.flatMap(Collection::stream)// 拆分为单个Stream
				.collect(toList());
		flatList.forEach(System.out::println);
	}

	@Test
	public void testStreamSort() {
		List<Integer> nums = Arrays.asList(3, 1, 4, 2);
		// 自然排序
		List<Integer> sortedNums = nums.stream().sorted().collect(toList());
		sortedNums.forEach(System.out::println);

		System.out.println("--------------------");
		// 自定义排序（降序）
		List<Integer> sortedNums2 = nums.stream().sorted((n1, n2) -> n2 - n1).collect(toList());
		sortedNums2.forEach(System.out::println);
	}

	@Test
	public void testStreamDistinct() {
		List<Integer> nums = Arrays.asList(1, 2, 2, 3, 3, 3);
		List<Integer> nums2 = nums.stream().distinct().collect(toList());
		nums2.forEach(System.out::println);
	}

	@Test
	public void testStreamLimit() {
		List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
		List<Integer> nums2 = nums.stream().limit(3).collect(toList());
		nums2.forEach(System.out::println);
	}

	@Test
	public void testSkip() {
		List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
		List<Integer> nums2 = nums.stream().skip(2).collect(toList());
		nums2.forEach(System.out::println); // [3,4,5]
	}
}
