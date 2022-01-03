package com.redhat.cloud.policies.app;

import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Startup
public class EnvironmentInfo {

    private static final Logger LOG = Logger.getLogger(EnvironmentInfo.class);

    @ConfigProperty(name = "env.name")
    Optional<String> environmentName;

    @PostConstruct
    public void init() {
        if (environmentName.isPresent()) {
            LOG.infof("Environment: %s", environmentName.get());
        } else {
            LOG.infof("Environment is not set");
        }
    }

    public boolean isFedramp() {
        return environmentName.isPresent() && (
                environmentName.get().equalsIgnoreCase("fedramp-stage") ||
                environmentName.get().equalsIgnoreCase("fedramp-prod")
        );
    }
}
