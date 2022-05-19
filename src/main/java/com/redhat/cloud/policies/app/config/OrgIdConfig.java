package com.redhat.cloud.policies.app.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

// TODO POL-650 Remove that config after we are using orgId everywhere
@ApplicationScoped
public class OrgIdConfig {

    public static final String USE_ORG_ID = "policies.use-org-id";

    @ConfigProperty(name = USE_ORG_ID, defaultValue = "false")
    public boolean useOrgId;

    public boolean isUseOrgId() {
        return useOrgId;
    }
}

