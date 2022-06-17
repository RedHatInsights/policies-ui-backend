package com.redhat.cloud.policies.app;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Startup
public class EnvironmentInfo {

    @ConfigProperty(name = "env.name")
    Optional<String> environmentName;

    @PostConstruct
    public void init() {
        if (environmentName.isPresent()) {
            Log.infof("Environment: %s", environmentName.get());
        } else {
            Log.infof("Environment is not set");
        }
    }

    public boolean isFedramp() {
        return environmentName.isPresent() && (
                environmentName.get().equalsIgnoreCase("fedramp-stage") ||
                environmentName.get().equalsIgnoreCase("fedramp-prod")
        );
    }
}
