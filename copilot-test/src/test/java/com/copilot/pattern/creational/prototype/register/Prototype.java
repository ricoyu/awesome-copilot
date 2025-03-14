package com.copilot.pattern.creational.prototype.register;

public interface Prototype {
	public Prototype clone();

	public String getName();

	public void setName(String name);
}