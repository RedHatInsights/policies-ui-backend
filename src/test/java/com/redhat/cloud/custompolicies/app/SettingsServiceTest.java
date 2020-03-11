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
package com.redhat.cloud.custompolicies.app;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author hrupp
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SettingsServiceTest extends AbstractITest {

  private static final String PREFERENCE_URL = API_BASE + "/settings";

  @ClassRule
  private static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres");

  @ClassRule
  public static MockServerContainer mockEngineServer = new MockServerContainer();

  @BeforeAll
  static void setUpEnv() {
    setupPostgres(postgreSQLContainer);
    setupRhId();
    setupMockEngine(mockEngineServer);
  }

  @AfterAll
  static void closePostgres() {
  //  postgreSQLContainer.stop();
  }


  @Test()
  @Order(1)
  public void getNoSettingsYet() {

    JsonPath jsonPath = getJsonPath();

    Assert.assertEquals(false, jsonPath.get("[0].fields[0].initialValue"));
    Assert.assertEquals("immediateEmail",jsonPath.get("[0].fields[0].name"));

    Assert.assertEquals(false, jsonPath.get("[0].fields[1].initialValue"));
    Assert.assertEquals("dailyEmail",jsonPath.get("[0].fields[1].name"));


  }

  @Test()
  @Order(2)
  public void setAndGet() {

    String payload = "{  \"immediateEmail\": true, \"dailyEmail\": false }";
    sendPayload(payload);

    JsonPath jsonPath = getJsonPath();
    Assert.assertEquals(true,jsonPath.get("[0].fields[0].initialValue"));
    Assert.assertEquals("immediateEmail",jsonPath.get("[0].fields[0].name"));

    Assert.assertEquals(false,jsonPath.get("[0].fields[1].initialValue"));
    Assert.assertEquals("dailyEmail",jsonPath.get("[0].fields[1].name"));

  }

  @Test()
  @Order(3)
  public void updateAndGet() {

    String payload = "{  \"immediateEmail\": false, \"dailyEmail\": true }";
    sendPayload(payload);

    JsonPath jsonPath = getJsonPath();
    Assert.assertEquals(false,jsonPath.get("[0].fields[0].initialValue"));
    Assert.assertEquals("immediateEmail",jsonPath.get("[0].fields[0].name"));

    Assert.assertEquals(true,jsonPath.get("[0].fields[1].initialValue"));
    Assert.assertEquals("dailyEmail",jsonPath.get("[0].fields[1].name"));

  }

  private ValidatableResponse sendPayload(String payload) {
    return given()
        .header(authHeader)
        .contentType("application/json")
        .body(payload)
        .when().post(PREFERENCE_URL)
        .then()
        .statusCode(200);
  }

  private JsonPath getJsonPath() {
    return given()
        .header(authHeader)
        .when()
        .get(PREFERENCE_URL)
        .then()
        .statusCode(200)
        .extract().jsonPath();
  }

}
