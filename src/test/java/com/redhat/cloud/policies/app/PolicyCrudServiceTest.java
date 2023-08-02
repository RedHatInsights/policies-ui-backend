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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
class PolicyCrudServiceTest extends AbstractITest {

    private static final String TENANT_ID = "1234";
    private static final String ORG_ID = "org-id-1234";
    private static final String POLICY_NAME_PREFIX = "my-policy";
    private UUID policyId;

    @Inject
    PoliciesHistoryTestHelper helper;

    @Inject
    Session session;

    @BeforeEach
    void beforeEach() throws Exception {
        policyId = createPolicy();
    }

    @Transactional
    @AfterEach
    void afterEach() throws Exception {
        session.createQuery("DELETE FROM Policy where id = :policyId")
                .setParameter("policyId", policyId)
                .executeUpdate();

        session.createQuery("DELETE FROM PoliciesHistoryEntry where policyId = :policyId")
                .setParameter("policyId", policyId.toString())
                .executeUpdate();
    }


    @Test
    void testGetPolicyHistory() {
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

    @Test
    void testGetPolicyHistoryWithGroupRestrictions() {
        List<UUID> userHostGroups = new ArrayList<UUID>(); // from rbac_example_groups.json
        UUID group1 = UUID.fromString("78e3dc30-cec3-4b49-be2d-37482c74a9ac");
        UUID group2 = UUID.fromString("79e3dc30-cec3-4b49-be2d-37482c74a9ad");
        UUID randomGroup = UUID.randomUUID();
        userHostGroups.add(group1);
        userHostGroups.add(group2);
        userHostGroups.add(null); // ungrouped

        var entryGroup1 = helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID,  policyId, "host-id-1", "foo", List.of(group1), 1);
        var entryTwoGroups = helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-2", "fooBAR", List.of(group1, group2), 2L);
        helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-3", "FoOoOo", List.of(randomGroup), 3L);
        var entryUngrouped = helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-4", " foo", 4L);
        helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-5", "barFOO", List.of(randomGroup), 5L);
        helper.createPoliciesHistoryEntry(TENANT_ID, ORG_ID, policyId, "host-id-6", "bar", List.of(randomGroup), 6L);

        String responseBody = given()
                .basePath(API_BASE_V1_0)
                .header(authHeaderHostGroups)
                .pathParam("id", policyId)
                .queryParam("limit", 10)
                .when().get("/policies/{id}/history/trigger")
                .then().statusCode(200)
                .extract().asString();

        JsonObject history = new JsonObject(responseBody);
        JsonArray data = history.getJsonArray("data");
        assertEquals(3, data.size());
        data.getJsonObject(0).mapTo(HistoryItem.class);
        assertEquals(entryGroup1.getHostId(), data.getJsonObject(0).getString("id"));
        assertEquals(entryTwoGroups.getHostId(), data.getJsonObject(1).getString("id"));
        assertEquals(entryUngrouped.getHostId(), data.getJsonObject(2).getString("id"));
        assertEquals(3, history.getJsonObject("meta").getInteger("count"));
    }

    @Transactional
    UUID createPolicy() {

        Policy policy = new Policy();
        policy.id = UUID.randomUUID();
        policy.customerid = TENANT_ID;
        policy.orgId = ORG_ID;
        policy.name = POLICY_NAME_PREFIX + policy.id.toString();
        policy.conditions = "arch = \"x86_64\"";
        policy.persist();

        return policy.id;
    }
}
