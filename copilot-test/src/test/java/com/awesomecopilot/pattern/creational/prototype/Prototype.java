package com.awesomecopilot.pattern.creational.prototype;

public interface Prototype {
	/**
	 * 克隆自身的方法
	 * @return 一个从自身克隆出来的对象
	 */
	public Object clone();
}