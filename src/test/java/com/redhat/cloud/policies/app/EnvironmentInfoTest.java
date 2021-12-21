package com.redhat.cloud.policies.app;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class EnvironmentInfoTest {

    @InjectSpy
    EnvironmentInfo environmentInfo;

    @Test
    void testIsFedramp() {
        updateField(environmentInfo, "environmentName", Optional.of("prod"), EnvironmentInfo.class);
        assertFalse(environmentInfo.isFedramp());

        updateField(environmentInfo, "environmentName", Optional.of("stage"), EnvironmentInfo.class);
        assertFalse(environmentInfo.isFedramp());

        updateField(environmentInfo, "environmentName", Optional.of("fedramp-prod"), EnvironmentInfo.class);
        assertTrue(environmentInfo.isFedramp());

        updateField(environmentInfo, "environmentName", Optional.of("fedramp-stage"), EnvironmentInfo.class);
        assertTrue(environmentInfo.isFedramp());

        updateField(environmentInfo, "environmentName", Optional.empty(), EnvironmentInfo.class);
        assertFalse(environmentInfo.isFedramp());
    }

    private static <T, V> void updateField(T object, String field, V value, Class<T> klass) {
        try {
            Field bopUrlField = klass.getDeclaredField(field);
            bopUrlField.setAccessible(true);
            bopUrlField.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            String error = String.format("Error while updating value of field: [%s] to [%s].", field, value);

            if (e instanceof NoSuchFieldException) {
                if (object.getClass() == klass) {
                    error += "\nTry specifying the class you want instead of using theObject.getClass(). Mocked objects use a different class.";
                }
            }
            throw new RuntimeException(error, e);
        }
    }

}
