package com.awesomecopilot.pattern.creational.abstractfactory;

class ClassicTable implements Table {
    @Override
    public void displayStyle() {
        System.out.println("Displaying a classic table.");
    }
}