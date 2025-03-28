package com.awesomecopilot.algorithm.leetcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合
 * <p>
 * 给定两个整数 n 和 k，返回范围 [1, n] 中所有可能的 k 个数的组合。 <p>
 * 你可以按 任何顺序 返回答案。
 * <p>
 * 示例 1：
 * <p>
 * <pre>
 * 输入：n = 4, k = 2
 * 输出：
 * [
 *   [2,4],
 *   [3,4],
 *   [2,3],
 *   [1,2],
 *   [1,3],
 *   [1,4],
 * ]
 * </pre>
 *
 * <pre>
 * 示例 2：
 *
 * 输入：n = 1, k = 1
 * 输出：[[1]]
 * </pre>
 * <p>
 * 那如何生成这样的组合呢？回溯法应该是一个常用的方法。回溯法通常用于解决这类组合问题，通过递归的方式尝试每一种可能，并在满足条件时保存结果，否则回退继续尝试。
 * <p>
 * 那具体来说，回溯的过程是怎样的呢？假设我们要生成k个数的组合，那么我们可以从第一个位置开始，依次选择每个可能的数，然后递归地处理剩下的位置。 <p>
 * 例如，当处理第一个位置时，我们可以选择1到n中的某个数，然后第二个位置只能选择比第一个数大的数，这样可以避免重复。
 * <p>
 * 举个例子，比如n=4，k=2。第一个位置可以选择1、2、3、4中的任何一个。如果第一个位置选了1，那么第二个位置可以选择2、3、4； <p>
 * 如果第一个位置选了2，第二个位置只能选3、4；依此类推。这样就能生成所有不重复的组合。
 * <p>
 * 那如何用代码实现这个思路呢？我需要一个递归函数，参数可能包括当前的起始位置、当前的组合路径。每次递归的时候，从起始位置开始选择下一个数，添加到路径中， <p>
 * 然后递归处理下一个位置，起始位置是当前选择的数加一。当路径的长度等于k时，就将这个路径添加到结果列表中。
 * <p>
 * 比如，假设当前路径是空的，起始位置是1。然后循环从1到n中选择一个数i，添加到路径中。然后递归调用，起始位置是i+1，路径是当前路径加上i。当路径长度等于k时，保存结果。
 * <p>
 * 那这样的话，递归的终止条件就是路径的长度等于k。每次递归的时候，遍历可能的起始位置到n的数，依次添加到路径中，然后继续递归。
 * <p>
 * 那具体到Java代码的话，我需要一个类，比如叫做CombinationGenerator。里面有一个方法combine，接受n和k作为参数，返回所有组合的列表。
 * <p>
 * 在combine方法中，初始化一个结果列表，然后调用回溯函数。回溯函数需要当前路径、起始位置、n、k和结果列表作为参数。
 * <p>
 * 举个例子，比如n=4，k=2。第一次调用回溯函数时，路径是空的，start是1。然后循环i从1到4：
 * <p>
 * i=1时，路径添加1，此时长度是1，未到k=2，所以递归调用回溯函数，start是2。此时循环i从2到4：
 * <p>
 * i=2时，路径添加2，长度是2，满足条件，添加到结果中。然后回溯，移除2，继续循环i=3，添加3，添加到结果，依此类推。
 * <p>
 * 当递归返回到i=1的情况时，路径移除1，继续i=2的情况。此时路径添加2，然后递归start是3，此时只能选3或4。比如选3的话，路径长度到2，保存结果。依此类推。
 * <p>
 * 这样就能生成所有符合条件的组合。
 * <p/>
 * Copyright: Copyright (c) 2025-03-27 8:31
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CombinationGenerator {

	public List<List<Integer>> combine(int n, int k) {
		List<List<Integer>> result = new ArrayList<>();
		backtrack(result, new ArrayList<>(), 1, n, k);
		return result;
	}

	/**
	 * 回溯方法，递归生成所有组合。
	 *
	 * @param result 结果列表，保存所有组合
	 * @param path   当前递归路径，记录已选择的数
	 * @param start  当前选择的起始位置，避免重复
	 * @param n      范围的上限
	 * @param k      需要选择的元素个数
	 */
	public void backtrack(List<List<Integer>> result, List<Integer> path, int start, int n, int k) {
		// 如果当前路径中的元素数量等于k，将路径添加到结果中
		if (path.size() == k) {
			result.add(new ArrayList<>(path));// 注意需要复制当前路径，避免后续修改影响结果
			return;
		}

		// 计算当前循环的最大起始位置，进行剪枝优化
		// 剩余需要选择的元素个数为 t = k - path.size()
		// 则i的最大有效取值为 n - t + 1，因为从i开始至少需要t个元素（包括i本身）
		int maxStart = n - (k - path.size()) + 1;
		for (int i = start; i <= maxStart; i++) {
			path.add(i);// 将当前数加入路径
			backtrack(result, path, i + 1, n, k);// 递归处理下一个位置
			path.remove(path.size() - 1); // 回溯，移除路径最后一个数，尝试下一个可能
		}
	}

	// 示例运行
	public static void main(String[] args) {
		CombinationGenerator generator = new CombinationGenerator();
		// 示例1: n=4, k=2
		System.out.println(generator.combine(4, 2));
		// 示例2: n=1, k=1
		System.out.println(generator.combine(1, 1));
	}
}
