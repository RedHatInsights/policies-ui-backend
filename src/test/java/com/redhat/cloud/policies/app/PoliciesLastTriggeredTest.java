package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeAll;
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

    @BeforeEach
    @Transactional
    void beforeEach() {
        session.createQuery("DELETE FROM PoliciesHistoryEntry")
                .executeUpdate();
    }

    @Test
    @TestTransaction
    void testPolicyLastTriggeredIs0ByDefault() {
        Policy policy = createPolicy(testAccountId, UUID.randomUUID());
        assertEquals(0, policy.getLastTriggered());
    }

    @Test
    @TestTransaction
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
    @TestTransaction
    void testPolicyLastTriggeredAreSetByIdAndAccount() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        Policy policy = createPolicy(testAccountId, uuid1);
        Policy policy2 = createPolicy(testAccountId, uuid2);
        Policy policy3 = createPolicy(otherTestAccountId, uuid1);

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
        // The store procedure does not play well with hibernate.
        // This should not be a problem with real data as they are updated in separated transactions
        session.flush();
        session.clear();
        return session.find(Policy.class, policyId);
    }

    private Policy createPolicy(String accountId, UUID policyId) {
        Policy policy = new Policy();
        policy.id = UUID.randomUUID();
        policy.customerid = accountId;
        policy.name = "Policy: " +  policy.id.toString();
        policy.actions = "";
        policy.conditions = "foo";
        policy.setMtimeToNow();

        session.persist(policy);
        return getPolicy(policy.id);
    }

    private void createPoliciesHistoryEntry(long instant, UUID policyId, String accountId) {
        PoliciesHistoryEntry historyEntry = new PoliciesHistoryEntry();
        historyEntry.setId(UUID.randomUUID());
        historyEntry.setTenantId(accountId);
        historyEntry.setPolicyId(policyId.toString());
        historyEntry.setCtime(instant);
        session.persist(historyEntry);
        session.flush();
    }

}
