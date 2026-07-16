package com.awesomecopilot.collections;

import java.util.LinkedHashSet;

public class LinkedHashSetExample {
	
	public static void main(String[] args) {
		LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
		
		linkedHashSet.add("Java");
		linkedHashSet.add("C++");
		linkedHashSet.add("Python");
		
		// 保持插入顺序输出
		System.out.println("Elements in the linkedHashSet: " + linkedHashSet);
	}
}
