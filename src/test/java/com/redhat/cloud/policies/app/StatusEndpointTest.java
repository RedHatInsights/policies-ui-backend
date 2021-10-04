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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.with;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class StatusEndpointTest extends AbstractITest {

    @Order(1)
    @Test
    void getStatusSimple() {

        when()
                .get("/api/policies/v1.0/status")
                .then()
                .statusCode(200);

        // Run twice
        String body = getMetric();
        assertTrue(body.contains("application_status_isDegraded 0.0"));

        body = getMetric();
        assertTrue(body.contains("application_status_isDegraded 0.0"));
    }

    @Order(2)
    @Test
    void getStatusDegraded() {

        with()
                .queryParam("status", "degraded")
                .accept("application/json")
                .contentType("application/json")
                .when()
                .post("/admin/status")
                .then()
                .statusCode(200);

        try {
            when()
                    .get("/api/policies/v1.0/status")
                    .then()
                    .body("admin-degraded", is("true"))
                    .statusCode(500);

            String body = getMetric();
            assertTrue(body.contains("application_status_isDegraded 1.0"));

            body = getMetric();
            assertTrue(body.contains("application_status_isDegraded 1.0"));

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

    private String getMetric() {
        return when()
                .get("/metrics/application/status_isDegraded")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
    }
}
