/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.cloud.policies.app;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.redhat.cloud.policies.app.NotificationSystem.UserPreferences;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.Header;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;

import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
class UserConfigServiceTest extends AbstractITest {

    private static final String PREFERENCE_URL = API_BASE_V1_0 + "/user-config/preferences";

    @InjectMock
    EnvironmentInfo environmentInfo;

    @BeforeAll
    static void setUpEnv() {
        setupRhId();
    }

    @Test
    void passNotificationResponse() {
        try {
            mockWithValue(false, false);
            UserPreferences preferences = getPreferences(authHeader);
            assertFalse(preferences.instant_email);
            assertFalse(preferences.daily_email);

            mockWithValue(true, false);
            preferences = getPreferences(authHeader);
            assertTrue(preferences.instant_email);
            assertFalse(preferences.daily_email);

            mockWithValue(true, true);
            preferences = getPreferences(authHeader);
            assertTrue(preferences.instant_email);
            assertTrue(preferences.daily_email);

            mockWithValue(false, true);
            preferences = getPreferences(authHeader);
            assertFalse(preferences.instant_email);
            assertTrue(preferences.daily_email);

        } finally {
            clearMockValue();
        }
    }

    @Test
    void testFedramp() {
        when(environmentInfo.isFedramp()).thenReturn(Boolean.TRUE);

        given()
                .header(authHeader)
                .when()
                .get(PREFERENCE_URL)
                .then()
                .statusCode(404);
    }

    private void clearMockValue() {
        mockServerClient.clear(HttpRequest.request()
                .withMethod("GET")
                .withPath("/api/notifications/v1.0/user-config/notification-preference/rhel/policies")
        );
    }

    private void mockWithValue(boolean instantEmail, boolean dailyEmail) {
        clearMockValue();
        UserPreferences preferences = new UserPreferences();
        preferences.instant_email = instantEmail;
        preferences.daily_email = dailyEmail;
        mockServerClient
                .when(request()
                        .withPath("/api/notifications/v1.0/user-config/notification-preference/rhel/policies")
                        .withMethod("GET")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(JsonBody.json(preferences))
                );
    }

    private UserPreferences getPreferences(Header authHeader) {
        return given()
                .header(authHeader)
                .when()
                .get(PREFERENCE_URL)
                .then()
                .statusCode(200)
                .extract().as(UserPreferences.class);
    }

}
