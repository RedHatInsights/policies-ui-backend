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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import com.redhat.cloud.policies.app.model.history.PoliciesHistoryRepository;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.validation.constraints.NotNull;

import io.restassured.response.Response;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@Tag("integration")
class RestApiTest extends AbstractITest {

    @Inject
    TestUUIDHelperBean uuidHelper;

    @InjectMock
    PoliciesHistoryRepository policiesHistoryRepository;

    @Inject
    Session session;

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
                .header("x-rh-identity", "frobnitz")
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

        assertEquals(numberOfPolicies, jsonPath.getList("data").size());
        Map<String, Object> data = (Map<String, Object>) jsonPath.getList("data").get(0);
        assertTrue(data.containsKey("lastTriggered"));
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
    void testGetPoliciesRhIdNoAccount() {

        given()
                .header(authHeaderNoAccount)
                .when()
                .get(API_BASE_V1_0 + "/policies/")
                .then()
                .statusCode(401);
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

        assertEquals(numberOfPolicies, jsonPath.getList("").size());
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
    void testGetPoliciesSort2() {
        given()
                .header(authHeader)
                .when()
                // Default sort is on ctime desc, if no column is given
                .get(API_BASE_V1_0 + "/policies/?sortOrder=asc")
                .then()
                .statusCode(200)
                .assertThat()
                .body(" data.get(0).description", is("Just a test"));
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
        assertEquals(10, jsonPath.getList("data").size());
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        assertEquals(3, links.size());
        extractAndCheck(links, "first", 10, 0);
        extractAndCheck(links, "last", 10, 10);
        extractAndCheck(links, "next", 10, 10);
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
        assertEquals(5, jsonPath.getList("data").size());
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        assertEquals(links.size(), 3);
        extractAndCheck(links, "first", 5, 0);
        extractAndCheck(links, "last", 5, 10);
        extractAndCheck(links, "next", 5, 5);
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
        assertEquals(5, jsonPath.getList("data").size());
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        assertEquals(links.size(), 4);
        extractAndCheck(links, "first", 5, 0);
        extractAndCheck(links, "prev", 5, 0);
        extractAndCheck(links, "next", 5, 10);
        extractAndCheck(links, "last", 5, 10);
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
        assertEquals(policiesInDb, jsonPath.getList("").size());
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
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        assertEquals(links.size(), 4);
        extractAndCheck(links, "first", 5, 0);
        extractAndCheck(links, "prev", 5, 0);
        extractAndCheck(links, "next", 5, 7);
        extractAndCheck(links, "last", 5, 10);
    }

    @Test
    void testGetPoliciesPaged5() {

        JsonPath jsonPath =
                given()
                        .header(authHeader)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/?limit=1&offset=0")
                        .then()
                        .statusCode(200)
                        .extract().body().jsonPath();

        long policiesInDb = countPoliciesInDB();
        assertEquals(1, jsonPath.getList("data").size());
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        assertEquals(3, links.size());
        extractAndCheck(links, "first", 1, 0);
        extractAndCheck(links, "next", 1, 1);
        extractAndCheck(links, "last", 1, 11);
    }

    @Test
    void testGetPoliciesPaged6() {

        JsonPath jsonPath =
                given()
                        .header(authHeader)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/?limit=6&offset=0")
                        .then()
                        .statusCode(200)
                        .extract().body().jsonPath();

        int lastLimit = 6;
        int lastOffset = 6;

        long policiesInDb = countPoliciesInDB();
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        assertEquals(links.size(), 3);
        extractAndCheck(links, "first", 6, 0);
        extractAndCheck(links, "next", 6, 6);
        extractAndCheck(links, "last", lastLimit, lastOffset);

        JsonPath jsonPathLastPage =
                given()
                        .header(authHeader)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/?" + String.format("limit=%d&offset=%d", lastLimit, lastOffset))
                        .then()
                        .statusCode(200)
                        .extract().body().jsonPath();

        assertTrue(jsonPathLastPage.getList("data").size() > 0);
    }

    @Test
    void testGetPoliciesPaged7() {

        JsonPath jsonPath =
                given()
                        .header(authHeader)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/?limit=1&offset=0")
                        .then()
                        .statusCode(200)
                        .extract().body().jsonPath();

        int lastLimit = 1;
        int lastOffset = 11;

        long policiesInDb = countPoliciesInDB();
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        extractAndCheck(links, "last", lastLimit, lastOffset);

        JsonPath jsonPathLastPage =
                given()
                        .header(authHeader)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/?" + String.format("limit=%d&offset=%d", lastLimit, lastOffset))
                        .then()
                        .statusCode(200)
                        .extract().body().jsonPath();

        assertTrue(jsonPathLastPage.getList("data").size() > 0);
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

        assertEquals(policiesInDb, data.size());
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        assertEquals(links.size(), 2);
        extractAndCheck(links, "first", -1, 0);
        extractAndCheck(links, "last", -1, 0);
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

        assertEquals(policiesInDb, data.size());
        assertEquals(policiesInDb, jsonPath.getInt("meta.count"));
        Map<String, String> links = jsonPath.get("links");
        assertEquals(links.size(), 2);
        extractAndCheck(links, "first", -1, 0);
        extractAndCheck(links, "last", -1, 0);
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

        assertTrue(size >= 8);
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

        assertTrue(size <= 3);
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
                .when().get(API_BASE_V1_0 + "/policies/?filter[actions]=notification&filter:op[name]=ilike")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetPoliciesInvalidFilter2() {
        given()
                .header(authHeader)
                .when()
                .get(API_BASE_V1_0 + "/policies/?filter[name]=notification&filter:op[name]=boolean_is")
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
        PoliciesHistoryEntry entry = new PoliciesHistoryEntry();
        entry.setId(UUID.randomUUID());
        entry.setTenantId(accountId);
        entry.setPolicyId("9b3b4429-1393-4120-95da-54c17a512367");
        entry.setCtime(new GregorianCalendar(2020, 4, 10, 10, 0, 0).getTimeInMillis());
        Transaction transaction = session.beginTransaction();
        session.persist(entry);
        session.flush();
        transaction.commit();

        JsonPath jsonPath =
                given()
                        .header(authHeader)
                        .when().get(API_BASE_V1_0 + "/policies/9b3b4429-1393-4120-95da-54c17a512367")
                        .then()
                        .statusCode(200)
                        .body(containsString("5th policy"))
                        .extract().jsonPath();

        TestPolicy policy = jsonPath.getObject("", TestPolicy.class);
        assertEquals("notification", policy.actions, "Action does not match");
        assertEquals("\"cores\" > 4", policy.conditions, "Conditions do not match");
        assertTrue(policy.isEnabled, "Policy is not enabled");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(policy.lastTriggered);
        assertEquals(2020, cal.get(Calendar.YEAR));
        assertEquals(4, cal.get(Calendar.MONTH));
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, cal.get(Calendar.HOUR));
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
    void getOnePolicyHistory() {
        String uuid = setupPolicyForHistory();
        mockPoliciesHistory("dce4760b-d796-48f0-a7b9-7a07a6a45d1d", "VM 22", 2);

        ExtractableResponse<Response> er =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/8671900e-9d31-47bf-9249-8f45698ede72/history/trigger")
                        .then()
                        .statusCode(200)
                        .extract();

        JsonPath jsonPath = er.body().jsonPath();
        int totalCount = jsonPath.getInt("meta.count");
        assertEquals(2, totalCount);
        List<Map<String, Object>> returnedBody = jsonPath.getList("data");
        try {
            assertEquals(2, returnedBody.size());
            Map<String, Object> map = returnedBody.get(0);
            assertEquals("VM 22", map.get("hostName"));
            assertEquals("dce4760b-d796-48f0-a7b9-7a07a6a45d1d", map.get("id"));
        } finally {
            deletePolicyById(uuid);
        }
    }

    @Test
    void getOnePolicyHistoryFilterByName() {
        String uuid = setupPolicyForHistory();
        mockPoliciesHistory("dce4760b-0000-48f0-0000-7a07a6a45d1d", "VM", 1);

        ExtractableResponse<Response> er =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/8671900e-9d31-47bf-9249-8f45698ede72/history/trigger" +
                                "?filter[name]=VM22")
                        .then()
                        .statusCode(200)
                        .extract();

        JsonPath jsonPath = er.body().jsonPath();
        int totalCount = jsonPath.getInt("meta.count");
        assertEquals(1, totalCount);
        List<Map<String, Object>> returnedBody = jsonPath.getList("data");
        try {
            assertEquals(1, returnedBody.size());
            Map<String, Object> map = returnedBody.get(0);
            assertEquals("VM", map.get("hostName"));
            assertEquals("dce4760b-0000-48f0-0000-7a07a6a45d1d", map.get("id"));
        } finally {
            deletePolicyById(uuid);
        }
    }

    @Test
    void getOnePolicyHistoryFilterByNameNotEqual() {
        String uuid = setupPolicyForHistory();
        mockPoliciesHistory("dce4760b-0000-48f0-aaaa-7a07a6a45d1d", "VM22", 1);

        ExtractableResponse<Response> er =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/8671900e-9d31-47bf-9249-8f45698ede72/history/trigger" +
                                "?filter[name]=VM&filter:op[name]=not_equal")
                        .then()
                        .statusCode(200)
                        .extract();

        JsonPath jsonPath = er.body().jsonPath();
        int totalCount = jsonPath.getInt("meta.count");
        assertEquals(1, totalCount);
        List<Map<String, Object>> returnedBody = jsonPath.getList("data");
        try {
            assertEquals(1, returnedBody.size());
            Map<String, Object> map = returnedBody.get(0);
            assertEquals("VM22", map.get("hostName"));
            assertEquals("dce4760b-0000-48f0-aaaa-7a07a6a45d1d", map.get("id"));
        } finally {
            deletePolicyById(uuid);
        }
    }

