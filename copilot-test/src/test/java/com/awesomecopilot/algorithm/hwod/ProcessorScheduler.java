package com.awesomecopilot.algorithm.hwod;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理器问题
 * <p>
 * 一、题目描述
 * <p>
 * 某公司研发了一款高性能AI处理器。每台物理设备Q 具备8颗AI处理器，编号分别为0，1，2，3，4，5，6，7。
 * <p>
 * 编号0-3的处理器处于同一个链路内，编号4-7的处理器处于另外一个链路中，不同链路中的处理器不能通信。
 * <p>
 * 如下图所示。现给定 服务器Q 可用的处理器编号数组array，以及任意的处理器申请数量num，找出符合下列亲和性调度原则的芯片组合。
 * <p>
 * 如果不存在符合要求的组合，则返回空列表。
 * <p>
 * 亲和性调度原则：
 * <ul>
 *     <li/>如果申请处理器个数为1，则选择同一链路，剩余可用的处理器数量为1个的最佳，其次是剩余3个的为次佳，然后是剩余2个，最后是剩余4个。
 *     <li/>如果申请处理器个数为2，则选择同一链路剩余可用的处理器数量为2个的为最佳，其次是剩余4个，最后是剩余3个。
 *     <li/>如果申请处理器个数为4，则必须选择同一链路剩余可用的处理器数量为4个。
 *     <li/>如果申请处理器个数为8，则申请节点所有8个处理器。
 *     <li/>
 * </ul>
 * <p>
 * 提示：
 * <p>
 * 任各申请的处理器数量只能是1，2，4，8。
 * <p>
 * 编号0-3的处理器处于一个链路，编号4-7的处理器处于另外一个链路。
 * <p>
 * 处理器编号唯一，且不存在任何编号相同的处理器。
 * <p>
 * 二、输入描述
 * <p>
 * 输入包含可用的处理器编号数组 array Q，以及任务申请的处理器数量 num 两个部分。
 * <p>
 * 第一行为array，第二行为num。例如：
 * <p>
 * [0, 1, 4, 5, 6, 7] <p>
 * 1
 * <p>
 * 表示当前编号为0，1，4，5，6，7的处理器可用。任务申请1个处理器。
 * <p/>
 * 0 <= array.length <= 8 <p>
 * 0 <= array[i] <= 7 <p>
 * num in [1, 2, 4, 8] <p>
 * <p>
 * 三、输出描述
 * <p>
 * 输出为组合列表，当 array = [0, 1, 4, 5, 6, 7]，num = 1时，输出为[[0], [1]]
 * <p>
 * <pre>
 * 测试用例1：
 *
 * 1、输入
 * [0, 1, 4, 5, 6, 7]
 * 1
 *
 * 2、输出
 * [[0], [1]]
 * </pre>
 * <p>
 * 3、说明 <p/>
 * 根据第一条亲和性调度原则，在剩余两个处理器的链路(0, 1, 2, 3)中选择处理器。 由于只有0和1可用，则返回任意一颗处理器即可。
 * <pre>
 * <pre>
 * 测试用例2：
 *
 * 1、输入
 * [0, 1, 4, 5, 6, 7]
 * 4
 *
 * 2、输出
 * [[4, 5, 6, 7]]
 *
 * 3、说明
 * 根据第三条亲和性调度原则，必须选择同一链路剩余可用的处理器数量为4个的环。
 * </pre>
 * <p>
 * 请用Java实现并给出核心的解题思路, 不要太啰嗦, Java代码中要加入详细的注释以解释清楚代码逻辑, 要给类取一个合理的类名
 * <p/>
 * 对于比较难以理解的, 需要技巧性的代码逻辑, 请在Java注释之外详细举例说明
 * <p/>
 * 对于核心算法部分, 请另外举例说明代码逻辑
 * <p/>
 * Copyright: Copyright (c) 2025-04-19 9:23
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class ProcessorScheduler {

	// 测试用例
	public static void main(String[] args) {
		ProcessorScheduler scheduler = new ProcessorScheduler();

		// 测试用例1
		int[] array1 = {0, 1, 4, 5, 6, 7};
		int num1 = 1;
		System.out.println(scheduler.scheduleProcessors(array1, num1)); // 输出 [[0], [1]]

		// 测试用例2
		int[] array2 = {0, 1, 4, 5, 6, 7};
		int num2 = 4;
		System.out.println(scheduler.scheduleProcessors(array2, num2)); // 输出 [[4, 5, 6, 7]]
	}

	/**
	 * 根据亲和性调度原则选择处理器组合
	 *
	 * @param array 可用处理器编号数组
	 * @param num   申请的处理器数量
	 * @return 符合条件的处理器组合列表
	 */
	public List<List<Integer>> scheduleProcessors(int[] array, int num) {
		List<List<Integer>> result = new ArrayList<>();

		//1. 将处理器按链路分组
		List<Integer> link0 = new ArrayList<>(); // 链路0-3
		List<Integer> link1 = new ArrayList<>(); // 链路4-7

		for (Integer processor : array) {
			if (processor >= 0 && processor <= 3) {
				link0.add(processor);
			} else if (processor >= 4 && processor <= 7) {
				link1.add(processor);
			}
		}

		// 2. 处理特殊情况：申请8个处理器
		if (num == 8) {
			if (link0.size() + link1.size() == 8) {
				List<Integer> allProcessors = new ArrayList<>();
				for (int i = 0; i < 8; i++) {
					allProcessors.add(i);
				}
				result.add(allProcessors);
			}
			return result;
		}

		// 3. 根据申请数量选择最佳链路
		List<Integer> selectedLink = selectBestLink(link0, link1, num);

		// 4. 生成所有可能的组合
		if (selectedLink != null && selectedLink.size() >= num) {
			generateCombinations(selectedLink, num, 0, new ArrayList<>(), result);
		}

		return result;
	}

	/**
	 * 生成所有可能的处理器组合（回溯算法）
	 *
	 * @param processors 可用的处理器列表
	 * @param num        需要选择的处理器数量
	 * @param start      起始索引
	 * @param current    当前组合
	 * @param result     结果列表
	 */
	private void generateCombinations(List<Integer> processors, int num, int start, List<Integer> current,
	                                  List<List<Integer>> result) {
		if (current.size() == num) {
			result.add(new ArrayList<>(current));
			return;
		}

		for (int i = start; i < processors.size(); i++) {
			current.add(processors.get(i));
			generateCombinations(processors, num, i + 1, current, result);
			current.remove(current.size() - 1);
		}
	}

	private List<Integer> selectBestLink(List<Integer> link0, List<Integer> link1, int num) {
		int size0 = link0.size();
		int size1 = link1.size();

		// 申请1个处理器的情况
		if (num == 1) {
			// 优先选择剩余1个处理器的链路，然后是3个、2个、4个
			if (size0 == 1) {
				return link0;
			}
			if (size1 == 1) {
				return link1;
			}
			if (size0 == 3) {
				return link0;
			}
			if (size1 == 3) {
				return link1;
			}
			if (size0 == 2) {
				return link0;
			}
			if (size1 == 2) {
				return link1;
			}
			if (size0 == 4) {
				return link0;
			}
			if (size1 == 4) {
				return link1;
			}
		}

		// 申请2个处理器的情况
		if (num == 2) {
			if (size0 == 2) {
				return link0;
			}
			if (size1 == 2) {
				return link1;
			}
			if (size0 == 4) {
				return link0;
			}
			if (size1 == 4) {
				return link1;
			}
			if (size0 == 3) {
				return link0;
			}
			if (size1 == 3) {
				return link1;
			}
		}

		// 申请4个处理器的情况
		if (num == 4) {
			if (size0 == 4) {
				return link0;
			}
			if (size1 == 4) {
				return link1;
			}
		}

		return null;
	}
}
