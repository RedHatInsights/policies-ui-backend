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
package com.redhat.cloud.policies.app;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author hrupp
 */
@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
class RestApiTest extends AbstractITest {

  @Inject
  TestUUIDHelperBean uuidHelper;

  @AfterEach
  void cleanUUID() {
    uuidHelper.clearUUID();
  }


  @Test
  void testFactsNoAuth() {
    given()
        .when().get(API_BASE_V1_0 + "/facts")
        .then()
        .statusCode(401);
  }

  @Test
  void testBadAuth() {
    given()
        .header("x-rh-identity","frobnitz")
        .when().get(API_BASE_V1_0 + "/facts")
        .then()
        .statusCode(401);
  }

  @Test
  void testFactEndpoint() {
    given()
        .header(authHeader)
        .when().get(API_BASE_V1_0 + "/facts")
        .then()
        .statusCode(200)
        .body(containsString("os_release"));
  }

  @Test
  void testGetPolicies() {

    long numberOfPolicies = countPoliciesInDB();

    JsonPath jsonPath =
    given()
        .header(authHeader)
        .when().get(API_BASE_V1_0 + "/policies/")
        .then()
        .statusCode(200)
        .extract().body().jsonPath();

    Assert.assertEquals(numberOfPolicies, jsonPath.getList("data").size());
    Map<String,Object> data = (Map<String, Object>) jsonPath.getList("data").get(0);
    Assert.assertTrue(data.containsKey("lastEvaluation"));
  }

  @Test
  void testGetPoliciesNoAuth() {

    given()
        .header(authRbacNoAccess)
      .when()
        .get(API_BASE_V1_0 + "/policies/")
      .then()
        .statusCode(403);
  }

  @Test
  void testGetPolicyIds() {

    long numberOfPolicies = countPoliciesInDB();

    JsonPath jsonPath =
    given()
        .header(authHeader)
        .when().get(API_BASE_V1_0 + "/policies/ids")
        .then()
        .statusCode(200)
        .extract().body().jsonPath();

    Assert.assertEquals(numberOfPolicies, jsonPath.getList("").size());
  }

  @Test
  void testGetPolicyIdsBadAuth() {

    given()
        .header(authRbacNoAccess)
      .when()
        .get(API_BASE_V1_0 + "/policies/ids")
      .then()
        .statusCode(403);
  }

  @Test
  void testGetPolicyIdsBadFilter() {

    given()
        .header(authHeader)
      .when()
        .get(API_BASE_V1_0 + "/policies/ids?limit=a")
      .then()
        .statusCode(400);
  }


  @Test
  void testGetPoliciesSort() {
    given()
            .header(authHeader)
      .when()
        .get(API_BASE_V1_0 + "/policies/?sortColumn=description")
            .then()
            .statusCode(200)
            .assertThat()
            .body(" data.get(0).description", is("Another test"));
  }

  @Test
  void testGetPoliciesBadPaged() {

    given()
        .header(authHeader)
      .when()
        .get(API_BASE_V1_0 + "/policies/?limit=XX")
      .then()
        .statusCode(400);
  }

  @Test
  void testGetPoliciesPaged1() {

    JsonPath jsonPath =
    given()
            .header(authHeader)
          .when()
            .get(API_BASE_V1_0 + "/policies/?limit=10")
          .then()
            .statusCode(200)
            .extract().body().jsonPath();

    long policiesInDb = countPoliciesInDB();
    Assert.assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
    Map<String, String> links = jsonPath.get("links");
    Assert.assertEquals(3, links.size());
    extractAndCheck(links,"first",10,0);
    extractAndCheck(links,"last",10,10);
    extractAndCheck(links,"next",10,10);
  }
  @Test
  void testGetPoliciesPaged2() {

    JsonPath jsonPath =
    given()
            .header(authHeader)
          .when()
            .get(API_BASE_V1_0 + "/policies/?limit=5")
          .then()
            .statusCode(200)
            .extract().body().jsonPath();

    long policiesInDb = countPoliciesInDB();
    Assert.assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
    Map<String, String> links = jsonPath.get("links");
    Assert.assertEquals(links.size(),3);
    extractAndCheck(links,"first",5,0);
    extractAndCheck(links,"last",5,10);
    extractAndCheck(links,"next",5,5);
  }

