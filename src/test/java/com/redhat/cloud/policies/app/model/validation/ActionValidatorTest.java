package com.redhat.cloud.policies.app.model.validation;

import javax.validation.ConstraintValidatorContext;

import com.redhat.cloud.policies.app.EnvironmentInfo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionValidatorTest {

    private final ActionValidator testee;
    private final ConstraintValidatorContext constraintValidatorContext;

    EnvironmentInfo environmentInfo;

    public ActionValidatorTest() {
        this.environmentInfo = Mockito.mock(EnvironmentInfo.class);
        this.testee = new ActionValidator();
        this.testee.initialize(null);
        this.testee.environmentInfo = environmentInfo;
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
        void shouldBeInvalid(String input) {
            assertFalse(testee.isValid(input, constraintValidatorContext));
        }
    }

    @Nested
    class ValidFedrampValidations {

        @BeforeEach
        public void beforeEach() {
            when(environmentInfo.isFedramp()).thenReturn(true);
        }

        @ParameterizedTest
        @ValueSource(strings = { "", " ; "})
        void shouldBeValid(String input) {
            assertTrue(testee.isValid(input, constraintValidatorContext));
        }

        @Test
        void shouldBeValidWhenInputIsNull() {
            assertTrue(testee.isValid(null, constraintValidatorContext));
        }
    }

    @Nested
    class InvalidFedrampValidations {

        @BeforeEach
        public void beforeEach() {
            when(environmentInfo.isFedramp()).thenReturn(true);
        }

        @ParameterizedTest
        @ValueSource(strings = {"notification", " ;notification", "Notification", "NoTiFication", "NoTiFication123_13", " ;notificationS", " :notification"})
        void shouldBeInvalid(String input) {
            assertFalse(testee.isValid(input, constraintValidatorContext));
        }
    }
}
