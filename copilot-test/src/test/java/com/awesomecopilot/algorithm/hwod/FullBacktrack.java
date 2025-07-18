package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class FullBacktrack {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入数组中的数字: ");
		int[] nums = Arrays.stream(scanner.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();
		boolean[] used = new boolean[nums.length];
		List<Integer> current = new ArrayList<>();
		backtrack(nums, used, current);
	}

	private static void backtrack(int[] nums, boolean[] used, List<Integer> current) {
		if (current.size() == nums.length) {
			System.out.println(current);
			return;
		}

		for (int i = 0; i < nums.length; i++) {
			if (!used[i]) {
				used[i] = true; //标记该位置已使用
				current.add(nums[i]);
				backtrack(nums, used, current);
				current.remove(current.size() - 1);
				used[i] = false;
			}
		}
	}
}
