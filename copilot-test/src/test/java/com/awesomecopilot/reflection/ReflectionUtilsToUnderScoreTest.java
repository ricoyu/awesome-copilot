package com.awesomecopilot.reflection;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ReflectionUtils#toUnderScore(String)} 的单元测试类
 */
public class ReflectionUtilsToUnderScoreTest {

    /**
     * TC01: 输入为 null，期望返回 ""
     */
    @Test
    public void testToUnderScore_NullInput_ReturnsEmptyString() {
        assertEquals("", ReflectionUtils.toUnderScore(null));
    }

    /**
     * TC02: 输入为空字符串，期望返回 ""
     */
    @Test
    public void testToUnderScore_EmptyString_ReturnsEmptyString() {
        assertEquals("", ReflectionUtils.toUnderScore(""));
    }

    /**
     * TC03: 输入为纯空格字符串，期望返回 ""
     */
    @Test
    public void testToUnderScore_BlankString_ReturnsEmptyString() {
        assertEquals("", ReflectionUtils.toUnderScore(" "));
    }

    /**
     * TC04: 全小写字符串，期望原样返回
     */
    @Test
    public void testToUnderScore_AllLowerCase_NoChange() {
        assertEquals("abc", ReflectionUtils.toUnderScore("abc"));
    }

    /**
     * TC05: 首字母大写的驼峰命名，正常转为 snake_case
     */
    @Test
    public void testToUnderScore_FirstLetterCapitalized_NormalConversion() {
        assertEquals("abc_def", ReflectionUtils.toUnderScore("AbcDef"));
    }

    /**
     * TC06: 连续多个大写字母开头，正确识别分隔点
     */
    @Test
    public void testToUnderScore_ConsecutiveUppercase_CorrectSplitting() {
        assertEquals("ab_cdef", ReflectionUtils.toUnderScore("ABCDef"));
    }

    /**
     * TC07: 混合大小写，逐个识别变化点
     */
    @Test
    public void testToUnderScore_MixedCase_CorrectSplitting() {
        assertEquals("a_bc_de_f", ReflectionUtils.toUnderScore("aBcDeF"));
    }

    /**
     * TC08: 含有数字的字段名，保留数字位置不变
     */
    @Test
    public void testToUnderScore_WithNumbers_PreservesNumberPosition() {
        assertEquals("user1_id2", ReflectionUtils.toUnderScore("user1Id2"));
    }

    /**
     * TC09: 字段名前后带有下划线，最终结果需清理掉前后下划线
     */
    @Test
    public void testToUnderScore_LeadingTrailingUnderscores_RemovedInResult() {
        assertEquals("user_id", ReflectionUtils.toUnderScore("__UserId__"));
    }

    /**
     * TC10: 英文特殊词 iPhone 应该转成 i_phone
     */
    @Test
    public void testToUnderScore_iPhone_SpecialWordHandledCorrectly() {
        assertEquals("i_phone", ReflectionUtils.toUnderScore("iPhone"));
    }

    /**
     * TC11: XMLHTTPRequest 类似多连续大写场景处理
     */
    @Test
    public void testToUnderScore_XMLHttpRequest_HandledAsExpected() {
        assertEquals("xml_http_request", ReflectionUtils.toUnderScore("XMLHttpRequest"));
    }
}
