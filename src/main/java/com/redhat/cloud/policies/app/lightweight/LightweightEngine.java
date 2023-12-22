package com.redhat.cloud.policies.app.lightweight;

import com.redhat.cloud.policies.app.EngineResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

// TODO POL-649 Retries on all calls: https://quarkus.io/guides/smallrye-fault-tolerance
@Path("/lightweight-engine")
@RegisterRestClient(configKey = "engine")
@RegisterProvider(EngineResponseExceptionMapper.class)
public interface LightweightEngine {

    /**
     * Validates a condition like {@code facts.arch = 'x86_64'} with Hawkular.
     * @param condition the condition to validate
     */
    @PUT
    @Path("/validate")
    @Consumes(TEXT_PLAIN)
    void validateCondition(@NotNull String condition);
}
