package com.awesomecopilot.pattern.visitor.example2;

public class Rectangle implements Shape {

	public void printOn(Printer printer) {
		printer.printRectangle(this);
	}
}