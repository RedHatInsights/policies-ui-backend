package com.redhat.cloud.policies.app.lightweight;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;

@ApplicationScoped
public class AccountLatestUpdateRepository {

    @Inject
    EntityManager entityManager;

    public void setLatestToNow(String accountId) {
        String sql = "INSERT INTO account_latest_update (account_id, latest) SELECT :accountId, :latest " +
                "ON CONFLICT (accountId) DO UPDATE SET latest = :latest";
        entityManager.createNativeQuery(sql)
                .setParameter("accountId", accountId)
                .setParameter("latest", LocalDateTime.now(UTC))
                .executeUpdate();
    }
}
