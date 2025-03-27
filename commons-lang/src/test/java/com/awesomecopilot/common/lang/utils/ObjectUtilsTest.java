package com.awesomecopilot.common.lang.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectUtilsTest {

    @Test
    public void testIsEqual() {
    	Integer i = new Integer(300);
        Integer j = new Integer(300);
        assertTrue(ObjectUtils.equalTo(i, j));

        String s = "300";
        assertFalse(ObjectUtils.equalTo(i, s));
    }
}
