package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.history.PoliciesHistoryEntry;
import org.hibernate.Session;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class PoliciesHistoryTestHelper {

    @Inject
    Session session;

    @Transactional
    public PoliciesHistoryEntry createPoliciesHistoryEntry(String tenantId, UUID policyId, String hostId, String hostName, long ctime) {
        PoliciesHistoryEntry historyEntry = new PoliciesHistoryEntry();
        historyEntry.setId(UUID.randomUUID());
        historyEntry.setTenantId(tenantId);
        historyEntry.setPolicyId(policyId.toString());
        historyEntry.setHostId(hostId);
        historyEntry.setHostName(hostName);
        historyEntry.setCtime(ctime);
        session.persist(historyEntry);
        return historyEntry;
    }
}
