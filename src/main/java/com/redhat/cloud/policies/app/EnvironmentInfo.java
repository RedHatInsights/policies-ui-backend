package com.redhat.cloud.policies.app;

import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;

@Startup
public class EnvironmentInfo {

    private static final Logger LOG = Logger.getLogger(EnvironmentInfo.class);

    @ConfigProperty(name = "env.name", defaultValue = "")
    String environmentName;

    @PostConstruct
    public void init() {
        LOG.infof("Environment: %s", environmentName);
    }

    public Boolean isFedramp() {
        return environmentName.equalsIgnoreCase("fedramp-stage") || environmentName.equalsIgnoreCase("fedramp-prod");
    }
}
