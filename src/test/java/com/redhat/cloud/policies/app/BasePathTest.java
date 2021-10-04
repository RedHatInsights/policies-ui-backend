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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@Tag("integration")
public class BasePathTest extends AbstractITest {

    @Test
    void checkBasePath404Json() {
        given()
                .header(authHeader)
                .accept(ContentType.JSON)
                .when()
                .get(API_BASE_V1_0)
                .then()
                .statusCode(404);
    }

    @Test
    void checkBasePath404Txt() {
        given()
                .header(authHeader)
                .accept(ContentType.TEXT)
                .when()
                .get(API_BASE_V1_0)
                .then()
                .statusCode(404);
    }

    @Test
    void checkBasePath404Html() {
        given()
                .header(authHeader)
                .accept(ContentType.HTML)
                .when()
                .get(API_BASE_V1_0)
                .then()
                .statusCode(404);
    }
}
