package com.redhat.cloud.policies.app.lightweight;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

// TODO POL-649 Retries on all calls: https://quarkus.io/guides/smallrye-fault-tolerance
// TODO POL-649 Exception mapper?
@Path("/lightweight-engine")
@RegisterRestClient(configKey = "engine")
public interface LightweightEngine {

    // TODO POL-649 The validation errors need to be properly forwarded to the browser.
    /**
     * Validates a condition like {@code facts.arch = 'x86_64'} with Hawkular.
     * @param condition the condition to validate
     */
    @PUT
    @Path("/validate")
    @Consumes(TEXT_PLAIN)
    void validateCondition(@NotNull String condition);
}