  @Test
  void testGetPoliciesPaged3() {

    JsonPath jsonPath =
    given()
            .header(authHeader)
          .when()
            .get(API_BASE_V1_0 + "/policies/?limit=5&offset=5")
          .then()
            .statusCode(200)
            .extract().body().jsonPath();

    long policiesInDb = countPoliciesInDB();
    Assert.assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
    Map<String, String> links = jsonPath.get("links");
    Assert.assertEquals(links.size(),4);
    extractAndCheck(links,"first",5,0);
    extractAndCheck(links,"prev",5,0);
    extractAndCheck(links,"next",5,10);
    extractAndCheck(links,"last",5,10);
  }

  @Test
  void testGetPolicyIdsPaged3() {

    JsonPath jsonPath =
    given()
            .header(authHeader)
          .when()
            .get(API_BASE_V1_0 + "/policies/ids?limit=5&offset=5")
          .then()
            .statusCode(200)
            .extract().body().jsonPath();

    long policiesInDb = countPoliciesInDB();
    Assert.assertEquals(policiesInDb, jsonPath.getList("").size());
  }

  @Test
  void testGetPoliciesPaged4() {

    JsonPath jsonPath =
    given()
            .header(authHeader)
          .when()
            .get(API_BASE_V1_0 + "/policies/?limit=5&offset=2")
          .then()
            .statusCode(200)
            .extract().body().jsonPath();

    long policiesInDb = countPoliciesInDB();
    Assert.assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
    Map<String, String> links = jsonPath.get("links");
    Assert.assertEquals(links.size(),4);
    extractAndCheck(links,"first",5,0);
    extractAndCheck(links,"prev",5,0);
    extractAndCheck(links,"next",5,7);
    extractAndCheck(links,"last",5,10);
  }

  @Test
  void testGetPoliciesWithNoLimit() {
    JsonPath jsonPath =
            given()
                    .header(authHeader)
                    .when()
                    .get(API_BASE_V1_0 + "/policies/?limit=-1&offset=0")
                    .then()
                    .statusCode(200)
                    .extract().body().jsonPath();

    List<?> data = jsonPath.get("data");

    long policiesInDb = countPoliciesInDB();

    Assert.assertEquals(policiesInDb, data.size());
    Assert.assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
    Map<String, String> links = jsonPath.get("links");
    Assert.assertEquals(links.size(), 2);
    extractAndCheck(links,"first",-1,0);
    extractAndCheck(links,"last",-1,0);
  }

  @Test
  void testGetPoliciesWithNoLimitIgnoresOffset() {
    JsonPath jsonPath =
            given()
                    .header(authHeader)
                    .when()
                    .get(API_BASE_V1_0 + "/policies/?limit=-1&offset=12321")
                    .then()
                    .statusCode(200)
                    .extract().body().jsonPath();

    List<?> data = jsonPath.get("data");

    long policiesInDb = countPoliciesInDB();

    Assert.assertEquals(policiesInDb, data.size());
    Assert.assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
    Map<String, String> links = jsonPath.get("links");
    Assert.assertEquals(links.size(), 2);
    extractAndCheck(links,"first",-1,0);
    extractAndCheck(links,"last",-1,0);
  }

  @Test
  void testGetPoliciesInvalidSort() {
    given()
            .header(authHeader)
            .when().get(API_BASE_V1_0 + "/policies/?sortColumn=foo")
            .then()
            .statusCode(400);
    //        .statusLine(containsString("Unknown Policy.SortableColumn requested: [foo]"));
  }

