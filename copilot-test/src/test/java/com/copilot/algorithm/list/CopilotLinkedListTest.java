package com.copilot.algorithm.list;

/**
 * <p>
 * Copyright: (C), 2022-06-27 7:03
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class CopilotLinkedListTest {
	
	public static void main(String[] args) {
		CopilotLinkedList list = new CopilotLinkedList();
		list.insert("hello", 0);
		list.insert("world", 1);
		list.insert("rico", 2);
		list.delete(1);
		list.print();
	}
}
