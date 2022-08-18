package com.redhat.cloud.policies.app.lightweight;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;

@ApplicationScoped
public class OrgIdLatestUpdateRepository {

    @Inject
    EntityManager entityManager;

    @Transactional
    public void setLatestToNow(String orgId) {
        String sql = "INSERT INTO org_id_latest_update (org_id, latest) SELECT :orgId, :latest " +
                "ON CONFLICT (org_id) DO UPDATE SET latest = :latest";
        entityManager.createNativeQuery(sql)
                .setParameter("orgId", orgId)
                .setParameter("latest", LocalDateTime.now(UTC))
                .executeUpdate();
    }
}
