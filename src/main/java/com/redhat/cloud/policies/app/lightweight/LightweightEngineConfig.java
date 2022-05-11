package com.redhat.cloud.policies.app.lightweight;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

// TODO POL-649 Remove this class after we're done migrating to the lightweight engine.
@ApplicationScoped
public class LightweightEngineConfig {

    private static final Logger LOGGER = Logger.getLogger(LightweightEngineConfig.class);

    @ConfigProperty(name = "lightweight-engine.enabled", defaultValue = "false")
    boolean enabled;

    public void runAtStartup(@Observes StartupEvent event) {
        LOGGER.infof("The lightweight engine is %s", enabled ? "enabled" : "disabled");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
      * <b>/!\ WARNING /!\</b> Do not use this method from runtime code.
      */
    public void overrideForTest(boolean enabled) {
        this.enabled = enabled;
    }
}
