package com.redhat.cloud.policies.app;

import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.Session;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;

import static java.util.concurrent.TimeUnit.MINUTES;

@Singleton
public class PoliciesHistoryCleaner {

    public static final String POLICIES_HISTORY_CLEANER_DELETE_AFTER_CONF_KEY = "policies-history.cleaner.delete-after";

    private static final Logger LOGGER = Logger.getLogger(PoliciesHistoryCleaner.class);
    private static final Duration DEFAULT_DELETE_DELAY = Duration.ofDays(14L);

    @Inject
    Session session;

    /**
     * The policies UI shows a retention time for policies history entries.
     * This scheduled job deletes from the database the history entries which are older than that retention time.
     */
    /*
     * TODO The scheduling is delayed to prevent an unwanted execution during tests. Remove the delay and set the period
     * to `disabled` after the Quarkus 2 bump. See https://quarkus.io/guides/scheduler-reference for more details.
     */
    @Scheduled(identity = "PoliciesHistoryCleaner", delay = 10L, delayUnit = MINUTES, every = "{policies-history.cleaner.period}")
    @Transactional
    public void clean() {
        Duration deleteDelay = ConfigProvider.getConfig().getOptionalValue(POLICIES_HISTORY_CLEANER_DELETE_AFTER_CONF_KEY, Duration.class)
                .orElse(DEFAULT_DELETE_DELAY);
        Instant deleteBefore = Instant.now().minus(deleteDelay);
        LOGGER.infof("Policies history purge starting. Entries older than %s will be deleted.", deleteBefore.toString());
        int deleted = session.createQuery("DELETE FROM PoliciesHistoryEntry WHERE ctime < :ctime")
                .setParameter("ctime", deleteBefore.toEpochMilli())
                .executeUpdate();
        LOGGER.infof("Policies history purge ended. %d entries were deleted from the database.", deleted);
    }
}
