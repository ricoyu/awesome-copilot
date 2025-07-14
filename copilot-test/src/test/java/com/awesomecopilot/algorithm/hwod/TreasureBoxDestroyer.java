package com.awesomecopilot.algorithm.hwod;

import org.springframework.boot.autoconfigure.cache.CacheProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 阿里巴巴找黄金宝箱(II) - 贪心思维
 * <p>
 * 一、题目描述
 * <p>
 * 一贫如洗的樵夫阿里巴巴在去砍柴的路上，无意中发现了强盗集团的藏宝地，藏宝地有编号从0-N的箱子，每个箱子上面贴有箱子中藏有金币的数量。
 * <p>
 * 从金币数量中选出一个数字集合，并销毁贴有这些数字的每个箱子，如果能销毁一半及以上的箱子，则返回这个数字集合的最小大小。
 * <p>
 * 二、输入描述
 * <p>
 * 一个数字字串，数字之间使用逗号分隔，例如: 6,6,6,6,3,3,3,1,1,5字串中数字的个数为偶数，并且
 * <p>
 * 1 <= 字串中数字的个数 <= 100000
 * <p>
 * 1 <= 每个数字 <= 100000
 * <p>
 * 三、输出描述
 * <p>
 * 这个数字集合的最小大小，例如: 2
 *
 * <pre>
 * 测试用例1
 *
 * 1、输入
 * 1,1,1,1,2,2,2,2,3,3
 *
 * 2、输出
 * 2
 * </pre>
 *
 * 3、说明 <p>
 * 选择数字1和2，销毁8个箱子，超过一半。
 *
 * <pre>
 * 测试用例2
 *
 * 1、输入
 * 1,2,3,4,5,6,7,8,9,10
 *
 * 2、输出
 * 5
 * </pre>
 * <p/>
 * Copyright: Copyright (c) 2025-07-12 8:41
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class TreasureBoxDestroyer {

	public static void main(String[] args) {
		System.out.print("请输入数字字串，数字之间使用逗号分隔，例如: 6,6,6,6,3,3,3,1,1,5: ");
		Scanner scanner = new Scanner(System.in);
		int[] nums = Arrays.stream(scanner.nextLine().trim().split(",")).mapToInt(Integer::parseInt).toArray();
		System.out.println(minSetSize(nums));
	}

	/**
	 * 返回销毁一半及以上箱子的最小数字集合大小
	 * @param nums
	 * @return
	 */
	public static int minSetSize(int[] nums) {
		int n = nums.length;

		Map<Integer, Integer> freqMap = new HashMap<>();
		for (int i : nums) {
			freqMap.put(i, freqMap.getOrDefault(i, 0) + 1);
		}

		// 3. 将所有频率放入一个列表中
		List<Integer> freqList = new ArrayList<>(freqMap.values());
		// 4. 按照频率从高到低排序
		//Collections.sort(freqList, Collections.reverseOrder());
		freqList.sort((a, b) -> b-a);

		// 5. 贪心选择频率大的数字直到销毁一半箱子
		int removed = 0; // 已销毁箱子数量
		int count = 0;   // 选择的数字个数
		for (Integer freq : freqList) {
			removed += freq;
			count ++;
			if (removed >= n/2) {
				break;
			}
		}

		return count;
	}
}