  @Test
  void testGetPoliciesFilter() {
    given()
            .header(authHeader)
            .when().get(API_BASE_V1_0 + "/policies/?filter[name]=Detect%&filter:op[name]=like")
            .then()
            .statusCode(200)
            .assertThat()
            .body("data.size()", is(1))
            .assertThat()
            .body("data.get(0).name", is("Detect Nice box"));
  }

  @Test
  void testGetPoliciesFilter2() {
    given()
            .header(authHeader)
            .when().get(API_BASE_V1_0 + "/policies/?filter[is_enabled]=true")
            .then()
            .statusCode(200)
            .assertThat()
            .body("data.size()", is(11));
  }

  @Test
  void testGetPoliciesFilter3() {
    given()
            .header(authHeader)
            .when().get(API_BASE_V1_0 + "/policies/?filter[is_enabled]")
            .then()
            .statusCode(200)
            .assertThat()
            .body("data.size()", is(11));
  }

  @Test
  void testGetPolicyIdsFilter() {
    given()
          .header(authHeader)
        .when()
          .get(API_BASE_V1_0 + "/policies/ids?filter[name]=Detect%&filter:op[name]=like")
        .then()
          .statusCode(200)
        .assertThat()
          .body("size()", is(1))
        .assertThat()
          .body("get(0)", is("f36aa564-ffc8-48c6-a27f-31ddd4c16c8b"));
  }

  @Test
  void testGetPolicyIdsEnabledFilter1() {

    int size =
    given()
          .header(authHeader)
        .when()
          .get(API_BASE_V1_0 + "/policies/ids?filter[is_enabled]=true")
        .then()
          .statusCode(200)
        .extract().body().jsonPath().getInt("size()");

    Assert.assertTrue(size >= 8);
  }

  @Test
  void testGetPolicyIdsEnabledFilter2() {
    int size =
    given()
          .header(authHeader)
        .when()
          .get(API_BASE_V1_0 + "/policies/ids?filter[is_enabled]=false")
        .then()
          .statusCode(200)
        .extract().body().jsonPath().getInt("size()");

    Assert.assertTrue(size <= 3);
  }


  @Test
  void testGetPoliciesFilterILike() {
    given()
            .header(authHeader)
            .when().get(API_BASE_V1_0 + "/policies/?filter[name]=detect%&filter:op[name]=ilike")
            .then()
            .statusCode(200)
            .assertThat()
            .body("data.size()", is(1))
            .assertThat()
            .body("data.get(0).name", is("Detect Nice box"));
  }

  @Test
  void testGetPoliciesInvalidFilter() {
    given()
            .header(authHeader)
            .when().get(API_BASE_V1_0 + "/policies/?filter[actions]=email&filter:op[name]=ilike")
            .then()
            .statusCode(400);
  }

  @Test
  void testGetPoliciesInvalidFilter2() {
    given()
        .header(authHeader)
      .when()
        .get(API_BASE_V1_0 + "/policies/?filter[name]=email&filter:op[name]=boolean_is")
      .then()
        .statusCode(400);
  }


  @Test
  void testGetPoliciesForUnknownAccount() {
    given()
        .when().get(API_BASE_V1_0 + "/policies/")
        .then()
        .statusCode(401);
  }

