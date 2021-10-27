package com.redhat.cloud.policies.app.model.validation;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ActionValidatorTest {

    private final ActionValidator testee;
    private final ConstraintValidatorContext constraintValidatorContext;

    public ActionValidatorTest() {
        this.testee = new ActionValidator();
        this.testee.initialize(null);

        this.constraintValidatorContext = mock(ConstraintValidatorContext.class);
    }

    @Nested
    class ValidValidations {

        @ParameterizedTest
        @ValueSource(strings = {"", "notification", " ; ", " ;notification"})
        void shouldBeValid(String input) {
            assertTrue(testee.isValid(input, constraintValidatorContext));
        }

        @Test
        void shouldBeValidWhenInputIsNull() {
            assertTrue(testee.isValid(null, constraintValidatorContext));
        }
    }

    @Nested
    class InvalidValidations {

        @ParameterizedTest
        @ValueSource(strings = {"Notification", "NoTiFication", "NoTiFication123_13", " ;notificationS", " :notification"})
        void shouldBeInValid(String input) {
            assertFalse(testee.isValid(input, constraintValidatorContext));
        }
    }
}
