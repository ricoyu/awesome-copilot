package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 约瑟夫问题
 * <p>
 * 一、题目描述
 * <p>
 * 输入一个由随机数组成的数组（数组中每个数均是大于 0 的整数，长度已知），和初始计数值m。
 * <p>
 * 从数组首位置开始计数，计数到 m 后，将数组该位置数值替换为计数值m，并将数组该位置数值出列，然后从下一个位置重新开始计数，直到数组所有数值出列为止。
 * <p>
 * 如果计数到达数组尾部，则回到数组首位置继续计数。
 * <p>
 * 请编程实现上述计数过程，同时输出数组出列的顺序。
 * <p>
 * 例如：输入的随机数列为 3,1,2,4，初始计数值 m=7，从数组首位置开始计数（数值 3 所在位置）。
 * <p>
 * 第一轮计数出列数字为 3，计数值更新为 m=2，出列后数组为 3,1,4，从数组 4 所在位置重新计数。
 * <p>
 * 第一轮计数出列数字为 3，计数值更新为 m=3，出列后数组为 3,1,4，从数组 4 所在位置重新计数。
 * <p>
 * 第一轮计数出列数字为 1，计数值更新为 m=1，出列后数组为 4，从数组 1 所在位置重新计数。
 * <p>
 * 最终输出：2,3,1,4。
 * <p/>
 * 这是一个变种的约瑟夫问题，每次出列后计数值 m 被替换为出列位置的原数组数值，循环进行。
 *
 * <pre>
 * 初始数组：[3, 1, 2, 4]
 * 初始 m = 7
 * </pre>
 * 使用 index = (index + m - 1) % list.size() 是关键技巧，这可以让我们在循环结构中正确计算出每轮要出列的下标。
 * <ol>
 *     <li/>第一次从 index=0 开始，(0 + 7 - 1) % 4 = 2，出列 2，更新 m = 2
 *     <ul>
 *          <li/>剩余：[3, 1, 4]，index = 2（即原始 index=2 被删后，下一个index就是2）
 *     </ul>
 *     <li/>第二次从 index=2 开始，(2 + 2 - 1) % 3 = 0，出列 3，更新 m = 3
 *     <ul>
 *          <li/>剩余：[1, 4]，index = 0
 *     </ul>
 *     <li/>第三次从 index=0 开始，(0 + 3 - 1) % 2 = 0，出列 1，更新 m = 1
 *     <ul>
 *          <li/>剩余：[4]，index = 0
 *     </ul>
 *     <li/>第四次从 index=0 开始，(0 + 1 - 1) % 1 = 0，出列 4
 *     <ul>
 *          <li/>最终顺序：[2, 3, 1, 4]
 *     </ul>
 * </ol>
 * Copyright: Copyright (c) 2025-05-12 9:14
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class JosephusVariableCounter {

	public List<Integer> solve(int[] nums, int m) {
		// 用于存储出列顺序
		List<Integer> result = new ArrayList<Integer>();
		// 使用ArrayList方便删除操作
		ArrayList<Integer> list = Arrays.stream(nums)
				.boxed()
				.collect(Collectors.toCollection(ArrayList::new));

		// 当前计数索引
		int index = 0;

		// 直到所有元素出列
		while (!list.isEmpty()) {
			// (index + m - 1) % list.size() 计算出当前应出列元素的位置
			index = (index + m - 1) % list.size();
			// 将出列元素加入结果列表
			int removed = list.remove(index);
			result.add(removed);

			// 更新计数值m为刚出列元素的值
			m = removed;
		}

		return result;
	}

	public static void main(String[] args) {
		JosephusVariableCounter solver = new JosephusVariableCounter();
		int[] input = {3, 1, 2, 4};
		int m = 7;
		List<Integer> output = solver.solve(input, m);
		System.out.println(output); // 应输出 [2, 3, 1, 4]
	}
}
