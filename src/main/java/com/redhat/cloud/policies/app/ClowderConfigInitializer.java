package com.redhat.cloud.policies.app;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.Optional;

@ApplicationScoped
public class ClowderConfigInitializer {

    private static final Logger LOGGER = Logger.getLogger(ClowderConfigInitializer.class);
    private static final String ENGINE_URL_KEY = "engine/mp-rest/url";
    private static final String NOTIFICATIONS_URL_KEY = "notifications/mp-rest/url";
    private static final String RBAC_URL_KEY = "rbac/mp-rest/url";

    @ConfigProperty(name = "clowder.endpoints.policies-engine-service")
    Optional<String> policiesEngineClowderEndpoint;

    @ConfigProperty(name = "clowder.endpoints.notifications-backend-service")
    Optional<String> notificationsBackendClowderEndpoint;

    @ConfigProperty(name = "clowder.endpoints.rbac-service")
    Optional<String> rbacClowderEndpoint;

    void init(@Observes StartupEvent event) {
        if (policiesEngineClowderEndpoint.isPresent()) {
            String engineUrl = "http://" + policiesEngineClowderEndpoint.get();
            LOGGER.infof("Overriding the policies-engine URL with the config value from Clowder: %s", engineUrl);
            System.setProperty(ENGINE_URL_KEY, engineUrl);
        }
        if (notificationsBackendClowderEndpoint.isPresent()) {
            String notificationsUrl = "http://" + notificationsBackendClowderEndpoint.get();
            LOGGER.infof("Overriding the notifications-backend URL with the config value from Clowder: %s", notificationsUrl);
            System.setProperty(NOTIFICATIONS_URL_KEY, notificationsUrl);
        }
        if (rbacClowderEndpoint.isPresent()) {
            String rbacUrl = "http://" + rbacClowderEndpoint.get();
            LOGGER.infof("Overriding the RBAC URL with the config value from Clowder: %s", rbacUrl);
            System.setProperty(RBAC_URL_KEY, rbacUrl);
        }
    }
}
