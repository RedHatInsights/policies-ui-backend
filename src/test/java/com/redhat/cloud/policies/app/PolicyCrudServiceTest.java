package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.engine.HistoryItem;
import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
class PolicyCrudServiceTest extends AbstractITest {

    private static final String TENANT_ID = "1234";
    private static final String ORG_ID = "org-id-1234";
    private static final String POLICY_NAME = "my-policy";

    @Inject
    PoliciesHistoryTestHelper helper;

    @Inject
    Session session;

    @Transactional
    @AfterEach
    void afterEach() {
        session.createQuery("DELETE FROM Policy where orgId = :orgId AND name = :name")
                .setParameter("orgId", ORG_ID)
                .setParameter("name", POLICY_NAME)
                .executeUpdate();
    }


    @Test
    void test() {
        UUID policyId = createPolicy();

        PoliciesHistoryEntry historyEntry1 = helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-1", "foo", 1L);
        helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-2", "fooBAR", 2L);
        helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-3", "FoOoOo", 3L);
        PoliciesHistoryEntry historyEntry2 = helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-4", " foo", 4L);
        helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-5", "barFOO", 5L);
        helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-6", "bar", 6L);

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

    @Transactional
    UUID createPolicy() {

        Policy policy = new Policy();
        policy.id = UUID.randomUUID();
        policy.customerid = TENANT_ID;
        policy.orgId = ORG_ID;
        policy.name = POLICY_NAME;
        policy.conditions = "arch = \"x86_64\"";
        policy.persist();

        return policy.id;
    }
}
