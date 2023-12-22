package com.redhat.cloud.policies.app.model.history;

import com.redhat.cloud.policies.app.PoliciesHistoryTestHelper;
import com.redhat.cloud.policies.app.TestLifecycleManager;
import com.redhat.cloud.policies.app.model.pager.Pager;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;

import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.EQUAL;
import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.LIKE;
import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.NOT_EQUAL;
import static io.quarkus.panache.common.Sort.Direction.Ascending;
import static io.quarkus.panache.common.Sort.Direction.Descending;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PoliciesHistoryRepositoryTest {

    private static final String TENANT_ID_1 = "tenant-id-1";
    private static final String TENANT_ID_2 = "tenant-id-2";
    private static final String TENANT_ID_3 = "tenant-id-3";
    private static final String ORG_ID_1 = "org-id-1";
    private static final String ORG_ID_2 = "org-id-2";
    private static final String ORG_ID_3 = "org-id-3";
    private static final UUID POLICY_ID_1 = UUID.randomUUID();
    private static final UUID POLICY_ID_2 = UUID.randomUUID();
    private static final UUID POLICY_ID_3 = UUID.randomUUID();
    private static final String HOST_ID_1 = "host-id-1";
    private static final String HOST_ID_2 = "host-id-2";
    private static final String HOST_ID_3 = "host-id-4";
    private static final String HOST_NAME_1 = "host-name-1";
    private static final String HOST_NAME_2 = "host-name-2";
    private static final String HOST_NAME_3 = "host-name-4";
    private static final UUID GROUP_ID_1 = UUID.randomUUID();
    private static final UUID GROUP_ID_2 = UUID.randomUUID();

    @Inject
    PoliciesHistoryTestHelper helper;

    @Inject
    PoliciesHistoryRepository repository;

    private static List<PoliciesHistoryEntry> insertedEntries;

    @BeforeAll
    void setUp() {
        insertedEntries = List.of(
            helper.createPoliciesHistoryEntry(TENANT_ID_1, ORG_ID_1, POLICY_ID_1, HOST_ID_1, HOST_NAME_1, 1L),
            helper.createPoliciesHistoryEntry(TENANT_ID_2, ORG_ID_2, POLICY_ID_1, HOST_ID_1, HOST_NAME_1, 2L),
            helper.createPoliciesHistoryEntry(TENANT_ID_2, ORG_ID_2, POLICY_ID_2, HOST_ID_1, HOST_NAME_1, 3L),
            helper.createPoliciesHistoryEntry(TENANT_ID_2, ORG_ID_2, POLICY_ID_2, HOST_ID_1, HOST_NAME_2, 3L),
            helper.createPoliciesHistoryEntry(TENANT_ID_2, ORG_ID_2, POLICY_ID_2, HOST_ID_2 + "-foo", HOST_NAME_2, 4L),
            helper.createPoliciesHistoryEntry(TENANT_ID_2, ORG_ID_2, POLICY_ID_2, HOST_ID_2, "bar-" + HOST_NAME_2, 5L),

            helper.createPoliciesHistoryEntry(TENANT_ID_3, ORG_ID_3, POLICY_ID_3, HOST_ID_1, HOST_NAME_1,1L),
            helper.createPoliciesHistoryEntry(TENANT_ID_3, ORG_ID_3, POLICY_ID_3, HOST_ID_3, HOST_NAME_3,
                                              List.of(GROUP_ID_1), 1L),
            helper.createPoliciesHistoryEntry(TENANT_ID_3, ORG_ID_3, POLICY_ID_3, HOST_ID_2, HOST_NAME_2,
                                              List.of(GROUP_ID_2), 1L)
        );
    }

    @Test
    void testPagerEmptySettings() {
        Pager pager = Pager.builder().build();

        assertEquals(0, repository.count("unknown-org-id", null, POLICY_ID_1, pager));
        assertEquals(0, repository.count(ORG_ID_1, null, UUID.randomUUID(), pager));
        assertEquals(1, repository.count(ORG_ID_1, null, POLICY_ID_1, pager));
        assertEquals(1, repository.count(ORG_ID_2, null, POLICY_ID_1, pager));
        assertEquals(4, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));

        assertTrue(repository.find("unknown-org-id", null, POLICY_ID_1, pager).isEmpty());
        assertTrue(repository.find(ORG_ID_1, null, UUID.randomUUID(), pager).isEmpty());
        assertEquals(List.of(insertedEntries.get(0)), repository.find(ORG_ID_1, null, POLICY_ID_1, pager));
        assertEquals(List.of(insertedEntries.get(1)), repository.find(ORG_ID_2, null, POLICY_ID_1, pager));
        assertEquals(List.of(insertedEntries.get(5), insertedEntries.get(4), insertedEntries.get(2), insertedEntries.get(3)), repository.find(ORG_ID_2, null, POLICY_ID_2, pager));
    }

    @Test
    void testPagerSingleEqualFilter() {
        Pager pager = Pager.builder().filter("name", EQUAL, "host-name").build();
        assertEquals(0, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));
        assertTrue(repository.find(ORG_ID_2, null, POLICY_ID_2, pager).isEmpty());
    }

    @Test
    void testPagerSingleNotEqualFilter() {
        Pager pager = Pager.builder().filter("name", NOT_EQUAL, "red-hat").build();
        assertEquals(4, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));
        assertEquals(List.of(insertedEntries.get(5), insertedEntries.get(4), insertedEntries.get(2), insertedEntries.get(3)), repository.find(ORG_ID_2, null, POLICY_ID_2, pager));
    }

    @Test
    void testPagerSingleLikeFilter() {
        Pager pager = Pager.builder().filter("name", LIKE, "host-name-2").build();
        assertEquals(3, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));
        assertEquals(List.of(insertedEntries.get(5), insertedEntries.get(4), insertedEntries.get(3)), repository.find(ORG_ID_2, null, POLICY_ID_2, pager));
    }

    @Test
    void testPagerCombinedEqAndLikeFilters() {
        Pager pager = Pager.builder().filter("id", EQUAL, HOST_ID_2).filter("name", LIKE, "bar-").build();
        assertEquals(1, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));
        assertEquals(List.of(insertedEntries.get(5)), repository.find(ORG_ID_2, null, POLICY_ID_2, pager));
    }

    @Test
    void testPagerSingleSort() {
        Pager pager = Pager.builder().addSort("ctime", Ascending).build();
        assertEquals(4, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));
        assertEquals(List.of(insertedEntries.get(2), insertedEntries.get(3), insertedEntries.get(4), insertedEntries.get(5)), repository.find(ORG_ID_2, null, POLICY_ID_2, pager));
    }

    @Test
    void testCombinedSorts() {
        Pager pager = Pager.builder().addSort("id", Descending).addSort("name", Ascending).build();
        assertEquals(4, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));
        assertEquals(List.of(insertedEntries.get(4), insertedEntries.get(5), insertedEntries.get(2), insertedEntries.get(3)), repository.find(ORG_ID_2, null, POLICY_ID_2, pager));
    }

    @Test
    void testPagerLimit() {
        Pager pager = Pager.builder().itemsPerPage(2).build();
        assertEquals(4, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));
        assertEquals(List.of(insertedEntries.get(5), insertedEntries.get(4)), repository.find(ORG_ID_2, null, POLICY_ID_2, pager));
    }

    @Test
    void testPagerLimitAndOffset() {
        Pager pager = Pager.builder().itemsPerPage(1).page(3).build();
        assertEquals(4, repository.count(ORG_ID_2, null, POLICY_ID_2, pager));
        assertEquals(List.of(insertedEntries.get(3)), repository.find(ORG_ID_2, null, POLICY_ID_2, pager));
    }

    @Test
    void testPager() {
        /*
         * The query result is not asserted. This is only here to make sure the app doesn't go boom when everything is combined.
         */
        Pager pager = Pager.builder()
                .itemsPerPage(2)
                .page(3)
                .filter("name", EQUAL, "foo")
                .filter("id", LIKE, "bar")
                .addSort("name", Ascending)
                .addSort("ctime", Descending).build();
        repository.find(ORG_ID_2, null, POLICY_ID_2, pager);
    }

    @Test
    void testHostGroupsNoResult() {
        Pager pager = Pager.builder().build();
        long count = repository.count(ORG_ID_3, List.of(UUID.randomUUID()), POLICY_ID_3, pager);
        assertEquals(0, count);
        assertTrue(repository.find(ORG_ID_3, List.of(UUID.randomUUID()), POLICY_ID_3, pager).isEmpty());
    }

    @Test
    void testHostGroupsWithNoGroup() {
        Pager pager = Pager.builder().build();

        List<UUID> hostGroupIds = new ArrayList<UUID>();
        // Passing a null within hostGroupIds to lookup entries
        // of systems that are not in a group.
        // Default value of host_groups is `[]` empty JSONB array.
        hostGroupIds.add((UUID) null);
        assertEquals(4, repository.count(ORG_ID_2, hostGroupIds, POLICY_ID_2, pager));
        assertEquals(1, repository.count(ORG_ID_3, hostGroupIds, POLICY_ID_3, pager));

        List<PoliciesHistoryEntry> entries = repository.find(ORG_ID_2, hostGroupIds, POLICY_ID_2, pager);
        assertEquals(4, entries.size());
        for (PoliciesHistoryEntry entry : entries) {
            assertEquals(0, entry.getHostGroups().size(), entry.toString());
        }

        entries = repository.find(ORG_ID_3, hostGroupIds, POLICY_ID_3, pager);
        assertEquals(1, entries.size());
        assertEquals(0, entries.get(0).getHostGroups().size());
    }

    @Test
    void testHostGroupsOneGroup() {
        Pager pager = Pager.builder().build();
        List<UUID> hostGroupIds = List.of(GROUP_ID_1);
        assertEquals(1, repository.count(ORG_ID_3, hostGroupIds, POLICY_ID_3, pager));
        assertEquals(0, repository.count(ORG_ID_2, hostGroupIds, POLICY_ID_2, pager));

        List<PoliciesHistoryEntry> entries = repository.find(ORG_ID_3, hostGroupIds, POLICY_ID_3, pager);
        assertEquals(1, entries.size());
        assertEquals(1, entries.get(0).getHostGroups().size());
        assertEquals(GROUP_ID_1.toString(), entries.get(0).getHostGroups().getJsonObject(0).getString("id"));
    }

    @Test
    void testHostGroupsTwoGroupsLookup() {
        Pager pager = Pager.builder().build();
        List<UUID> hostGroupIds = List.of(UUID.randomUUID(), GROUP_ID_1);
        assertEquals(1, repository.count(ORG_ID_3, hostGroupIds, POLICY_ID_3, pager));
        assertEquals(0, repository.count(ORG_ID_2, hostGroupIds, POLICY_ID_2, pager));

        List<PoliciesHistoryEntry> entries = repository.find(ORG_ID_3, hostGroupIds, POLICY_ID_3, pager);
        assertEquals(1, entries.size());
        assertEquals(1, entries.get(0).getHostGroups().size());
        assertEquals(GROUP_ID_1.toString(), entries.get(0).getHostGroups().getJsonObject(0).getString("id"));
    }

    @Test
    void testostGroupsCombined() {
        Pager pager = Pager.builder().build();

        List<UUID> hostGroupIds = new ArrayList<UUID>();
        // Passing a null within hostGroupIds to lookup entries
        // of systems that are not in a group.
        // Default value of host_groups is `[]` empty JSONB array.
        hostGroupIds.add((UUID) null);
        hostGroupIds.add(GROUP_ID_1);

        assertEquals(2, repository.count(ORG_ID_3, hostGroupIds, POLICY_ID_3, pager));

        List<PoliciesHistoryEntry> entries = repository.find(ORG_ID_3, hostGroupIds, POLICY_ID_3, pager);
        assertEquals(2, entries.size());
        for (PoliciesHistoryEntry entry : entries) {
            if (entry.getHostGroups().size() > 0) {
                assertEquals(GROUP_ID_1.toString(), entry.getHostGroups().getJsonObject(0).getString("id"),
                             entry.toString());
            }
        }
    }
}
