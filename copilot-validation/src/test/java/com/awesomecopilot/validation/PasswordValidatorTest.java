package com.awesomecopilot.validation;

import com.awesomecopilot.validation.validation.PasswordValidator;
import com.awesomecopilot.validation.validation.annotation.Password;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordValidator using JUnit 5 and Mockito.
 */
public class PasswordValidatorTest {
    
    @Mock
    private Password passwordAnnotation;
    
    @Mock
    private ConstraintValidatorContext context;
    
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;
    
    private PasswordValidator validator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new PasswordValidator();
        
        // Mock the context behavior for setting custom messages
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }
    
    @Test
    void testNullPasswordReturnsTrue() {
        // Initialize with default values
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertFalse(validator.isValid(null, context));
        verifyMessageSet("密码不能为空");
    }
    
    @Test
    void testEmptyPasswordReturnsTrue() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertFalse(validator.isValid("", context));
        verifyMessageSet("密码不能为空");
    }
    
    @Test
    void testPasswordTooShort() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertFalse(validator.isValid("Abc123!", context));
        verifyMessageSet("密码长度必须在8-20位之间");
    }
    
    @Test
    void testPasswordTooLong() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertTrue(validator.isValid("Abcdefgh1234567890!@", context)); // 21 chars
        verifyNoMessageSet();
    }
    
    @Test
    void testPasswordMissingNumberWhenRequired() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertFalse(validator.isValid("Abcdefgh!", context));
        verifyMessageSet("密码必须包含数字");
    }
    
    @Test
    void testPasswordMissingCaseDiffWhenRequired_OnlyLower() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertFalse(validator.isValid("abcdefgh1!", context));
        verifyMessageSet("密码必须同时包含大写字母和小写字母");
    }
    
    @Test
    void testPasswordMissingCaseDiffWhenRequired_OnlyUpper() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertFalse(validator.isValid("ABCDEFGH1!", context));
        verifyMessageSet("密码必须同时包含大写字母和小写字母");
    }
    
    @Test
    void testPasswordMissingLetterWhenCaseDiffNotRequired() {
        mockAnnotation(8, 20, true, false, true);
        validator.initialize(passwordAnnotation);
        
        assertFalse(validator.isValid("12345678!", context));
        verifyMessageSet("密码必须包含字母");
    }
    
    @Test
    void testPasswordMissingSpecialCharWhenRequired() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertFalse(validator.isValid("Abcdefgh1", context));
        verifyMessageSet("密码必须包含特殊字符（!@#$%^&*()_+-=[]{}|;:,.<>?`~）");
    }
    
    @Test
    void testValidPasswordWithAllRequirements() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertTrue(validator.isValid("Abcdefgh1!", context));
        verifyNoMessageSet();
    }
    
    @Test
    void testValidPasswordWithoutCaseDiffRequired() {
        mockAnnotation(8, 20, true, false, true);
        validator.initialize(passwordAnnotation);
        
        assertTrue(validator.isValid("abcdefgh1!", context));
        verifyNoMessageSet();
    }
    
    @Test
    void testValidPasswordWithoutSpecialCharRequired() {
        mockAnnotation(8, 20, false, true, true);
        validator.initialize(passwordAnnotation);
        
        assertTrue(validator.isValid("Abcdefgh1", context));
        verifyNoMessageSet();
    }
    
    @Test
    void testValidPasswordWithoutNumberRequired() {
        mockAnnotation(8, 20, true, true, false);
        validator.initialize(passwordAnnotation);
        
        assertTrue(validator.isValid("Abcdefgh!", context));
        verifyNoMessageSet();
    }
    
    @Test
    void testPasswordWithWhitespaceTrimmed() {
        mockAnnotation(8, 20, true, true, true);
        validator.initialize(passwordAnnotation);
        
        assertTrue(validator.isValid(" Abcdefgh1! ", context)); // Trimmed to valid
        verifyNoMessageSet();
    }
    
    // Helper methods
    
    private void mockAnnotation(int minLength, int maxLength, boolean requireSpecialChar,
                                boolean requireCaseDiff, boolean requireNumber) {
        when(passwordAnnotation.minLength()).thenReturn(minLength);
        when(passwordAnnotation.maxLength()).thenReturn(maxLength);
        when(passwordAnnotation.requireSpecialChar()).thenReturn(requireSpecialChar);
        when(passwordAnnotation.requireCaseDiff()).thenReturn(requireCaseDiff);
        when(passwordAnnotation.requireNumber()).thenReturn(requireNumber);
    }
    
    private void verifyMessageSet(String expectedMessage) {
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(expectedMessage);
        verify(violationBuilder).addConstraintViolation();
    }
    
    private void verifyNoMessageSet() {
        verify(context, never()).disableDefaultConstraintViolation();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }
}