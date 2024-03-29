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

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.with;

@QuarkusTest
class HealthCheckTest {

    @Test
    void testNormalHealth() {
        when()
                .get("/health")
                .then()
                .statusCode(200);
    }

    @Test
    void testAdminDown() {
        with()
                .queryParam("status", "admin-down")
                .accept("application/json")
                .contentType("application/json")
                .when()
                .post("/admin/status")
                .then()
                .statusCode(200);

        try {
            when()
                    .get("/health")
                    .then()
                    .statusCode(503);
        } finally {
            with()
                    .queryParam("status", "ok")
                    .accept("application/json")
                    .contentType("application/json")
                    .when()
                    .post("/admin/status")
                    .then()
                    .statusCode(200);
        }
    }
}
