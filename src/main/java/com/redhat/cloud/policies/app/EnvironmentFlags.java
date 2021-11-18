package com.redhat.cloud.policies.app;

import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;

@Startup
public class EnvironmentFlags {

    private static final Logger LOG = Logger.getLogger(EnvironmentFlags.class);

    @PostConstruct
    public void init() {
        try {
            LOG.info("Environment flags in use:");
            for (Field field: EnvironmentFlags.class.getDeclaredFields()) {
                ConfigProperty configAnnotation = field.getAnnotation(ConfigProperty.class);
                if (configAnnotation != null) {
                    LOG.infof("  - %s: %s", configAnnotation.name(), field.get(this));
                }
            }
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("Error getting flags while starting.", iae);
        }
    }

    @ConfigProperty(name = "flag.is-fedramp", defaultValue = "false")
    Boolean isFedramp;

    public Boolean isFedramp() {
        return isFedramp;
    }
}
