package com.awesomecopilot.pattern.creational.abstractfactory;

class ClassicChair implements Chair {
    @Override
    public void displayStyle() {
        System.out.println("Displaying a classic chair.");
    }
}