/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.engine.FullTrigger;
import com.redhat.cloud.policies.app.model.Msg;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.redhat.cloud.policies.app.model.engine.Trigger;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Interface to the backend engine
 */
@Path("/hawkular/alerts")
@RegisterRestClient(configKey = "engine")
@RegisterProvider(value = EngineResponseExceptionMapper.class, priority = 50)
public interface PolicyEngine {

    /**
     * Store and/or verify the FullTrigger, which is a wrapped Policy.
     *
     * @param trigger
     *            Policy wrapped in a data structure, the engine expects
     * @param isDryRun
     *            If true, the engine will only verify the Policy, but not store it
     * @param customerId
     *            Id of the customer this policy belongs to.
     * 
     * @return A message with verification/store results.
     */
    @POST
    @Path("/triggers/trigger")
    @Consumes("application/json")
    @Produces("application/json")
    Msg storeTrigger(FullTrigger trigger, @QueryParam("dry") boolean isDryRun,
            @HeaderParam("Hawkular-Tenant") String customerId);

    @GET
    @Path("/triggers")
    @Produces("application/json")
    List<Trigger> findTriggersForCustomer(@HeaderParam("Hawkular-Tenant") String customerId);

    /**
     * Update a trigger
     *
     * @param trigger
     *            Policy wrapped in a data structure, the engine expects
     * @param customerId
     *            Id of the customer this policy belongs to.
     * 
     * @return A message with update results
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/triggers/trigger/{triggerId}")
    Msg updateTrigger(@PathParam("triggerId") UUID triggerId, FullTrigger trigger, @QueryParam("dry") boolean isDryRun,
            @HeaderParam("Hawkular-Tenant") String customerId);

    /**
     * Delete the trigger with the given id
     *
     * @param triggerId
     *            Id of the trigger to delete
     * @param customerId
     *            Id of the customer this policy belongs to.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/triggers/{triggerId}")
    void deleteTrigger(@PathParam("triggerId") UUID triggerId, @HeaderParam("Hawkular-Tenant") String customerId);

    /**
     * Get the trigger with the given ID
     *
     * @param triggerId
     *            Id of the trigger to retrieve
     * @param customerId
     *            Id of the customer this policy belongs to.
     * 
     * @return a FullTrigger
     */
    @GET
    @Produces("application/json")
    @Path("/triggers/trigger/{triggerId}")
    FullTrigger fetchTrigger(@PathParam("triggerId") UUID triggerId, @HeaderParam("Hawkular-Tenant") String customerId);

    @GET
    @Path("/triggers")
    @Produces("application/json")
    List<Trigger> findTriggersById(@QueryParam("triggerIds") String triggerIds,
            @HeaderParam("Hawkular-Tenant") String customerId);

    @PUT
    @Path("/triggers/{triggerId}/enable")
    void enableTrigger(@PathParam("triggerId") UUID triggerId, @HeaderParam("Hawkular-Tenant") String customerId);

    @DELETE
    @Path("/triggers/{triggerId}/enable")
    void disableTrigger(@PathParam("triggerId") UUID triggerId, @HeaderParam("Hawkular-Tenant") String customerId);

    @GET
    @Consumes("application/json")
    Response findLastTriggered(@QueryParam("triggerIds") String triggerIds, @QueryParam("thin") boolean thin,
            @QueryParam("page") int page, @QueryParam("per_page") int per_page,
            // Once MP-RestClient 2.0 is available, we can turn the next lines
            // into String[] and set the query param style accordingly.
            @QueryParam("sort") String sort1, @QueryParam("sort") String sort2, @QueryParam("order") String order1,
            @QueryParam("order") String order2, @QueryParam("tagQuery") String tagQuery,
            @HeaderParam("Hawkular-Tenant") String customerId);
}
