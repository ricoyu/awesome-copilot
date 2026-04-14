package com.awesomecopilot.common.lang.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArrayUtils 工具类单元测试（完全匹配最新实现逻辑）")
class ArrayUtilsTest {
    
    // ==================== isArray 方法测试 ====================
    
    @Test
    @DisplayName("isArray - 应该正确识别所有数组（基本类型数组 + 对象数组）")
    void testIsArray_shouldReturnTrueForArrays() {
        assertTrue(ArrayUtils.isArray(new int[]{1, 2, 3}));
        assertTrue(ArrayUtils.isArray(new Integer[]{1, 2, 3}));
        assertTrue(ArrayUtils.isArray(new long[]{10L}));
        assertTrue(ArrayUtils.isArray(new Long[]{10L}));
        assertTrue(ArrayUtils.isArray(new double[]{1.1}));
        assertTrue(ArrayUtils.isArray(new Double[]{1.1}));
        assertTrue(ArrayUtils.isArray(new String[]{"a", "b"}));
        assertTrue(ArrayUtils.isArray(new Object[]{1, "hello", true}));
        assertTrue(ArrayUtils.isArray(new boolean[]{true, false}));
        assertTrue(ArrayUtils.isArray(new byte[]{1}));
        assertTrue(ArrayUtils.isArray(new char[]{'a'}));
        assertTrue(ArrayUtils.isArray(new float[]{1.1f}));
        assertTrue(ArrayUtils.isArray(new short[]{1}));
        assertTrue(ArrayUtils.isArray(new int[0]));           // 空数组
    }
    
    @Test
    @DisplayName("isArray - 非数组和 null 应该返回 false")
    void testIsArray_shouldReturnFalseForNonArraysAndNull() {
        assertFalse(ArrayUtils.isArray(null));
        assertFalse(ArrayUtils.isArray("hello"));
        assertFalse(ArrayUtils.isArray(123));
        assertFalse(ArrayUtils.isArray(123.45));
        assertFalse(ArrayUtils.isArray(true));
        assertFalse(ArrayUtils.isArray(new ArrayList<>()));
        assertFalse(ArrayUtils.isArray(Collections.emptyMap()));
    }
    
    // ==================== toListIfArray 方法测试 ====================
    
    @Test
    @DisplayName("toListIfArray - null 应该返回空 List")
    void testToListIfArray_null_shouldReturnEmptyList() {
        Object result = ArrayUtils.toListIfArray(null);
        assertTrue(result instanceof List);
        assertTrue(((List<?>) result).isEmpty());
    }
    
    @Test
    @DisplayName("toListIfArray - 非数组对象应该包装成 singletonList")
    void testToListIfArray_nonArray_shouldReturnSingletonList() {
        String str = "hello";
        Integer num = 123;
        Boolean flag = true;
        Object obj = new Object();
        
        Object result = ArrayUtils.toListIfArray(str);
        assertTrue(result instanceof List);
        assertEquals(List.of("hello"), result);
        assertNotSame(str, result);                    // 不是原对象
        
        assertEquals(List.of(123), ArrayUtils.toListIfArray(num));
        assertEquals(List.of(true), ArrayUtils.toListIfArray(flag));
        assertEquals(List.of(obj), ArrayUtils.toListIfArray(obj));
    }
    
    @Test
    @DisplayName("toListIfArray - 基本类型数组全部转为对应包装类型的 List")
    void testToListIfArray_primitiveArrays() {
        assertEquals(List.of(1, 2, 3), ArrayUtils.toListIfArray(new int[]{1, 2, 3}));
        assertEquals(List.of(10L, 20L), ArrayUtils.toListIfArray(new long[]{10L, 20L}));
        assertEquals(List.of(1.1, 2.2), ArrayUtils.toListIfArray(new double[]{1.1, 2.2}));
        assertEquals(List.of(true, false), ArrayUtils.toListIfArray(new boolean[]{true, false}));
        assertEquals(List.of(1.1f, 2.2f), ArrayUtils.toListIfArray(new float[]{1.1f, 2.2f}));
        assertEquals(List.of('a', 'b'), ArrayUtils.toListIfArray(new char[]{'a', 'b'}));
        assertEquals(List.of((short) 1, (short) 2), ArrayUtils.toListIfArray(new short[]{1, 2}));
        assertEquals(List.of((byte) 1, (byte) 2), ArrayUtils.toListIfArray(new byte[]{1, 2}));
    }
    
