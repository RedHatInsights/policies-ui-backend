package com.redhat.cloud.policies.app;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.Set;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

// TODO POL-649 Retries on all calls: https://quarkus.io/guides/smallrye-fault-tolerance
// TODO POL-649 Exception mapper?
@Path("/lightweight-engine")
@RegisterRestClient(configKey = "engine")
public interface LightweightEngine {

    // TODO POL-649 The validation errors need to be properly forwarded to the browser.
    /**
     * Validates a condition with Hawkular.
     * @param condition the condition to validate
     */
    @PUT
    @Path("/validate")
    @Consumes(TEXT_PLAIN)
    void validateCondition(@NotNull String condition);

    /**
     * Orders the engine to reload triggers. For each trigger, if it was previously loaded by the engine and is still
     * available in the DB, the engine reloads it. If it was previously loaded and is no longer available in the DB,
     * the engine unloads it. If it has not been loaded yet and is available in the DB, the engine loads it.
     * @param accountId account ID
     * @param triggerIds the identifiers of the triggers to reload
     */
    @PUT
    @Path("/reload")
    @Consumes(APPLICATION_JSON)
    void reloadTriggers(@HeaderParam("Hawkular-Tenant") String accountId, @NotNull Set<UUID> triggerIds);
}
