package com.copilot.classloader.init;

import org.junit.Test;

public class ClassInitTest {
	
	static {
		System.out.println("init in ClassInitTest static block");
	}

	public static void main(String[] args) throws ClassNotFoundException {
//		A a = new A();
		Class.forName("com.copilot.classloader.init.A");
//		System.out.println(Thread.currentThread().getContextClassLoader());
//		Thread.currentThread().getContextClassLoader().loadClass("com.loserico.classloader.init.A");
		
		A.getName();
	}
	
	@Test
	public void testInstanceSubClass() throws ClassNotFoundException {
		Class.forName("com.copilot.classloader.init.A");
		System.out.println("-----------------------");
		C c = new C();
	}
	
	@Test
	public void testClassInitializeD() {
		D d = D.getInstance();
		System.out.println("D.a == " + d.getA());
		System.out.println("D.b == " + d.getB());
		
	}
}