  @Test
  void testGetOnePolicy() {
    JsonPath jsonPath =
    given()
        .header(authHeader)
        .when().get(API_BASE_V1_0 + "/policies/bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
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
        .when().get(API_BASE_V1_0 + "/policies/bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
        .then()
        .statusCode(403);
  }

  @Test
  void testGetOneBadPolicy() {
    given()
        .header(authHeader)
        .when().get(API_BASE_V1_0 + "/policies/15")
        .then()
        .statusCode(404);
  }

  @Test
  void storeNewEmptyPolicy() {

    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .queryParam("alsoStore", "true")
      .when()
        .post(API_BASE_V1_0 + "/policies")
      .then()
        .statusCode(400)
      ;
  }

  @Test
  void storeNewPolicy() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL";
    tp.conditions = "cores = 2";
    tp.name = "test1";

    ExtractableResponse<Response> er =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore","true")
      .when().post(API_BASE_V1_0 + "/policies")
        .then()
        .statusCode(201)
        .extract()
        ;

  Headers headers = er.headers();

  assert headers.hasHeaderWithName("Location");
  // Extract location and then check in subsequent call
  // that the policy is stored
  Header locationHeader = headers.get("Location");
  String location = locationHeader.getValue();
  // location is  a full url to the new resource.
  System.out.println(location);

    try {
      TestPolicy returnedBody = er.body().as(TestPolicy.class);
      Assert.assertNotNull(returnedBody);
      Assert.assertEquals("cores = 2", returnedBody.conditions);
      Assert.assertEquals("test1", returnedBody.name);

      JsonPath body =
          given()
              .header(authHeader)
            .when()
              .get(location)
            .then()
              .statusCode(200)
              .extract().body()
              .jsonPath();

      assert body.get("conditions").equals("cores = 2");
      assert body.get("name").equals("test1");
      Assert.assertEquals(body.get("id").toString(), returnedBody.id.toString());
    } finally {
      // now delete it again
      given()
          .header(authHeader)
          .when().delete(location)
          .then()
          .statusCode(200);
    }
  }

  @Test
  void storeNewPolicyWithLongName() {
    TestPolicy tp = new TestPolicy();
    tp.conditions = "cores = 2";
    tp.name = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis pa";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(tp)
            .queryParam("alsoStore","false")
            .when().post(API_BASE_V1_0 + "/policies")
            .then()
            .statusCode(400);
    Assert.assertTrue(tp.name.length() > 150);
  }

  @Test
  void editPolicyWithLongName() {
    TestPolicy tp = new TestPolicy();
    tp.id = UUID.fromString("bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c");
    tp.conditions = "cores = 2";
    tp.name = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis pa";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(tp)
            .queryParam("dry","true")
            .when().put(API_BASE_V1_0 + "/policies/bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
            .then()
            .statusCode(400);
    Assert.assertTrue(tp.name.length() > 150);
  }

  @Test
  void storeNewPolicyEngineProblem() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL";
    tp.conditions = "cores = 2";
    tp.name = "test1";
    // Use an explicit ID; that the mock server knows
    String uuid = "c49e92c4-dead-beef-9200-245b31933e94";
    uuidHelper.storeUUIDString(uuid);

    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore", "true")
      .when()
        .post(API_BASE_V1_0 + "/policies")
      .then()
        .statusCode(400);
  }

  @Test
  void storeNewPolicyNoActions() {
    TestPolicy tp = new TestPolicy();
    tp.conditions = "cores = 2";
    tp.name = UUID.randomUUID().toString();

    TestPolicy policy = given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore", "true")
      .when()
        .post(API_BASE_V1_0 + "/policies")
      .then()
        .statusCode(201)
        .extract().body().as(TestPolicy.class);

    deletePolicyById(policy.id.toString());
  }

  @Test
  void storeNewPolicyEmptyActions1() {
    TestPolicy tp = new TestPolicy();
    tp.conditions = "cores = 2";
    tp.name = UUID.randomUUID().toString();
    tp.actions = "";

    TestPolicy policy = given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore", "true")
      .when()
        .post(API_BASE_V1_0 + "/policies")
      .then()
        .statusCode(201)
        .extract().body().as(TestPolicy.class);

    deletePolicyById(policy.id.toString());
  }

  @Test
  void storeNewPolicyEmptyActions2() {
    TestPolicy tp = new TestPolicy();
    tp.conditions = "cores = 2";
    tp.name = UUID.randomUUID().toString();
    tp.actions = "; ";

    TestPolicy policy =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore", "true")
      .when()
        .post(API_BASE_V1_0 + "/policies")
      .then()
        .statusCode(201)
        .extract().body().as(TestPolicy.class);

    deletePolicyById(policy.id.toString());
  }

  @Test
  void storeNewPolicyBadActions() {
    TestPolicy tp = new TestPolicy();
    tp.conditions = "cores = 2";
    tp.name = UUID.randomUUID().toString();
    tp.actions = "hula";

    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
      .when()
        .post(API_BASE_V1_0 + "/policies")
      .then()
        .statusCode(400);
  }

  @Test
  void storeNewPolicyNoRbac() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL;webhook";
    tp.conditions = "cores = 2";
    tp.name = UUID.randomUUID().toString();

    given()
        .header(authRbacNoAccess)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore", "true")
      .when()
        .post(API_BASE_V1_0 + "/policies")
      .then()
        .statusCode(403);
  }

