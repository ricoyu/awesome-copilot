package com.awesomecopilot.pattern.flyweight.demo;

/**
 * concrete Flyweight
 * @author Rico Yu
 * @since Jul 1, 2016
 * @version 
 *
 */
public class JavaPlatform implements Platform {
	public JavaPlatform() {
		System.out.println("JavaPlatform object created");
	}

	@Override
	public void execute(Code code) {
		System.out.println("Compiling and executing Java code.");
	}
}