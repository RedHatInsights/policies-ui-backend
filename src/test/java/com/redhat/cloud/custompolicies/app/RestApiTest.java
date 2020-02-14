/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author hrupp
 */
@QuarkusTest
class RestApiTest {

  private static final String API_BASE = "/api/custom-policies/v1.0";
  @ClassRule
  private static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres");

  @ClassRule
  public static MockServerContainer mockEngineServer = new MockServerContainer();

  private static Header authHeader;
  private static MockServerClient mockServerClient;
  private static Header authRbacNoAccess; // Hans Dampf has no rbac access rights

  @BeforeAll
  static void configureMockEnvironment()  {
    setupPostgres();
    setupRhId();
    setupMockEngine();
  }

  // Helper to debug mock server issues
//  @AfterAll
//  static void mockLog() {
//    System.err.println(mockServerClient.retrieveLogMessages(request()));
//    System.err.println(mockServerClient.retrieveRecordedRequests(request()));
//  }

  private static void setupMockEngine() {
    // set up mock engine
    mockEngineServer.start();
    String mockServerUrl = "http://" + mockEngineServer.getContainerIpAddress() + ":" + mockEngineServer.getServerPort();
    System.err.println("Mock engine at " + mockServerUrl);
    mockServerClient = new MockServerClient(mockEngineServer.getContainerIpAddress(), mockEngineServer.getServerPort());
    mockServerClient
        .when(request()
            .withPath("/hawkular/alerts/triggers/trigger")
            .withHeader("Hawkular-Tenant","1234")
        )
        .respond(response()
            .withStatusCode(200)
            .withHeader("Content-Type","application/json")
            .withBody("{ \"msg\" : \"ok\" }")
        );
    mockServerClient
        .when(request()
            .withPath("/hawkular/alerts/triggers/.*")
            .withMethod("DELETE")
            .withHeader("Hawkular-Tenant","1234")
        )
        .respond(response()
            .withStatusCode(200)
            .withHeader("Content-Type","application/json")
            .withBody("{ \"msg\" : \"ok\" }")
        );
    mockServerClient
        .when(request()
            .withPath("/hawkular/alerts/triggers/trigger/.*")
            .withMethod("PUT")
            .withHeader("Hawkular-Tenant","1234")
        )
        .respond(response()
            .withStatusCode(200)
            .withHeader("Content-Type","application/json")
            .withBody("{ \"msg\" : \"ok\" }")
        );

    // RBac server
    String fullAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_full_access.json", false);
    String noAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_no_access.json", false);
    RestApiTest.mockServerClient
        .when(request()
                  .withPath("/api/rbac/v1/access/")
                  .withQueryStringParameter("application","custom-policies")
                  .withHeader("x-rh-identity",".*2UtZG9lLXVzZXIifQ==") // normal user all allowed
        )
        .respond(HttpResponse.response()
                     .withStatusCode(200)
                     .withHeader("Content-Type","application/json")
                     .withBody(fullAccessRbac)

        );
    RestApiTest.mockServerClient
        .when(request()
                  .withPath("/api/rbac/v1/access/")
                  .withQueryStringParameter("application","custom-policies")
                  .withHeader("x-rh-identity",".*kYW1wZi11c2VyIn0=") // hans dampf user nothing allowed
        )
        .respond(HttpResponse.response()
                     .withStatusCode(200)
                     .withHeader("Content-Type","application/json")
                     .withBody(noAccessRbac)
        );

    System.setProperty("engine/mp-rest/url",mockServerUrl);
    System.setProperty("rbac/mp-rest/url",mockServerUrl);


                         }

  private static void setupRhId() {
    // provide rh-id
    String rhid = HeaderHelperTest.getStringFromFile("rhid.txt",false);
    authHeader = new Header("x-rh-identity", rhid);
    rhid = HeaderHelperTest.getStringFromFile("rhid_hans.txt",false);
    authRbacNoAccess = new Header("x-rh-identity", rhid);
  }

  private static void setupPostgres() {
    postgreSQLContainer.start();
    // Now that postgres is started, we need to get its URL and tell Quarkus
    System.err.println("JDBC URL :" + postgreSQLContainer.getJdbcUrl());
    System.setProperty("quarkus.datasource.url", postgreSQLContainer.getJdbcUrl());
    System.setProperty("quarkus.datasource.username","test");
    System.setProperty("quarkus.datasource.password","test");

  }

