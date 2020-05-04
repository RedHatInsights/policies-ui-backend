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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import io.restassured.path.json.JsonPath;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockserver.model.HttpRequest;

/**
 * @author hrupp
 */
@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserConfigServiceTest extends AbstractITest {

  private static final String PREFERENCE_URL = API_BASE_V1_0 + "/user-config/email-preference";

  @BeforeAll
  static void setUpEnv() {
    setupRhId();
  }

  @Test()
  @Order(1)
  public void getNoSettingsYet() {

    JsonPath jsonPath = getJsonPath(authHeader, 200);

    Assert.assertEquals(false, jsonPath.get("[0].fields[0].initialValue"));
    Assert.assertEquals("immediateEmail",jsonPath.get("[0].fields[0].name"));

    Assert.assertEquals(false, jsonPath.get("[0].fields[1].initialValue"));
    Assert.assertEquals("dailyEmail",jsonPath.get("[0].fields[1].name"));


  }

  @Test()
  @Order(2)
  public void setAndGet() {

    String payload = "{  \"immediateEmail\": true, \"dailyEmail\": false }";
    sendPayload(payload, authHeader, 200);

    JsonPath jsonPath = getJsonPath(authHeader, 200);
    Assert.assertEquals(true,jsonPath.get("[0].fields[0].initialValue"));
    Assert.assertEquals("immediateEmail",jsonPath.get("[0].fields[0].name"));

    Assert.assertEquals(false,jsonPath.get("[0].fields[1].initialValue"));
    Assert.assertEquals("dailyEmail",jsonPath.get("[0].fields[1].name"));

  }

  @Test()
  @Order(3)
  public void updateAndGet() {

    String payload = "{  \"immediateEmail\": false, \"dailyEmail\": true }";
    sendPayload(payload, authHeader, 200);

    JsonPath jsonPath = getJsonPath(authHeader, 200);
    Assert.assertEquals(false,jsonPath.get("[0].fields[0].initialValue"));
    Assert.assertEquals("immediateEmail",jsonPath.get("[0].fields[0].name"));

    Assert.assertEquals(true,jsonPath.get("[0].fields[1].initialValue"));
    Assert.assertEquals("dailyEmail",jsonPath.get("[0].fields[1].name"));

  }

  @Test()
  @Order(4)
  public void setAndNoRbac() {

    String payload = "{  \"immediateEmail\": true, \"dailyEmail\": false }";
    sendPayload(payload, authRbacNoAccess, 403);
  }

  @Test()
  @Order(5)
  public void getAndNoRbac() {

    getJsonPath(authRbacNoAccess,403);
  }

  // This needs to run as last test in here, as it clears out
  // the route to the (mocked) notification pod, so that the
  // value can not be set.
  // See also TestLifecycleManager.invoke()
  @Test()
  @Order(2999)
  public void setAndFail() {

    mockServerClient.clear(HttpRequest.request()
                 .withMethod("PUT")
                 .withPath("/endpoints/email/subscription/.*")
    );

    String payload = "{  \"immediateEmail\": true, \"dailyEmail\": false }";
    sendPayload(payload, authHeader, 500);

  }

  private void sendPayload(String payload, Header authHeader, int expectedStatus) {
    given()
        .header(authHeader)
        .contentType("application/json")
        .body(payload)
        .when().post(PREFERENCE_URL)
        .then()
        .statusCode(expectedStatus);
  }

  private JsonPath getJsonPath(Header authHeader, int expectedCode) {
    return given()
        .header(authHeader)
        .when()
        .get(PREFERENCE_URL)
        .then()
        .statusCode(expectedCode)
        .extract().jsonPath();
  }

}
