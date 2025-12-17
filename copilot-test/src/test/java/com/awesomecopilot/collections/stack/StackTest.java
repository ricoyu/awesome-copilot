package com.awesomecopilot.collections.stack;

import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StackTest {

	@Test
	public void testStackUsage() {
		Stack stack = new Stack();
		stack.push("A");
		Object actual = stack.get(0);
		System.out.println("栈中第0个元素: " + actual);
		assertEquals("A", actual);
		stack.push("B");
		Object actual1 = stack.get(1);
		System.out.println("栈中第1个元素: " + actual1);
		assertEquals("B", actual1);
		stack.push("C");
		Object actual2 = stack.get(2);
		System.out.println("栈中第2个元素: " + actual2);
		assertEquals("C", actual2);
	}
}
