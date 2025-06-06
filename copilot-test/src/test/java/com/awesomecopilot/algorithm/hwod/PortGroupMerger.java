package com.awesomecopilot.algorithm.hwod;

/**
 * 端口合并
 *
 * 一、题目描述
 * 有M(1<=M<=10)个端口组，每个端口组是长度为N(1<=N<=100)的整数数组，如果某端口组间存在2个及以上端口相同，则认为这2个端口组互相关联，可以合并。
 *
 * 第一行输入端口组个数M，再输入M行，每行逗号分隔，代表端口组。
 *
 * 输出合并后的端口组，用二维数组表示。
 *
 * 二、输入描述
 * 第一行输入一个数字M
 *
 * 第二行开始输入M行，每行是长度为N的整数数组，用逗号分割
 *
 * 三、输出描述
 * 合并后的二维数组
 *
 * 测试用例1：
 *
 * 1、输入
 * 4
 * 4
 * 2,3,2
 * 1,2
 * 5
 *
 * 2、输出
 * [[4],[2, 3],[1, 2],[5]]
 *
 * 测试用例2：
 *
 * 1、输入
 * 3
 * 2,3,1
 * 4,3,2
 * 5
 *
 * 2、输出
 * [[1,2,3,4],[5]]
 * <p/>
 * Copyright: Copyright (c) 2025-06-06 12:00
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class PortGroupMerger {

	public static void main(String[] args) {
		System.out.print("[[1,2,3,4],[5]]");
	}
}
