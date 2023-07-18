package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import org.hibernate.Session;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PoliciesHistoryTestHelper {

    @Inject
    Session session;

    @Transactional
    public PoliciesHistoryEntry createPoliciesHistoryEntry(String tenantId, String orgId, UUID policyId, String hostId, String hostName, long ctime) {
        PoliciesHistoryEntry historyEntry = newPoliciesHistoryEntry(tenantId, orgId, policyId, hostId, hostName, ctime);
        session.persist(historyEntry);
        return historyEntry;
    }

    @Transactional
    public PoliciesHistoryEntry createPoliciesHistoryEntry(String tenantId, String orgId, UUID policyId, String hostId, String hostName, List<UUID> hostGroupIds, long ctime) {
        PoliciesHistoryEntry historyEntry = newPoliciesHistoryEntry(tenantId, orgId, policyId, hostId, hostName, ctime);
        historyEntry.setHostGroups(hostGroupIdsToJsonArray(hostGroupIds));
        session.persist(historyEntry);
        return historyEntry;
    }

    PoliciesHistoryEntry newPoliciesHistoryEntry(String tenantId, String orgId, UUID policyId, String hostId, String hostName, long ctime) {
        PoliciesHistoryEntry historyEntry = new PoliciesHistoryEntry();
        historyEntry.setId(UUID.randomUUID());
        historyEntry.setTenantId(tenantId);
        historyEntry.setOrgId(orgId);
        historyEntry.setPolicyId(policyId.toString());
        historyEntry.setHostId(hostId);
        historyEntry.setHostName(hostName);
        historyEntry.setCtime(ctime);
        return historyEntry;
    }

    JsonArray hostGroupIdsToJsonArray(List<UUID> hostGroupIds) {
        JsonArray jsonArray = new JsonArray();
        for (UUID hostGroupId : hostGroupIds) {
            jsonArray.add(JsonObject.of("id", hostGroupId.toString()));
        }
        return jsonArray;
    }
}
