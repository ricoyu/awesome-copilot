package com.awesomecopilot.algorithm.leetcode;

import java.util.Scanner;

public class HammingWeightCalculator {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.print("请输入正整数n: ");
		int n = scanner.nextInt();
		System.out.println(hammingWeight(n));
		scanner.close();
	}

	public static int hammingWeight(int n) {
		int count = 0;
		for (int i = 0; i < 32; i++) {
			if ((n & 1) == 1) {
				count++;
			}
			n = n >> 1;
		}
		return count;
	}
}
