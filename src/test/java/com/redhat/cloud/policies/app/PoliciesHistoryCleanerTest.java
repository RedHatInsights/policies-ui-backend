package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.redhat.cloud.policies.app.PoliciesHistoryCleaner.POLICIES_HISTORY_CLEANER_DELETE_AFTER_CONF_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
public class PoliciesHistoryCleanerTest {

    @Inject
    Session session;

    @Inject
    PoliciesHistoryCleaner policiesHistoryCleaner;

    @BeforeEach
    @Transactional
    public void beforeEach() {
        session.createQuery("DELETE FROM PoliciesHistoryEntry").executeUpdate();
    }

    @Test
    @TestTransaction
    public void testWithDefaultConfiguration() {
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)));
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)));
        assertCount(2L);
        policiesHistoryCleaner.clean();
        assertCount(1L);
    }

    @Test
    @TestTransaction
    public void testWithCustomConfiguration() {
        System.setProperty(POLICIES_HISTORY_CLEANER_DELETE_AFTER_CONF_KEY, "30m");
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofHours(1L)));
        createPoliciesHistoryEntry(Instant.now().minus(Duration.ofDays(28L)));
        assertCount(2L);
        policiesHistoryCleaner.clean();
        assertCount(0L);
        System.clearProperty(POLICIES_HISTORY_CLEANER_DELETE_AFTER_CONF_KEY);
    }

    private void createPoliciesHistoryEntry(Instant ctime) {
        PoliciesHistoryEntry historyEntry = new PoliciesHistoryEntry();
        historyEntry.setId(UUID.randomUUID());
        historyEntry.setTenantId("tenant-id");
        historyEntry.setPolicyId("policy-id");
        historyEntry.setCtime(ctime.toEpochMilli());
        session.persist(historyEntry);
    }

    private void assertCount(long expectedCount) {
        long actualCount = session.createQuery("SELECT COUNT(*) FROM PoliciesHistoryEntry", Long.class)
                .getSingleResult();
        assertEquals(expectedCount, actualCount);
    }
}
