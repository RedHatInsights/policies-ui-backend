package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
class PoliciesLastTriggeredTest {

    @Inject
    Session session;

    private final String testAccountId = "lt-test-account-id";
    private final String otherTestAccountId = "lt-test-2-account-id";
    private final String testOrgId = "lt-test-org-id";
    private final String otherTestOrgId = "lt-test-2-org-id";

    @Transactional
    @AfterEach
    void afterEach() {
        session.createQuery("DELETE FROM PoliciesHistoryEntry")
                .executeUpdate();
        session.createQuery("DELETE FROM Policy where orgId = :orgId1 OR orgId= :orgId2")
                .setParameter("orgId1", testOrgId)
                .setParameter("orgId2", otherTestOrgId)
                .executeUpdate();
    }

    @Test
    void testPolicyLastTriggeredIs0ByDefault() {
        Policy policy = createPolicy(testAccountId, testOrgId, UUID.randomUUID());
        assertEquals(0, policy.getLastTriggered());
    }

    @Test
    void testPolicyLastTriggeredShouldUpdateToGreatestHistory() {
        Policy policy = createPolicy(testAccountId, testOrgId, UUID.randomUUID());
        long lastTime = Instant.now().toEpochMilli();

        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)).toEpochMilli(), policy.id, policy.customerid, policy.orgId);
        createPoliciesHistoryEntry(lastTime, policy.id, policy.customerid, policy.orgId);
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)).toEpochMilli(), policy.id, policy.customerid, policy.orgId);

        policy = getPolicy(policy.id);

        assertEquals(lastTime, policy.getLastTriggered());
    }

    @Test
    void testPolicyLastTriggeredAreSetById() {
        Policy policy = createPolicy(testAccountId, testOrgId, UUID.randomUUID());
        Policy policy2 = createPolicy(testAccountId, testOrgId, UUID.randomUUID());
        Policy policy3 = createPolicy(otherTestAccountId, otherTestOrgId, UUID.randomUUID());

        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)).toEpochMilli(), policy.id, policy.customerid, policy.orgId);
        createPoliciesHistoryEntry(Instant.now().toEpochMilli(), policy.id, policy.customerid, policy.orgId);
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)).toEpochMilli(), policy.id, policy.customerid, policy.orgId);

        policy = getPolicy(policy.id);
        policy2 = getPolicy(policy2.id);
        policy3 = getPolicy(policy3.id);

        assertNotEquals(0, policy.getLastTriggered());
        assertEquals(0, policy2.getLastTriggered());
        assertEquals(0, policy3.getLastTriggered());
    }

    private Policy getPolicy(UUID policyId) {
        return session.find(Policy.class, policyId);
    }

    @Transactional
    public Policy createPolicy(String accountId, String orgId, UUID policyId) {
        Policy policy = new Policy();
        policy.id = policyId;
        policy.customerid = accountId;
        policy.orgId = orgId;
        policy.name = "Policy: " +  policy.id.toString();
        policy.actions = "";
        policy.conditions = "foo";
        policy.setMtimeToNow();

        session.persist(policy);
        return getPolicy(policy.id);
    }

    @Transactional
    public void createPoliciesHistoryEntry(long instant, UUID policyId, String accountId, String orgId) {
        PoliciesHistoryEntry historyEntry = new PoliciesHistoryEntry();
        historyEntry.setId(UUID.randomUUID());
        historyEntry.setTenantId(accountId);
        historyEntry.setOrgId(orgId);
        historyEntry.setPolicyId(policyId.toString());
        historyEntry.setCtime(instant);
        session.persist(historyEntry);
    }

}
