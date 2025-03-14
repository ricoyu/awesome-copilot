package com.copilot.jvm.clazz.initialize;

public class SubClass extends SuperClass {

	static {
		System.out.println("Subclass init");
	}
}
