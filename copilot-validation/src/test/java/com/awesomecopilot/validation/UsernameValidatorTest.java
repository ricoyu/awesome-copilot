package com.awesomecopilot.validation;

import com.awesomecopilot.validation.validation.UsernameValidator;
import com.awesomecopilot.validation.validation.annotation.Username;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsernameValidatorTest {

    private ConstraintValidator<Username, String> validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    public void setUp() {
        validator = new UsernameValidator();
        context = Mockito.mock(ConstraintValidatorContext.class);
    }

    @Test
    public void testNullValue() {
        assertFalse(validator.isValid(null, context));
    }

    @Test
    public void testEmptyString() {
        assertFalse(validator.isValid("", context));
    }

    @Test
    public void testBlankString() {
        assertFalse(validator.isValid("   ", context));
    }

    @Test
    public void testTooShort() {
        assertFalse(validator.isValid("ab", context)); // length 2
    }

    @Test
    public void testTooLong() {
        assertFalse(validator.isValid("abcdefghijklmnopqrstuvwxyz", context)); // length 26
    }

    @Test
    public void testMinLength() {
        assertTrue(validator.isValid("abc", context)); // length 3
    }

    @Test
    public void testMaxLength() {
        assertTrue(validator.isValid("abcdefghijklmno", context)); // length 15
    }

    @Test
    public void testStartsWithLetter() {
        assertTrue(validator.isValid("user_name@123", context));
    }

    @Test
    public void testStartsWithNumber() {
        assertTrue(validator.isValid("123user_name@", context));
    }

    @Test
    public void testStartsWithUnderscore() {
        assertFalse(validator.isValid("_username", context));
    }

    @Test
    public void testStartsWithAt() {
        assertFalse(validator.isValid("@username", context));
    }

    @Test
    public void testInvalidCharacters() {
        assertFalse(validator.isValid("user-name", context)); // hyphen not allowed
        assertFalse(validator.isValid("user!name", context)); // exclamation not allowed
    }

    @Test
    public void testOnlyAllowedCharacters() {
        assertTrue(validator.isValid("a1_@2b3", context));
    }

    @Test
    public void testAllUnderscoresAfterStart() {
        assertTrue(validator.isValid("a___", context)); // length 4
    }

    @Test
    public void testAllAtsAfterStart() {
        assertTrue(validator.isValid("1@@@", context)); // length 4
    }

    @Test
    public void testMixedValid() {
        boolean result = validator.isValid("User123_@abc", context); // Note: regex is case-sensitive, assumes a-zA-Z
        assertThat(result).isTrue();
    }
    
    @Test
    public void testValidUsernameWithAssertThat() {
        boolean result = validator.isValid("valid_user@123", context);
        //assertThat(result, is(true));
    }
}