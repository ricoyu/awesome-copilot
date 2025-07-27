package com.awesomecopilot.common.lang.utils;


import org.junit.jupiter.api.Test;

public class ReflectionUtilsTest {

    @Test
    public void test() {
        MsgMonitor monitor = new MsgMonitor();
        monitor.setIntField1(666);
        Object field = ReflectionUtils.getFieldValue("intField1", monitor);
        Class<?> aClass = field.getClass();
    }

    @Test
    public void testSetStaticField() {
        System.out.println(ReflectionUtils.getFieldValue("name", Banana.class));
        ReflectionUtils.setField("name", Banana.class, "泰国香蕉");
        System.out.println(ReflectionUtils.getFieldValue("name", Banana.class));
    }


}
