package com.awesomecopilot.algorithm.leetcode;

/**
 * 烹饪料理
 * <p>
 * 欢迎各位勇者来到力扣城，城内设有烹饪锅供勇者制作料理，为自己恢复状态。
 * <p>
 * 勇者背包内共有编号为 0 ~ 4 的五种食材，其中 materials[j] 表示第 j 种食材的数量。通过这些食材可以制作若干料理，cookbooks[i][j] 表示制作第 i 种料理需要第 j 种食材的数量，而 attribute[i] = [x,y] 表示第 i 道料理的美味度 x 和饱腹感 y。
 * <p>
 * 在饱腹感不小于 limit 的情况下，请返回勇者可获得的最大美味度。如果无法满足饱腹感要求，则返回 -1。
 * <p>
 * 每种料理只能制作一次。
 *
 * <pre>
 * 示例 1：
 *
 * 输入：materials = [3,2,4,1,2] cookbooks = [[1,1,0,1,2],[2,1,4,0,0],[3,2,4,1,0]] attribute = [[3,2],[2,4],[7,6]] limit = 5
 *
 * 输出：7
 * </pre>
 *
 * 解释： 食材数量可以满足以下两种方案： 方案一：制作料理 0 和料理 1，可获得饱腹感 2+4、美味度 3+2 方案二：仅制作料理 2， 可饱腹感为 6、美味度为 7 因此在满足饱腹感的要求下，可获得最高美味度 7
 *
 * <ul>核心思路是回溯法（暴力枚举）：
 *     <li/>遍历所有料理的组合（每个料理有 “做” 或 “不做” 两种选择）；
 *     <li/>做某道料理前，先检查食材是否足够；
 *     <li/>若做该料理后饱腹感≥limit，更新最大美味度；
 *     <li/>递归遍历所有可能的组合，最终返回符合条件的最大美味度（无则返回 - 1）。
 * </ul>
 * <p/>
 * Copyright: Copyright (c) 2026-03-10 9:05
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class CookingMaxDeliciousness {
	
	// 全局变量：记录最大美味度
	static int maxDelicious = -1;
	
	// 测试示例
	public static void main(String[] args) {
		CookingMaxDeliciousness solution = new CookingMaxDeliciousness();
		// 示例1输入
		int[] materials = {3,2,4,1,2};
		int[][] cookbooks = {{1,1,0,1,2},{2,1,4,0,0},{3,2,4,1,0}};
		int[][] attribute = {{3,2},{2,4},{7,6}};
		int limit = 5;
		// 输出应等于7
		System.out.println(solution.perfectMenu(materials, cookbooks, attribute, limit));
	}
	
	/**
	 * 主方法：计算满足饱腹感要求的最大美味度
	 * @param materials 食材数量数组（0~4号食材）
	 * @param cookbooks 料理所需食材数组（cookbooks[i][j] = 第i道料理需要第j种食材的数量）
	 * @param attribute 料理属性数组（attribute[i][0]=美味度，attribute[i][1]=饱腹感）
	 * @param limit 最低饱腹感要求
	 * @return 最大美味度（无法满足返回-1）
	 */
	public int perfectMenu(int[] materials, int[][] cookbooks, int[][] attribute, int limit) {
		// 回溯遍历所有料理组合
		backtrack(materials, cookbooks, attribute, limit, 0, 0, 0);
		return maxDelicious;
	}
	
	/**
	 * 回溯核心方法
	 * @param materials 剩余食材数量（递归中会复制，避免修改原数组）
	 * @param cookbooks 料理所需食材
	 * @param attribute 料理属性
	 * @param limit 最低饱腹感
	 * @param index 当前遍历到的料理索引（从0开始，避免重复选）
	 * @param curDelicious 当前累计美味度
	 * @param curFull 当前累计饱腹感
	 */
	private void backtrack(int[] materials, int[][] cookbooks, int[][] attribute, int limit, int index, int curDelicious, int curFull) {
		// 终止条件：遍历完所有料理
		if (index == cookbooks.length) {
			// 若饱腹感满足要求，更新最大美味度
			if (curFull >= limit && curDelicious > maxDelicious) {
				maxDelicious = curDelicious;
			}
			return;
		}
		
		// 选择1：不做当前索引的料理，直接遍历下一个
		backtrack(materials, cookbooks, attribute, limit, index + 1, curDelicious, curFull);
		
		// 选择2：做当前索引的料理（先检查食材是否足够）
		int[] need = cookbooks[index]; // 当前料理需要的食材
		boolean canCook = true;
		// 检查每种食材是否足够
		for (int i = 0; i < 5; i++) {
		    if (need[i] > materials[i]) {
				canCook = false;
				break;
		    }
		}
		
		if (canCook) {
			// 复制食材数组，避免修改原数组（递归回溯后恢复状态）
			int[] newMaterials = materials.clone();
			// 扣除制作当前料理的食材
			for (int i = 0; i < 5; i++) {
			    newMaterials[i] -= need[i];
			}
			// 累计美味度和饱腹感
			int newDelicious = curDelicious + attribute[index][0];
			int newFull = curFull + attribute[index][1];
			// 递归处理下一个料理
			backtrack(newMaterials, cookbooks, attribute, limit, index + 1, newDelicious, newFull);
		}
	}
}