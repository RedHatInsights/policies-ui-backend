package com.redhat.cloud.policies.app.model.history;

import com.redhat.cloud.policies.app.PoliciesHistoryTestHelper;
import com.redhat.cloud.policies.app.TestLifecycleManager;
import com.redhat.cloud.policies.app.model.pager.Pager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.EQUAL;
import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.LIKE;
import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.NOT_EQUAL;
import static io.quarkus.panache.common.Sort.Direction.Ascending;
import static io.quarkus.panache.common.Sort.Direction.Descending;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
class PoliciesHistoryRepositoryTest {

    private static final String TENANT_ID_1 = "tenant-id-1";
    private static final String TENANT_ID_2 = "tenant-id-2";
    private static final UUID POLICY_ID_1 = UUID.randomUUID();
    private static final UUID POLICY_ID_2 = UUID.randomUUID();
    private static final String HOST_ID_1 = "host-id-1";
    private static final String HOST_ID_2 = "host-id-2";
    private static final String HOST_NAME_1 = "host-name-1";
    private static final String HOST_NAME_2 = "host-name-2";

    @Inject
    PoliciesHistoryTestHelper helper;

    @Inject
    PoliciesHistoryRepository repository;

    @Test
    void test() {

        /*
         * Some of the following history entries don't make sense from a functional perspective, but it doesn't matter.
         * The only goal here is to test things from a technical perspective.
         */
        PoliciesHistoryEntry historyEntry1 = helper.createPoliciesHistoryEntry(TENANT_ID_1, POLICY_ID_1, HOST_ID_1, HOST_NAME_1, 1L);
        PoliciesHistoryEntry historyEntry2 = helper.createPoliciesHistoryEntry(TENANT_ID_2, POLICY_ID_1, HOST_ID_1, HOST_NAME_1, 2L);
        PoliciesHistoryEntry historyEntry3 = helper.createPoliciesHistoryEntry(TENANT_ID_2, POLICY_ID_2, HOST_ID_1, HOST_NAME_1, 3L);
        PoliciesHistoryEntry historyEntry4 = helper.createPoliciesHistoryEntry(TENANT_ID_2, POLICY_ID_2, HOST_ID_1, HOST_NAME_2, 3L);
        PoliciesHistoryEntry historyEntry5 = helper.createPoliciesHistoryEntry(TENANT_ID_2, POLICY_ID_2, HOST_ID_2 + "-foo", HOST_NAME_2, 4L);
        PoliciesHistoryEntry historyEntry6 = helper.createPoliciesHistoryEntry(TENANT_ID_2, POLICY_ID_2, HOST_ID_2, "bar-" + HOST_NAME_2, 5L);

        /*
         * Pager #1: empty settings.
         */
        Pager pager = Pager.builder().build();

        assertEquals(0, repository.count("unknown-tenant-id", POLICY_ID_1, pager));
        assertEquals(0, repository.count(TENANT_ID_1, UUID.randomUUID(), pager));
        assertEquals(1, repository.count(TENANT_ID_1, POLICY_ID_1, pager));
        assertEquals(1, repository.count(TENANT_ID_2, POLICY_ID_1, pager));
        assertEquals(4, repository.count(TENANT_ID_2, POLICY_ID_2, pager));

        assertTrue(repository.find("unknown-tenant-id", POLICY_ID_1, pager).isEmpty());
        assertTrue(repository.find(TENANT_ID_1, UUID.randomUUID(), pager).isEmpty());
        assertEquals(List.of(historyEntry1), repository.find(TENANT_ID_1, POLICY_ID_1, pager));
        assertEquals(List.of(historyEntry2), repository.find(TENANT_ID_2, POLICY_ID_1, pager));
        assertEquals(List.of(historyEntry6, historyEntry5, historyEntry3, historyEntry4), repository.find(TENANT_ID_2, POLICY_ID_2, pager));

        /*
         * Pager #2: single EQUAL filter.
         */
        pager = Pager.builder().filter("name", EQUAL, "host-name").build();
        assertEquals(0, repository.count(TENANT_ID_2, POLICY_ID_2, pager));
        assertTrue(repository.find(TENANT_ID_2, POLICY_ID_2, pager).isEmpty());

        /*
         * Pager #3: single LIKE filter.
         */
        pager = Pager.builder().filter("name", LIKE, "host-name-2").build();
        assertEquals(3, repository.count(TENANT_ID_2, POLICY_ID_2, pager));
        assertEquals(List.of(historyEntry6, historyEntry5, historyEntry4), repository.find(TENANT_ID_2, POLICY_ID_2, pager));

        /*
         * Pager #4: single NOT_EQUAL filter.
         */
        pager = Pager.builder().filter("name", NOT_EQUAL, "red-hat").build();
        assertEquals(4, repository.count(TENANT_ID_2, POLICY_ID_2, pager));
        assertEquals(List.of(historyEntry6, historyEntry5, historyEntry3, historyEntry4), repository.find(TENANT_ID_2, POLICY_ID_2, pager));

        /*
         * Pager #5: combined EQUAL and LIKE filters.
         */
        pager = Pager.builder().filter("id", EQUAL, HOST_ID_2).filter("name", LIKE, "bar-").build();
        assertEquals(1, repository.count(TENANT_ID_2, POLICY_ID_2, pager));
        assertEquals(List.of(historyEntry6), repository.find(TENANT_ID_2, POLICY_ID_2, pager));

        /*
         * Pager #6: single sort.
         */
        pager = Pager.builder().addSort("ctime", Ascending).build();
        assertEquals(4, repository.count(TENANT_ID_2, POLICY_ID_2, pager));
        assertEquals(List.of(historyEntry3, historyEntry4, historyEntry5, historyEntry6), repository.find(TENANT_ID_2, POLICY_ID_2, pager));

        /*
         * Pager #7: combined sorts.
         */
        pager = Pager.builder().addSort("id", Descending).addSort("name", Ascending).build();
        assertEquals(4, repository.count(TENANT_ID_2, POLICY_ID_2, pager));
        assertEquals(List.of(historyEntry5, historyEntry6, historyEntry3, historyEntry4), repository.find(TENANT_ID_2, POLICY_ID_2, pager));

        /*
         * Pager #8: limit.
         */
        pager = Pager.builder().itemsPerPage(2).build();
        assertEquals(4, repository.count(TENANT_ID_2, POLICY_ID_2, pager));
        assertEquals(List.of(historyEntry6, historyEntry5), repository.find(TENANT_ID_2, POLICY_ID_2, pager));

        /*
         * Pager #9: limit and offset.
         */
        pager = Pager.builder().itemsPerPage(1).page(3).build();
        assertEquals(4, repository.count(TENANT_ID_2, POLICY_ID_2, pager));
        assertEquals(List.of(historyEntry4), repository.find(TENANT_ID_2, POLICY_ID_2, pager));

        /*
         * Pager #10: everything.
         * The query result is not asserted. This is only here to make sure the app doesn't go boom when everything is combined.
         */
        pager = Pager.builder()
                .itemsPerPage(2)
                .page(3)
                .filter("name", EQUAL, "foo")
                .filter("id", LIKE, "bar")
                .addSort("name", Ascending)
                .addSort("ctime", Descending).build();
        repository.find(TENANT_ID_2, POLICY_ID_2, pager);
    }
}
