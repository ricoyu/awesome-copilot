package com.awesomecopilot.pattern.structural.composite;

/**
 * 具体的图形类
 * <p>
 * Copyright: Copyright (c) 2024-03-29 17:10
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class Square implements Graphic {
    @Override
    public void draw() {
        System.out.println("Drawing a Square");
    }
}