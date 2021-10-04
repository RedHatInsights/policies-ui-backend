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

import static org.hamcrest.CoreMatchers.containsString;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Test if the redirector works as expected api/v1/ -> api/v1.0/
 */
@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@Tag("integration")
public class RestApiRedirectTest extends AbstractITest {

    @Test
    void testGetOnePolicyApiV1Redirect() {
        JsonPath jsonPath = given().header(authHeader).when()
                .get(API_BASE_V1 + "/policies/bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c").then().statusCode(200)
                .body(containsString("1st policy")).extract().jsonPath();

        TestPolicy policy = jsonPath.getObject("", TestPolicy.class);
        Assert.assertEquals("Action does not match", "NOTIFICATION roadrunner@acme.org", policy.actions);
        Assert.assertEquals("Conditions do not match", "\"cores\" == 1", policy.conditions);
        Assert.assertTrue("Policy is not enabled", policy.isEnabled);
    }

    @Test
    void storeAndDeleteNewPolicy() {
        TestPolicy tp = new TestPolicy();
        tp.actions = "NOTIFICATION";
        tp.conditions = "cores = 2";
        tp.name = "test1-redirect";

        ExtractableResponse<Response> er = given().header(authHeader).contentType(ContentType.JSON).body(tp)
                .queryParam("alsoStore", "true").when().post(API_BASE_V1 + "/policies").then().statusCode(201)
                .extract();

        TestPolicy returnedBody = er.body().as(TestPolicy.class);
        try {
            Assert.assertEquals("cores = 2", returnedBody.conditions);
            Assert.assertEquals("test1-redirect", returnedBody.name);
        } finally {
            given().header(authHeader).when().delete(API_BASE_V1 + "/policies/" + returnedBody.id).then()
                    .statusCode(200);
        }
    }

    @Test
    void testGetPoliciesPaged4() {

        JsonPath jsonPath = given().header(authHeader).when().get(API_BASE_V1 + "/policies/?limit=5&offset=2").then()
                .statusCode(200).extract().body().jsonPath();

        long policiesInDb = countPoliciesInDB();
        Assert.assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        Assert.assertEquals(links.size(), 4);
        extractAndCheck(links, "first", 5, 0);
        extractAndCheck(links, "prev", 5, 0);
        extractAndCheck(links, "next", 5, 7);
    }

    @Test
    void testGetOpenApi() {
        given().header(authHeader).when().get(API_BASE_V1 + "/openapi.json").then().statusCode(200)
                .contentType("application/json");
    }
}