  @Test
  void storeAndUpdatePolicy() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL";
    tp.conditions = "cores = 2";
    tp.name = "test2";

    String str = "00000000-0000-0000-0000-000000000001";
    UUID testUUID = UUID.fromString(str);
    uuidHelper.storeUUID(testUUID);

    Headers headers =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore","true")
      .when().post(API_BASE_V1_0 + "/policies")
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
    Assert.assertTrue(location.endsWith(testUUID.toString()));

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
    Assert.assertEquals(tp.conditions,ret.conditions);

    Assert.assertNotNull(ret.ctime);
    Assert.assertNotNull(ret.mtime);
    String storeTime = ret.ctime; // keep for below
    Timestamp ctime = Timestamp.valueOf(ret.ctime);
    Timestamp mtime1 = Timestamp.valueOf(ret.mtime);
    // ctime and mtime oftern differ a tiny bit, which makes the nanos differ. Let's compare with some slack
    Assert.assertTrue("Ctime: " + ctime + ", mtime: " + mtime1 , Math.abs(mtime1.getTime()  - ctime.getTime()) < 2);

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

      Assert.assertEquals(storeTime,jsonPath.getString("ctime"));
      Assert.assertNotEquals(storeTime,jsonPath.getString("mtime"));
      Timestamp mtime2 = Timestamp.valueOf(jsonPath.getString("mtime"));
      Assert.assertTrue(ctime.before(mtime2));
      Assert.assertTrue(mtime1.before(mtime2));

    }
    finally {
      // now delete it again
      given()
          .header(authHeader)
        .when()
          .delete(location)
          .then()
          .statusCode(200);
    }
  }

  @Test
  void storeAndEnableDisablePolicy() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "EMAIL";
    tp.conditions = "cores = 2";
    tp.name = "test2";
    tp.isEnabled = false;

    TestPolicy testPolicy =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore","true")
      .when().post(API_BASE_V1_0 + "/policies")
        .then()
        .statusCode(201)
        .extract().body().as(TestPolicy.class)
        ;

    String mt = testPolicy.mtime;
    Timestamp t1 = Timestamp.valueOf(mt);

    try {
      // Now enable
      given()
          .header(authHeader)
          .contentType(ContentType.JSON)
          .queryParam("enabled",true)
          .when().post(API_BASE_V1_0 + "/policies/" + testPolicy.id + "/enabled")
          .then()
          .statusCode(200);

      // check if good
      //boolean  isEnabled =
      JsonPath jp =
          given()
              .header(authHeader)
              .when().get(API_BASE_V1_0 + "/policies/" + testPolicy.id)
              .then()
              .statusCode(200)
              .extract()
              .body()
              .jsonPath();

      boolean isEnabled = jp.getBoolean("isEnabled");
      Assert.assertTrue(isEnabled);

      String t = jp.getString("mtime");
      Timestamp t2 = Timestamp.valueOf(t);
      Assert.assertTrue(t2.after(t1));

      // Now disable
      given()
          .header(authHeader)
          .contentType(ContentType.JSON)
          .queryParam("enabled",false)
          .when().post(API_BASE_V1_0 + "/policies/" + testPolicy.id + "/enabled")
          .then()
          .statusCode(200);

      // check if good
      testPolicy =
          given()
              .header(authHeader)
            .when()
              .get(API_BASE_V1_0 + "/policies/" + testPolicy.id)
            .then()
              .statusCode(200)
              .extract().body().as(TestPolicy.class);

      Assert.assertFalse(testPolicy.isEnabled);
      Timestamp t3 = Timestamp.valueOf(testPolicy.mtime);
      Assert.assertTrue(t3.after(t2));

    }
    finally {
      // now delete it again
      given()
          .header(authHeader)
          .when().delete(API_BASE_V1_0 + "/policies/" + testPolicy.id)
          .then()
          .statusCode(200);
    }
  }

  // Check that update is protected by RBAC.
  // we need to store as user with access first.
  @Test
  void storeAndUpdatePolicyNoUpdateAccess() {
    TestPolicy tp = new TestPolicy();
    tp.actions = "webhook";
    tp.conditions = "cores = 2";
    tp.name = "test2";

    Headers headers =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
        .queryParam("alsoStore","true")
      .when().post(API_BASE_V1_0 + "/policies")
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
    assert tp.conditions.equals(ret.conditions);

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
  void updateNoAuth() {
    TestPolicy tp = new TestPolicy();
    tp.name = UUID.randomUUID().toString();
    tp.conditions = "facts.arch";

    given()
        .header(authRbacNoAccess)
        .contentType(ContentType.JSON)
        .body(tp)
      .when()
        .put(API_BASE_V1_0 + "/policies/aaaaaaaa-bbbb-cccc-dddd-245b31933e94")
      .then()
        .statusCode(403);
  }

  @Test
  void updateEmptyPolicy() {

    given()
        .header(authRbacNoAccess)
        .contentType(ContentType.JSON)
      .when()
        .put(API_BASE_V1_0 + "/policies/aaaaaaaa-bbbb-cccc-dddd-245b31933e94")
      .then()
        .statusCode(400);
  }

  @Test
  void updateUnknownPolicy() {
    TestPolicy tp = new TestPolicy();
    tp.name = UUID.randomUUID().toString();
    tp.conditions = "facts.arch";

    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(tp)
      .when()
        .put(API_BASE_V1_0 + "/policies/aaaaaaaa-bbbb-cccc-dddd-245b31933e94")
      .then()
        .statusCode(404);
  }

  @Test
  void validateNewPolicy() {
    TestPolicy tp = new TestPolicy();
    tp.conditions = "cores = 2";
    tp.name = UUID.randomUUID().toString();

    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(tp)
            .when().post(API_BASE_V1_0 + "/policies/validate")
            .then()
            .statusCode(200)
            ;
  }

  @Test
  void validateNewEmptyPolicy() {

    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
      .when()
        .post(API_BASE_V1_0 + "/policies/validate")
      .then()
        .statusCode(400)
        ;
  }

  @Test
  void validateNewPolicyBadAuth() {
    TestPolicy tp = new TestPolicy();
    tp.conditions = "cores = 2";
    tp.name = UUID.randomUUID().toString();

    given()
        .header(authRbacNoAccess)
        .body(tp)
        .contentType(ContentType.JSON)
      .when()
        .post(API_BASE_V1_0 + "/policies/validate")
      .then()
        .statusCode(403)
        ;
  }

  @Test
  void validateExistingPolicy() {
    TestPolicy tp = new TestPolicy();
    tp.name = UUID.randomUUID().toString();
    tp.conditions = "cores = 2";

    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(tp)
            .when().post(API_BASE_V1_0 + "/policies/validate")
            .then()
            .statusCode(200)
    ;
  }

  @Test
  void validateNewPolicyNewName() {
    String name = "Not repeated";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(name)
            .when().post(API_BASE_V1_0 + "/policies/validate-name")
            .then()
            .statusCode(200);
  }

  @Test
  void validateNewPolicyExistingName() {
    String name = "1st policy";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(name)
            .when().post(API_BASE_V1_0 + "/policies/validate-name")
            .then()
            .statusCode(409);
  }

  @Test
  void validateNewPolicyLongName() {
    String name = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis pa";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(name)
            .when().post(API_BASE_V1_0 + "/policies/validate-name")
            .then()
            .statusCode(400);
    Assert.assertTrue(name.length() > 150);
  }

  @Test
  void validateExistingPolicyNewName() {
    String name = "Not repeated";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(name)
            .when().post(API_BASE_V1_0 + "/policies/validate-name?id=bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
            .then()
            .statusCode(200);
  }

  @Test
  void validateExistingPolicyExistingName() {
    String name = "3rd policy";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(name)
            .when().post(API_BASE_V1_0 + "/policies/validate-name?id=bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
            .then()
            .statusCode(409);
  }

  @Test
  void validateExistingPolicyUsingSameName() {
    String name = "1st policy";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(name)
            .when().post(API_BASE_V1_0 + "/policies/validate-name?id=bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
            .then()
            .statusCode(200);
  }

  @Test
  void validateExistingPolicyUsingLongName() {
    String name = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis pa";
    given()
            .header(authHeader)
            .contentType(ContentType.JSON)
            .body(name)
            .when().post(API_BASE_V1_0 + "/policies/validate-name?id=bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
            .then()
            .statusCode(400);
    Assert.assertTrue(name.length() > 150);
  }



  @Test
  void deletePolicy() {
    deletePolicyById("e3bdc9dd-18d4-4900-805d-7f59b3c736f7");
  }

  private void deletePolicyById(String id) {

    given()
        .header(authHeader)
      .when()
        .delete(API_BASE_V1_0 + "/policies/" + id)
      .then()
        .statusCode(200)
        ;

    // Now check that it is gone
    given()
        .header(authHeader)
      .when()
        .get(API_BASE_V1_0 + "/policies/" + id)
      .then()
        .statusCode(404);
  }

  @Test
  void deletePolicyNotInEngine() {

    given()
        .header(authHeader)
        .when().delete(API_BASE_V1_0 + "/policies/c49e92c4-764c-4163-9200-245b31933e94")
        .then()
        .statusCode(200)
    ;
  }  @Test
  void deletePolicyEngineProblem() {

    given()
        .header(authHeader)
      .when()
        .delete(API_BASE_V1_0 + "/policies/c49e92c4-dead-beef-9200-245b31933e94")
      .then()
        .statusCode(500)
    ;
    // Engine had a problem, so we did not delete the policy. Check that it is still there
    given()
        .header(authHeader)
      .when()
        .get(API_BASE_V1_0 + "/policies/c49e92c4-dead-beef-9200-245b31933e94")
      .then()
        .statusCode(200)
    ;

  }
  @Test
  void deleteUnknownPolicy() {

    given()
        .header(authHeader)
        .when().delete(API_BASE_V1_0 + "/policies/aaaaaaaa-bbbb-cccc-dddd-245b31933e94")
        .then()
        .statusCode(404)
    ;
  }

  @Test
  void deletePolicyNoRbacAccess() {

    given()
        .header(authRbacNoAccess)
        .when().delete(API_BASE_V1_0 + "/policies/e3bdc9dd-18d4-4900-805d-7f59b3c736f7")
        .then()
        .statusCode(403)
    ;
  }

  @Test
  void deletePolicies() {
    List<UUID> uuids = new ArrayList<>();
    uuids.add(UUID.randomUUID());
    uuids.add(UUID.fromString("cd6cceb8-65dd-4988-a566-251fd20d7e2c")); // known one
    uuids.add(UUID.randomUUID());
    uuids.add(UUID.fromString("c49e92c4-dead-beef-9200-245b31933e94")); // simulate engine problem

    JsonPath jsonPath =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(uuids)
      .when()
        .delete(API_BASE_V1_0 + "/policies/ids")
      .then()
        .statusCode(200)
      .extract().body().jsonPath();

    List<String> list = jsonPath.getList("");
    Assert.assertEquals(3, list.size());
    Assert.assertTrue(list.contains("cd6cceb8-65dd-4988-a566-251fd20d7e2c"));
    Assert.assertFalse(list.contains("c49e92c4-dead-beef-9200-245b31933e94"));
  }

  @Test
  void deletePoliciesNoAuth() {
    List<UUID> uuids = new ArrayList<>();
    uuids.add(UUID.randomUUID());

    given()
        .header(authRbacNoAccess)
        .contentType(ContentType.JSON)
        .body(uuids)
      .when()
        .delete(API_BASE_V1_0 + "/policies/ids")
      .then()
        .statusCode(403);
    }

  @Test
  void enableNonExistingPolicy() {
    String tpId = "00000000-dead-beef-9200-245b31933e94";
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .queryParam("enabled",true)
        .when().post(API_BASE_V1_0 + "/policies/" + tpId + "/enabled")
        .then()
        .statusCode(404);
  }

  @Test
  void enableNoPermission() {
    String tpId = "00000000-dead-beef-9200-245b31933e94";
    given()
        .header(authRbacNoAccess)
        .contentType(ContentType.JSON)
        .queryParam("enabled",true)
        .when().post(API_BASE_V1_0 + "/policies/" + tpId + "/enabled")
        .then()
        .statusCode(403);
  }

  @Test
  void enableDisablePolicies() {
    List<UUID> uuids = new ArrayList<>();
    uuids.add(UUID.randomUUID());
    uuids.add(UUID.fromString("9b3b4429-1393-4120-95da-54c17a512367")); // known one
    uuids.add(UUID.randomUUID());
    uuids.add(UUID.fromString("c49e92c4-dead-beef-9200-245b31933e94")); // simulate engine problem

    JsonPath jsonPath =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(uuids)
      .when()
        .queryParam("enabled",true)
        .post(API_BASE_V1_0 + "/policies/ids/enabled")
      .then()
        .statusCode(200)
      .extract().body().jsonPath();

    List<String> list = jsonPath.getList("");
    Assert.assertEquals(1, list.size());
    Assert.assertTrue(list.contains("9b3b4429-1393-4120-95da-54c17a512367"));
    Assert.assertFalse(list.contains("c49e92c4-dead-beef-9200-245b31933e94"));

    jsonPath =
    given()
        .header(authHeader)
        .contentType(ContentType.JSON)
        .body(uuids)
      .when()
        .queryParam("enabled",false)
        .post(API_BASE_V1_0 + "/policies/ids/enabled")
      .then()
        .statusCode(200)
      .extract().body().jsonPath();

    list = jsonPath.getList("");
    Assert.assertEquals(1, list.size());
    Assert.assertTrue(list.contains("9b3b4429-1393-4120-95da-54c17a512367"));
    Assert.assertFalse(list.contains("c49e92c4-dead-beef-9200-245b31933e94"));

  }

  @Test
  void enableDisablePoliciesNoAuth() {
    List<UUID> uuids = new ArrayList<>();
    uuids.add(UUID.randomUUID());

    given()
        .header(authRbacNoAccess)
        .contentType(ContentType.JSON)
        .body(uuids)
      .when()
        .queryParam("enabled", true)
        .post(API_BASE_V1_0 + "/policies/ids/enabled")
      .then()
        .statusCode(403);
  }

  @Test
  void testOpenApiEndpoint() {
    given()
        .header("Accept",ContentType.JSON)
        .when()
        .get(API_BASE_V1_0 + "/openapi.json")
        .then()
        .statusCode(200)
        .contentType("application/json");
  }

}