    @Test
    @DisplayName("toListIfArray - 包装类型数组（Long[]、Integer[]、Double[]、String[]）转为 List")
    void testToListIfArray_wrapperArrays() {
        assertEquals(List.of(10L, 20L), ArrayUtils.toListIfArray(new Long[]{10L, 20L}));
        assertEquals(List.of(1, 2, 3), ArrayUtils.toListIfArray(new Integer[]{1, 2, 3}));
        assertEquals(List.of(1.1, 2.2), ArrayUtils.toListIfArray(new Double[]{1.1, 2.2}));
        assertEquals(Arrays.asList("a", "b", "c"), ArrayUtils.toListIfArray(new String[]{"a", "b", "c"}));
    }
    
    @Test
    @DisplayName("toListIfArray - 其他对象数组（Boolean[]、Object[] 等）也转为 List（走 Object[] 分支）")
    void testToListIfArray_otherObjectArrays() {
        // Boolean[]
        Boolean[] boolArr = {true, false};
        Object result1 = ArrayUtils.toListIfArray(boolArr);
        assertTrue(result1 instanceof List);
        assertEquals(List.of(true, false), result1);
        assertNotSame(boolArr, result1);
        
        // 通用 Object[]
        Object[] mixed = {1, "hello", true};
        Object result2 = ArrayUtils.toListIfArray(mixed);
        assertTrue(result2 instanceof List);
        assertEquals(List.of(1, "hello", true), result2);
        assertNotSame(mixed, result2);
    }
    
    @Test
    @DisplayName("toListIfArray - 空数组统一返回空 List")
    void testToListIfArray_emptyArrays() {
        assertEquals(Collections.emptyList(), ArrayUtils.toListIfArray(new int[]{}));
        assertEquals(Collections.emptyList(), ArrayUtils.toListIfArray(new Integer[]{}));
        assertEquals(Collections.emptyList(), ArrayUtils.toListIfArray(new String[]{}));
        assertEquals(Collections.emptyList(), ArrayUtils.toListIfArray(new Object[]{}));
        assertEquals(Collections.emptyList(), ArrayUtils.toListIfArray(new boolean[]{}));
    }
    
    // ==================== 参数化测试（覆盖核心场景） ====================
    
    @ParameterizedTest
    @MethodSource("provideTestCases")
    @DisplayName("toListIfArray 参数化测试")
    void testToListIfArray_parameterized(Object input, Object expected) {
        Object result = ArrayUtils.toListIfArray(input);
        
        assertEquals(expected, result);
        
        // 只要输入不是 null，结果一定是 List
        if (input != null) {
            assertTrue(result instanceof List);
        }
    }
    
    static List<Arguments> provideTestCases() {
        return List.of(
                Arguments.of(null, Collections.emptyList()),
                Arguments.of("single", List.of("single")),
                Arguments.of(999, List.of(999)),
                Arguments.of(new int[]{10, 20}, List.of(10, 20)),
                Arguments.of(new long[]{10L, 20L}, List.of(10L, 20L)),
                Arguments.of(new Integer[]{1, 2}, List.of(1, 2)),
                Arguments.of(new Long[]{10L, 20L}, List.of(10L, 20L)),
                Arguments.of(new String[]{"x", "y"}, List.of("x", "y")),
                Arguments.of(new boolean[]{true, false}, List.of(true, false)),
                Arguments.of(new Object[]{1, "a"}, List.of(1, "a")),
                Arguments.of(new Boolean[]{true, false}, List.of(true, false))
        );
    }
}