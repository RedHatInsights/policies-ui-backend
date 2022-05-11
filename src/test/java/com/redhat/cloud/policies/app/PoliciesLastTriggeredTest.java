package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
class PoliciesLastTriggeredTest {

    @Inject
    Session session;

    private final String accountId = "account-id";

    @BeforeEach
    @Transactional
    void beforeEach() {
        session.createQuery("DELETE FROM PoliciesHistoryEntry")
                .executeUpdate();
    }

    @Test
    @TestTransaction
    void testPolicyLastTriggeredIs0ByDefault() {
        Policy policy = createPolicy();
        assertEquals(0, policy.getLastTriggered());
    }

    @Test
    @TestTransaction
    void testPolicyLastTriggeredShouldUpdateToGreatestHistory() {
        Policy policy = createPolicy();
        long lastTime = Instant.now().toEpochMilli();

        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)).toEpochMilli(), policy.id);
        createPoliciesHistoryEntry(lastTime, policy.id);
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)).toEpochMilli(), policy.id);

        policy = getPolicy(policy.id);

        assertEquals(lastTime, policy.getLastTriggered());
    }

    @Test
    @TestTransaction
    void testPolicyLastTriggeredAreSetById() {
        Policy policy = createPolicy();
        Policy policy2 = createPolicy();

        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)).toEpochMilli(), policy.id);
        createPoliciesHistoryEntry(Instant.now().toEpochMilli(), policy.id);
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)).toEpochMilli(), policy.id);

        policy2 = getPolicy(policy2.id);

        assertEquals(0, policy2.getLastTriggered());
    }

    private Policy getPolicy(UUID policyId) {
        // The store procedure does not play well with hibernate.
        // This should not be a problem with real data as they are updated in separated transactions
        session.flush();
        session.clear();
        return session.find(Policy.class, policyId);
    }

    private Policy createPolicy() {
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

    private void createPoliciesHistoryEntry(long instant, UUID policyId) {
        PoliciesHistoryEntry historyEntry = new PoliciesHistoryEntry();
        historyEntry.setId(UUID.randomUUID());
        historyEntry.setTenantId(accountId);
        historyEntry.setPolicyId(policyId.toString());
        historyEntry.setCtime(instant);
        session.persist(historyEntry);
        session.flush();
    }

}