    @Test
    void getOnePolicyHistoryFilterById() {
        String uuid = setupPolicyForHistory();
        mockPoliciesHistory("dce4760b-0000-48f0-0000-7a07a6a45d1d", "VM", 1);

        ExtractableResponse<Response> er =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .when()
                        .get(API_BASE_V1_0 + "/policies/8671900e-9d31-47bf-9249-8f45698ede72/history/trigger" +
                                "?filter[id]=dce4760b-0000-48f0-0000-7a07a6a45d1d")
                        .then()
                        .statusCode(200)
                        .extract();

        JsonPath jsonPath = er.body().jsonPath();
        int totalCount = jsonPath.getInt("meta.count");
        assertEquals(1, totalCount);
        List<Map<String, Object>> returnedBody = jsonPath.getList("data");
        try {
            assertEquals(1, returnedBody.size());
            Map<String, Object> map = returnedBody.get(0);
            assertEquals("VM", map.get("hostName"));
            assertEquals("dce4760b-0000-48f0-0000-7a07a6a45d1d", map.get("id"));
        } finally {
            deletePolicyById(uuid);
        }
    }

    @Test
    void getOnePolicyHistoryFilterByIdLike() {
        String uuid = setupPolicyForHistory();
        mockPoliciesHistory("dce4760b-0000-48f0-0000-7a07a6a45d1d", "VM", 1);

        try {
            given()
                    .header(authHeader)
                    .contentType(ContentType.JSON)
                    .when()
                    .get(API_BASE_V1_0 + "/policies/8671900e-9d31-47bf-9249-8f45698ede72/history/trigger" +
                            "?filter[id]=dce4760b-0000-48f0-0000-7a07a6a45d1d&filter:op[id]=like")
                    .then()
                    .statusCode(200);
        } finally {
            deletePolicyById(uuid);
        }
    }


