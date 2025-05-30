package com.awesomecopilot.pattern.creational.factorymethod;

/**
 * 具体品牌的电脑类
 * <p>
 * Copyright: Copyright (c) 2024-03-29 14:10
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>

 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public class MiComputer extends Computer {
    @Override
    public void setOperationSystem() {
        System.out.println("小米笔记本安装Win10系统");
    }
}