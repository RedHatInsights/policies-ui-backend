package com.redhat.cloud.policies.app;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EnvironmentFlags {

    @ConfigProperty(name = "flag.is-fedramp", defaultValue = "false")
    Boolean isFedramp;

    public Boolean isFedramp() {
        return isFedramp;
    }
}