  @AfterAll
  static void closePostgres() {
    postgreSQLContainer.stop();
  }

  @Test
  void testFactsNoAuth() {
    given()
        .when().get(API_BASE + "/facts")
        .then()
        .statusCode(401);
  }

  @Test
  void testBadAuth() {
    given()
        .header("x-rh-identity","frobnitz")
        .when().get(API_BASE + "/facts")
        .then()
        .statusCode(401);
  }

  @Test
  void testFactEndpoint() {
    given()
        .header(authHeader)
        .when().get(API_BASE + "/facts")
        .then()
        .statusCode(200)
        .body(containsString("os_release"));
  }

  @Test
  void testGetPolicies() {
    given()
        .header(authHeader)
        .when().get(API_BASE + "/policies/")
        .then()
        .statusCode(200)
        .body(containsString("2nd policy"));
  }

  @Test
  void testGetPoliciesSort() {
    given()
            .header(authHeader)
            .when().get(API_BASE + "/policies/?sortColumn=description")
            .then()
            .statusCode(200)
            .assertThat()
            .body("get(0).description", is("Another test"));
  }

  @Test
  void testGetPoliciesInvalidSort() {
    given()
            .header(authHeader)
            .when().get(API_BASE + "/policies/?sortColumn=foo")
            .then()
            .statusCode(400);
    //        .statusLine(containsString("Unknown Policy.SortableColumn requested: [foo]"));
  }

  @Test
  void testGetPoliciesFilter() {
    given()
            .header(authHeader)
            .when().get(API_BASE + "/policies/?filter[name]=Detect%&filter:op[name]=like")
            .then()
            .statusCode(200)
            .assertThat()
            .body("size()", is(1))
            .assertThat()
            .body("get(0).name", is("Detect Nice box"));
  }

  @Test
  void testGetPoliciesFilterILike() {
    given()
            .header(authHeader)
            .when().get(API_BASE + "/policies/?filter[name]=detect%&filter:op[name]=ilike")
            .then()
            .statusCode(200)
            .assertThat()
            .body("size()", is(1))
            .assertThat()
            .body("get(0).name", is("Detect Nice box"));
  }

  @Test
  void testGetPoliciesInvalidFilter() {
    given()
            .header(authHeader)
            .when().get(API_BASE + "/policies/?filter[actions]=email&filter:op[name]=ilike")
            .then()
            .statusCode(400);
  }


  @Test
  void testGetPoliciesForUnknownAccount() {
    given()
        .when().get(API_BASE + "/policies/")
        .then()
        .statusCode(401);
  }

  @Test
  void testGetOnePolicy() {
    JsonPath jsonPath =
    given()
        .header(authHeader)
        .when().get(API_BASE + "/policies/1")
        .then()
        .statusCode(200)
        .body(containsString("1st policy"))
        .extract().jsonPath();

    TestPolicy policy = jsonPath.getObject("", TestPolicy.class);
    Assert.assertEquals("Action does not match", "EMAIL roadrunner@acme.org", policy.actions);
    Assert.assertEquals("Conditions do not match", "\"cores\" == 1", policy.conditions);
    Assert.assertTrue("Policy is not enabled", policy.isEnabled);
  }

  @Test
  void testGetOnePolicyNoAccess() {
    given()
        .header(authRbacNoAccess)
        .when().get(API_BASE + "/policies/1")
        .then()
        .statusCode(403);
  }

  @Test
  void testGetOneBadPolicy() {
    given()
        .header(authHeader)
        .when().get(API_BASE + "/policies/15")
        .then()
        .statusCode(404);
  }

  @Test
  void storeNewPolicy() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL roadrunner@acme.org";
    tp.conditions = "cores = 2";
    tp.name = "test1";

    Headers headers =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore","true")
      .when().post(API_BASE + "/policies")
        .then()
        .statusCode(201)
        .extract().headers()
        ;

    assert headers.hasHeaderWithName("Location");
    // Extract location and then check in subsequent call
    // that the policy is stored
    Header locationHeader = headers.get("Location");
    String location = locationHeader.getValue();
    // location is  a full url to the new resource.
    System.out.println(location);

    JsonPath body =
    given()
        .header(authHeader)
        .when().get(location)
        .then()
        .statusCode(200)
        .extract().body()
        .jsonPath();

