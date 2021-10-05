package com.redhat.cloud.policies.app.model.validation;

import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActionValidatorTest {

    private ActionValidator testee;
    private ConstraintValidatorContext constraintValidatorContext;

    @BeforeEach
    void setUp() {
        this.testee = new ActionValidator();
        this.testee.initialize(null);

        this.constraintValidatorContext = mock(ConstraintValidatorContext.class);
    }

    @Test
    void shouldBeValidWhenInputIsEmpty() {
        assertTrue(testee.isValid("", constraintValidatorContext));
    }

    @Test
    void shouldBeValidWhenInputIsNull() {
        assertTrue(testee.isValid(null, constraintValidatorContext));
    }

    @Test
    void shouldBeValidWhenInputIsNotification() {
        assertTrue(testee.isValid("notification", constraintValidatorContext));
    }

    @Test
    void shouldBeInValidWhenInputIsNotificationWithUpperCase() {
        assertTrue(testee.isValid("Notification", constraintValidatorContext));
    }

    @Test
    void shouldBeValidWhenInputIsEmptyAndContainsSemicolon() {
        assertTrue(testee.isValid(" ; ", constraintValidatorContext));
    }

    @Test
    void shouldBeValidWhenInputIsEmptyAndContainsSemicolonsAndNotification() {
        assertTrue(testee.isValid(" ;notification", constraintValidatorContext));
    }

    @Test
    void shouldBeInValidWhenInputIsEmptyAndContainsSemicolonAndNotificationS() {
        assertFalse(testee.isValid(" ;notificationS", constraintValidatorContext));
    }

    @Test
    void shouldInValidWhenInputIsEmptyAndContainsColonAndNotification() {
        assertFalse(testee.isValid(" :notification", constraintValidatorContext));
    }
}