    @NotNull
    private String setupPolicyForHistory() {
        TestPolicy tp = new TestPolicy();
        tp.actions = "notification";
        tp.conditions = "cores = 2";
        tp.name = "test1";
        // Use an explicit ID; that the mock server knows
        String uuid = "8671900e-9d31-47bf-9249-8f45698ede72";
        uuidHelper.storeUUIDString(uuid);

        given()
                .header(authHeader)
                .contentType(ContentType.JSON)
                .body(tp)
                .queryParam("alsoStore", "true")
                .when()
                .post(API_BASE_V1_0 + "/policies")
                .then()
                .statusCode(201);
        return uuid;
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
        tp.actions = "notification";
        tp.conditions = "cores = 2";
        tp.name = "test1";

        ExtractableResponse<Response> er =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .body(tp)
                        .queryParam("alsoStore", "true")
                        .when().post(API_BASE_V1_0 + "/policies")
                        .then()
                        .statusCode(201)
                        .extract();

        Headers headers = er.headers();

        assertTrue(headers.hasHeaderWithName("Location"));
        // Extract location and then check in subsequent call
        // that the policy is stored
        Header locationHeader = headers.get("Location");
        String location = locationHeader.getValue();
        // location is  a full url to the new resource.
        System.out.println(location);

        try {
            TestPolicy returnedBody = er.body().as(TestPolicy.class);
            assertNotNull(returnedBody);
            assertEquals("cores = 2", returnedBody.conditions);
            assertEquals("test1", returnedBody.name);

            JsonPath body =
                    given()
                            .header(authHeader)
                            .when()
                            .get(location)
                            .then()
                            .statusCode(200)
                            .extract().body()
                            .jsonPath();

            assertEquals("cores = 2", body.get("conditions"));
            assertEquals("test1", body.get("name"));
            assertEquals(body.get("id").toString(), returnedBody.id.toString());
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
                .queryParam("alsoStore", "false")
                .when().post(API_BASE_V1_0 + "/policies")
                .then()
                .statusCode(400);
        assertTrue(tp.name.length() > 150);
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
                .queryParam("dry", "true")
                .when().put(API_BASE_V1_0 + "/policies/bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
                .then()
                .statusCode(400);
        assertTrue(tp.name.length() > 150);
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
        tp.actions = "notification";
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
        tp.actions = "notification";
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
                        .queryParam("alsoStore", "true")
                        .when().post(API_BASE_V1_0 + "/policies")
                        .then()
                        .statusCode(201)
                        .extract().headers();

        assertTrue(headers.hasHeaderWithName("Location"));
        // Extract location and then check in subsequent call
        // that the policy is stored
        Header locationHeader = headers.get("Location");
        String location = locationHeader.getValue();
        // location is  a full url to the new resource.
        System.out.println(location);
        assertTrue(location.endsWith(testUUID.toString()));

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
        TestPolicy ret = jsonb.fromJson(resp, TestPolicy.class);
        assertEquals(tp.conditions, ret.conditions);

        assertNotNull(ret.ctime);
        assertNotNull(ret.mtime);
        String storeTime = ret.ctime; // keep for below
        Timestamp ctime = Timestamp.valueOf(ret.ctime);
        Timestamp mtime1 = Timestamp.valueOf(ret.mtime);
        // ctime and mtime oftern differ a tiny bit, which makes the nanos differ. Let's compare with some slack
        assertTrue(Math.abs(mtime1.getTime() - ctime.getTime()) < 2, "Ctime: " + ctime + ", mtime: " + mtime1);

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
            assertTrue(content.equalsIgnoreCase("cores = 3"));

            assertEquals(storeTime, jsonPath.getString("ctime"));
            Assertions.assertNotEquals(storeTime, jsonPath.getString("mtime"));
            Timestamp mtime2 = Timestamp.valueOf(jsonPath.getString("mtime"));
            assertTrue(ctime.before(mtime2));
            assertTrue(mtime1.before(mtime2));

        } finally {
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
        tp.actions = "notification";
        tp.conditions = "cores = 2";
        tp.name = "test2";
        tp.isEnabled = false;

        TestPolicy testPolicy =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .body(tp)
                        .queryParam("alsoStore", "true")
                        .when().post(API_BASE_V1_0 + "/policies")
                        .then()
                        .statusCode(201)
                        .extract().body().as(TestPolicy.class);

        String mt = testPolicy.mtime;
        Timestamp t1 = Timestamp.valueOf(mt);

        try {
            // Now enable
            given()
                    .header(authHeader)
                    .contentType(ContentType.JSON)
                    .queryParam("enabled", true)
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
            assertTrue(isEnabled);

            String t = jp.getString("mtime");
            Timestamp t2 = Timestamp.valueOf(t);
            assertTrue(t2.after(t1));

            // Now disable
            given()
                    .header(authHeader)
                    .contentType(ContentType.JSON)
                    .queryParam("enabled", false)
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

            Assertions.assertFalse(testPolicy.isEnabled);
            Timestamp t3 = Timestamp.valueOf(testPolicy.mtime);
            assertTrue(t3.after(t2));

        } finally {
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
        tp.actions = "notification";
        tp.conditions = "cores = 2";
        tp.name = "test2";

        Headers headers =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .body(tp)
                        .queryParam("alsoStore", "true")
                        .when().post(API_BASE_V1_0 + "/policies")
                        .then()
                        .statusCode(201)
                        .extract().headers();

        assertTrue(headers.hasHeaderWithName("Location"));
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
        TestPolicy ret = jsonb.fromJson(resp, TestPolicy.class);
        assertEquals(tp.conditions, ret.conditions);

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
                .body(Json.createValue(name).toString())
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
                .body(Json.createValue(name).toString())
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
                .body(Json.createValue(name).toString())
                .when().post(API_BASE_V1_0 + "/policies/validate-name")
                .then()
                .statusCode(400);
    }

    @Test
    void validateExistingPolicyNewName() {
        String name = "Not repeated";
        given()
                .header(authHeader)
                .contentType(ContentType.JSON)
                .body(Json.createValue(name).toString())
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
                .body(Json.createValue(name).toString())
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
                .body(Json.createValue(name).toString())
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
        assertEquals(3, list.size());
        assertTrue(list.contains("cd6cceb8-65dd-4988-a566-251fd20d7e2c"));
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
                .queryParam("enabled", true)
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
                .queryParam("enabled", true)
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

        JsonPath jsonPath =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .body(uuids)
                        .when()
                        .queryParam("enabled", true)
                        .post(API_BASE_V1_0 + "/policies/ids/enabled")
                        .then()
                        .statusCode(200)
                        .extract().body().jsonPath();

        List<String> list = jsonPath.getList("");
        assertEquals(1, list.size());
        assertTrue(list.contains("9b3b4429-1393-4120-95da-54c17a512367"));

        jsonPath =
                given()
                        .header(authHeader)
                        .contentType(ContentType.JSON)
                        .body(uuids)
                        .when()
                        .queryParam("enabled", false)
                        .post(API_BASE_V1_0 + "/policies/ids/enabled")
                        .then()
                        .statusCode(200)
                        .extract().body().jsonPath();

        list = jsonPath.getList("");
        assertEquals(1, list.size());
        assertTrue(list.contains("9b3b4429-1393-4120-95da-54c17a512367"));

    }

    @Test
    void enableDisableNullBody() {

        given()
                .header(authHeader)
                .contentType(ContentType.JSON)
                .body("")
                .when()
                .queryParam("enabled", true)
                .post(API_BASE_V1_0 + "/policies/ids/enabled")
                .then()
                .statusCode(400);
    }

    @Test
    void enableDisableBadBody() {

        List<String> body = new ArrayList<>();
        body.add("Hello");
        body.add("World");

        given()
                .header(authHeader)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .queryParam("enabled", true)
                .post(API_BASE_V1_0 + "/policies/ids/enabled")
                .then()
                .statusCode(400);
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
                .header("Accept", ContentType.JSON)
                .when()
                .get(API_BASE_V1_0 + "/openapi.json")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    void mapBadAcceptHeader() {

        String body =
                given()
                        .header(authHeader)
                        .header("Accept", "blabla")
                        .when()
                        .get(API_BASE_V1_0 + "/policies/c49e92c4-dead-beef-9200-245b31933e94")
                        .then()
                        .statusCode(400)
                        .extract().body().asPrettyString();

        assertNotNull(body);
        assertTrue(body.contains("Something went wrong,"));
    }

    @Test
    void mapBadContentHeader() {

        String body =
                given()
                        .header(authHeader)
                        .header("Content-Type", "bla bla")
                        .when()
                        .get(API_BASE_V1_0 + "/policies/c49e92c4-dead-beef-9200-245b31933e94")
                        .then()
                        .statusCode(400)
                        .extract().body().asPrettyString();

        assertNotNull(body);
        assertTrue(body.contains("Something went wrong,"));
        assertFalse(body.contains("RESTEASY00"));
    }

    @Test
    void catchInvalidPath() {

        String body =
                given()
                        .header(authHeader)
                        .when()
                        .get(API_BASE_V1_0 + "/hula")
                        .then()
                        .statusCode(404)
                        .extract().body().asPrettyString();

        assertNotNull(body);
        assertTrue(body.contains("Something went wrong,"));
        assertFalse(body.contains("RESTEASY00"));
    }

    @Test
    void catchOtherInvalidPath() {

        String body =
                given()
                        .header(authHeader)
                        .when()
                        .get(API_BASE_V1_0)
                        .then()
                        .statusCode(404)
                        .extract().body().asPrettyString();

        assertNotNull(body);
        assertTrue(body.contains("Something went wrong,"));
        assertFalse(body.contains("RESTEASY00"));
    }

    @Test
    void deletePolicyBadParam() {

        String body =
                given()
                        .header(authHeader)
                        .when()
                        .delete(API_BASE_V1_0 + "/policies/id?query=test")
                        .then()
                        .statusCode(404)
                        .extract().body().asPrettyString();

        assertNotNull(body);
        assertTrue(body.contains("Something went wrong,"));
        assertFalse(body.contains("RESTEASY00"));
    }

    private void mockPoliciesHistory(String hostId, String hostName, int count) {
        String tenantId = "1234";
        UUID policyId = UUID.fromString("8671900e-9d31-47bf-9249-8f45698ede72");
        List<PoliciesHistoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PoliciesHistoryEntry entry = new PoliciesHistoryEntry();
            entry.setTenantId(tenantId);
            entry.setPolicyId(policyId.toString());
            entry.setHostId(hostId);
            entry.setHostName(hostName);
            entries.add(entry);
        }
        when(policiesHistoryRepository.count(eq(tenantId), eq(policyId), any())).thenReturn((long) entries.size());
        when(policiesHistoryRepository.find(eq(tenantId), eq(policyId), any())).thenReturn(entries);
    }
}