    assert body.get("conditions").equals("cores = 2");
    assert body.get("name").equals("test1");

    // now delete it again
    given()
        .header(authHeader)
      .when().delete(location)
        .then()
        .statusCode(200);
  }

  @Test
  void storeNewPolicyNoRbac() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL roadrunner@acme.org";
    tp.conditions = "cores = 2";
    tp.name = "test1";

    given()
        .header(authRbacNoAccess)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore", "true")
        .when().post(API_BASE + "/policies")
        .then()
        .statusCode(403);
  }

  @Test
  void storeAndUpdatePolicy() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL roadrunner@acme.org";
    tp.conditions = "cores = 2";
    tp.name = "test2";
    tp.triggerId ="123-abc";

    Headers headers =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore","true")
      .when().post(API_BASE + "/policies")
        .then()
        .statusCode(201)
        .extract().headers()
        ;

    assert headers.hasHeaderWithName("Location");
    // Extract location and then check in subsequent call
    // that the policy is stored
    Header locationHeader = headers.get("Location");
    String location = locationHeader.getValue();
    // location is  a full url to the new resource.
    System.out.println(location);

    String resp =
    given()
        .header(authHeader)
      .when().get(location)
        .then()
        .statusCode(200)
        .extract()
        .body()
        .asString();

    Jsonb jsonb = JsonbBuilder.create();
    TestPolicy ret = jsonb.fromJson(resp,TestPolicy.class);
    assert tp.triggerId.equals(ret.triggerId);

    try {
      // update
      ret.conditions = "cores = 3";
      given()
          .header(authHeader)
          .contentType(ContentType.JSON)
          .body(ret)
        .when().put(location)
          .then()
          .statusCode(200);

      JsonPath jsonPath =
          given()
              .header(authHeader)
              .when().get(location)
              .then()
              .statusCode(200)
              .extract().body().jsonPath();
      String content = jsonPath.getString("conditions");
      assert content.equalsIgnoreCase("cores = 3");

    }
    finally {
      // now delete it again
      given()
          .header(authHeader)
          .when().delete(location)
          .then()
          .statusCode(200);
    }
  }

  // Check that update is protected by RBAC.
  // we need to store as user with access first.
  @Test
  void storeAndUpdatePolicyNoUpdateAccess() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL roadrunner@acme.org";
    tp.conditions = "cores = 2";
    tp.name = "test2";
    tp.triggerId ="123-abc";

    Headers headers =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore","true")
      .when().post(API_BASE + "/policies")
        .then()
        .statusCode(201)
        .extract().headers()
        ;

    assert headers.hasHeaderWithName("Location");
    // Extract location and then check in subsequent call
    // that the policy is stored
    Header locationHeader = headers.get("Location");
    String location = locationHeader.getValue();
    // location is  a full url to the new resource.
    System.out.println(location);

    String resp =
    given()
        .header(authHeader)
      .when().get(location)
        .then()
        .statusCode(200)
        .extract()
        .body()
        .asString();

    Jsonb jsonb = JsonbBuilder.create();
    TestPolicy ret = jsonb.fromJson(resp,TestPolicy.class);
    assert tp.triggerId.equals(ret.triggerId);

    try {
      // update
      ret.conditions = "cores = 3";
      given()
          .header(authRbacNoAccess)
          .contentType(ContentType.JSON)
          .body(ret)
        .when().put(location)
          .then()
          .statusCode(403);

    }
    finally {
      // now delete it again
      given()
          .header(authHeader)
          .when().delete(location)
          .then()
          .statusCode(200);
    }
  }


  @Test
  void deletePolicy() {

    given()
        .header(authHeader)
      .when().delete(API_BASE + "/policies/3")
        .then()
        .statusCode(200)
        ;

    // Now check that it is gone
    given()
        .header(authHeader)
      .when().get(API_BASE + "/policies/3")
        .then()
        .statusCode(404);
  }

  @Test
  void deletePolicyNoRbacAccess() {

    given()
        .header(authRbacNoAccess)
        .when().delete(API_BASE + "/policies/3")
        .then()
        .statusCode(403)
    ;

  }

  @Test
  void testOpenApiEndpoint() {
    given()
        .header("Accept",ContentType.JSON)
        .when()
        .get(API_BASE + "/openapi.json")
        .then()
        .statusCode(200)
        .contentType("application/json");

  }
}
