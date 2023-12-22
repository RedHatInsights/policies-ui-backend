package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
class PoliciesHistoryCleanerTest {

    @Inject
    Session session;

    @BeforeEach
    @Transactional
    void beforeEach() {
        session.createQuery("DELETE FROM PoliciesHistoryEntry")
                .executeUpdate();
    }

    @Test
    @TestTransaction
    void testPostgresStoredProcedure() {
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)));
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)));
        assertCount(2L);
        session.createNativeQuery("CALL cleanPoliciesHistory()").executeUpdate();
        assertCount(1L);
    }

    private void createPoliciesHistoryEntry(Instant ctime) {
        PoliciesHistoryEntry historyEntry = new PoliciesHistoryEntry();
        historyEntry.setId(UUID.randomUUID());
        historyEntry.setTenantId("tenant-id");
        historyEntry.setOrgId("org-id");
        historyEntry.setPolicyId(UUID.randomUUID().toString());
        historyEntry.setCtime(ctime.toEpochMilli());
        session.persist(historyEntry);
    }

    private void assertCount(long expectedCount) {
        long actualCount = session.createQuery("SELECT COUNT(*) FROM PoliciesHistoryEntry", Long.class)
                .getSingleResult();
        assertEquals(expectedCount, actualCount);
    }
}
