package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.engine.HistoryItem;
import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
class PolicyCrudServiceTest extends AbstractITest {

    private static final String TENANT_ID = "1234";

    @Inject
    PoliciesHistoryTestHelper helper;

    @Inject
    LightweightEngineConfig lightweightEngineConfig;

    @Test
    void test() {
        UUID policyId = createPolicy();

        PoliciesHistoryEntry historyEntry1 = helper.createPoliciesHistoryEntry(TENANT_ID, policyId, "host-id-1", "foo", 1L);
        helper.createPoliciesHistoryEntry(TENANT_ID, policyId, "host-id-2", "fooBAR", 2L);
        helper.createPoliciesHistoryEntry(TENANT_ID, policyId, "host-id-3", "FoOoOo", 3L);
        PoliciesHistoryEntry historyEntry2 = helper.createPoliciesHistoryEntry(TENANT_ID, policyId, "host-id-4", " foo", 4L);
        helper.createPoliciesHistoryEntry(TENANT_ID, policyId, "host-id-5", "barFOO", 5L);
        helper.createPoliciesHistoryEntry(TENANT_ID, policyId, "host-id-6", "bar", 6L);

        String responseBody = given()
                .basePath(API_BASE_V1_0)
                .header(authHeader)
                .pathParam("id", policyId)
                .queryParam("filter[name]", "foo")
                .queryParam("filter:op[name]", "LIKE")
                .queryParam("sortColumn", "name")
                .queryParam("sortDirection", "desc")
                .queryParam("limit", 2)
                .queryParam("offset", 2)
                .when().get("/policies/{id}/history/trigger")
                .then().statusCode(200)
                .extract().asString();

        JsonObject history = new JsonObject(responseBody);
        JsonArray data = history.getJsonArray("data");
        assertEquals(2, data.size());
        data.getJsonObject(0).mapTo(HistoryItem.class);
        assertEquals(historyEntry1.getHostName(), data.getJsonObject(0).getString("hostName"));
        assertEquals(historyEntry2.getHostName(), data.getJsonObject(1).getString("hostName"));
        assertEquals(5, history.getJsonObject("meta").getInteger("count"));
    }

    private UUID createPolicy() {

        Policy policy = new Policy();
        policy.name = "my-policy";
        policy.conditions = "arch = \"x86_64\"";

        String responseBody = given()
                .basePath(API_BASE_V1_0)
                .header(authHeader)
                .contentType(JSON)
                .body(Json.encode(policy))
                .queryParam("alsoStore", true)
                .when().post("/policies")
                .then().statusCode(201)
                .extract().asString();

        JsonObject jsonPolicy = new JsonObject(responseBody);
        return UUID.fromString(jsonPolicy.getString("id"));
    }

    @Test
    void testUnavailableSync() {
        lightweightEngineConfig.overrideForTest(true);
        given()
                .header(authHeader)
                .contentType(JSON)
                .when().post("/admin/sync")
                .then().statusCode(503);
        lightweightEngineConfig.overrideForTest(false);
    }

    @Test
    void testUnavailableVerify() {
        lightweightEngineConfig.overrideForTest(true);
        given()
                .header(authHeader)
                .when().get("/admin/verify")
                .then().statusCode(503);
        lightweightEngineConfig.overrideForTest(false);
    }
}
