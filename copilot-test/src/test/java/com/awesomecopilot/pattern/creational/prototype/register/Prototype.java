package com.awesomecopilot.pattern.creational.prototype.register;

public interface Prototype {
	public Prototype clone();

	public String getName();

	public void setName(String name);
}