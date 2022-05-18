package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;
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

    @Transactional
    @BeforeEach
    void beforeEach() {
        session.createQuery("DELETE FROM PoliciesHistoryEntry")
                .executeUpdate();
        session.createQuery("DELETE FROM Policy where customerid = :customerid1 OR customerid= :customerid2")
                .setParameter("customerid1", testAccountId)
                .setParameter("customerid2", otherTestAccountId)
                .executeUpdate();
    }

    @Test
    void testPolicyLastTriggeredIs0ByDefault() {
        Policy policy = createPolicy(testAccountId, UUID.randomUUID());
        assertEquals(0, policy.getLastTriggered());
    }

    @Test
    void testPolicyLastTriggeredShouldUpdateToGreatestHistory() {
        Policy policy = createPolicy(testAccountId, UUID.randomUUID());
        long lastTime = Instant.now().toEpochMilli();

        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)).toEpochMilli(), policy.id, policy.customerid);
        createPoliciesHistoryEntry(lastTime, policy.id, policy.customerid);
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)).toEpochMilli(), policy.id, policy.customerid);

        policy = getPolicy(policy.id);

        assertEquals(lastTime, policy.getLastTriggered());
    }

    @Test
    void testPolicyLastTriggeredAreSetById() {
        Policy policy = createPolicy(testAccountId, UUID.randomUUID());
        Policy policy2 = createPolicy(testAccountId, UUID.randomUUID());
        Policy policy3 = createPolicy(otherTestAccountId, UUID.randomUUID());

        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)).toEpochMilli(), policy.id, policy.customerid);
        createPoliciesHistoryEntry(Instant.now().toEpochMilli(), policy.id, policy.customerid);
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)).toEpochMilli(), policy.id, policy.customerid);

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
    private Policy createPolicy(String accountId, UUID policyId) {
        Transaction txn = session.beginTransaction();
        try {
            Policy policy = new Policy();
            policy.id = policyId;
            policy.customerid = accountId;
            policy.name = "Policy: " +  policy.id.toString();
            policy.actions = "";
            policy.conditions = "foo";
            policy.setMtimeToNow();

            session.persist(policy);
            return getPolicy(policy.id);
        } finally {
            txn.commit();
        }
    }

    @Transactional
    private void createPoliciesHistoryEntry(long instant, UUID policyId, String accountId) {
        Transaction txn = session.beginTransaction();
        try {
            PoliciesHistoryEntry historyEntry = new PoliciesHistoryEntry();
            historyEntry.setId(UUID.randomUUID());
            historyEntry.setTenantId(accountId);
            historyEntry.setPolicyId(policyId.toString());
            historyEntry.setCtime(instant);
            session.persist(historyEntry);
        } finally {
            txn.commit();
        }
    }

}
