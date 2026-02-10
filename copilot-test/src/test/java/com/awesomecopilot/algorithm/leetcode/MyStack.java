package com.awesomecopilot.algorithm.leetcode;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 用队列实现栈
 * <p>
 * 请你仅使用两个队列实现一个后入先出（LIFO）的栈，并支持普通栈的全部四种操作（push、top、pop 和 empty）。
 * <p>
 *
 * <ul>实现 MyStack 类:
 *     <li/>void push(int x) 将元素 x 压入栈顶。
 *     <li/>int pop() 移除并返回栈顶元素。
 *     <li/>int top() 返回栈顶元素。
 *     <li/>boolean empty() 如果栈是空的，返回 true ；否则，返回 false 。
 * </ul>
 *
 * <pre>
 * 输入：
 * ["MyStack", "push", "push", "top", "pop", "empty"]
 * [[], [1], [2], [], [], []]
 * 输出：
 * [null, null, null, 2, 2, false]
 *
 * 解释：
 * MyStack myStack = new MyStack();
 * myStack.push(1);
 * myStack.push(2);
 * myStack.top(); // 返回 2
 * myStack.pop(); // 返回 2
 * myStack.empty(); // 返回 False
 * </pre>
 * 注意：
 * <p>
 * 你只能使用队列的标准操作 —— 也就是 push to back、peek/pop from front、size 和 is empty 这些操作。
 * <p>
 * 你所使用的语言也许不支持队列。 你可以使用 list （列表）或者 deque（双端队列）来模拟一个队列 , 只要是标准的队列操作即可。
 * <p>
 * 用两个队列实现的栈结构
 * <p>
 * 核心：利用队列的"先入先出"特性，通过中转队列调整元素顺序，模拟栈的"后入先出"
 * <p/>
 * Copyright: Copyright (c) 2026-02-04 8:55
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MyStack {
	// 主队列：存储栈的所有元素，队首即为栈顶
	private Queue<Integer> queue1;
	// 辅助队列：push 操作时的临时中转容器
	private Queue<Integer> queue2;

	/**
	 * 构造方法：初始化两个空队列（LinkedList 实现 Queue 接口）
	 */
	public MyStack() {
		queue1 = new LinkedList<>();
		queue2 = new LinkedList<>();
	}

	/**
	 * 压栈操作：将元素 x 压入栈顶
	 * @param x 要压入的元素
	 */
	public void push(int x) {
		// 步骤1：先把新元素放入辅助队列 queue2（此时 queue2 只有 x）
		queue2.offer(x);
		// 步骤2：把主队列 queue1 中所有元素依次出队，并入队到 queue2
		// 目的：让新元素 x 始终在 queue2 的队首（模拟栈顶）
		while (!queue1.isEmpty()) {
			queue2.offer(queue1.poll());
		}
		// 步骤3：交换两个队列的引用，让 queue2 变为新的主队列，queue1 变为空的辅助队列
		Queue<Integer> temp = queue1;
		queue1 = queue2;
		queue2 = temp;
	}

	/**
	 * 出栈操作：移除并返回栈顶元素
	 * @return 栈顶元素
	 */
	public int pop() {
		// 主队列队首就是栈顶，直接出队即可
		return queue1.poll();
	}

	/**
	 * 获取栈顶元素：仅返回，不移除
	 * @return 栈顶元素
	 */
	public int top() {
		// 主队列队首就是栈顶，直接取值即可
		return queue1.peek();
	}

	/**
	 * 判断栈是否为空
	 * @return 空返回 true，否则返回 false
	 */
	public boolean empty() {
		// 只需判断主队列是否为空
		return queue1.isEmpty();
	}

	// 测试示例
	public static void main(String[] args) {
		MyStack myStack = new MyStack();
		myStack.push(1);  // 栈：[1]
		myStack.push(2);  // 栈：[1, 2]（栈顶是2）
		System.out.println(myStack.top());  // 输出 2（栈顶元素）
		System.out.println(myStack.pop());  // 输出 2（移除栈顶），栈变为 [1]
		System.out.println(myStack.empty());// 输出 false（栈非空）
	}
}
