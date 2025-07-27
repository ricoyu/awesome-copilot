package com.awesomecopilot.common.lang.utils;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjectUtilsTest {

    @Test
    public void testIsEqual() {
    	Integer i = new Integer(300);
        Integer j = new Integer(300);
        assertTrue(ObjectUtils.equals(i, j));

        String s = "300";
        assertFalse(ObjectUtils.equals(i, s));
    }
}
