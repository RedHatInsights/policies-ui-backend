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
package com.redhat.cloud.policies.app.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cloud.policies.app.lightweight.OrgIdLatestUpdateRepository;
import com.redhat.cloud.policies.app.lightweight.LightweightEngine;
import com.redhat.cloud.policies.app.auth.RhIdPrincipal;
import com.redhat.cloud.policies.app.model.Msg;
import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.UUIDHelperBean;
import com.redhat.cloud.policies.app.model.engine.HistoryItem;
import com.redhat.cloud.policies.app.model.history.PoliciesHistoryRepository;
import com.redhat.cloud.policies.app.model.pager.Page;
import com.redhat.cloud.policies.app.model.pager.Pager;
import com.redhat.cloud.policies.app.rest.utils.PagingUtils;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hibernate.exception.ConstraintViolationException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/policies/v1.0/policies")
@Produces("application/json")
@Consumes("application/json")
@Timed("PolicySvc")
@RequestScoped
public class PolicyCrudService {

    public static final String MISSING_PERMISSIONS_TO_RETRIEVE_POLICIES = "Missing permissions to retrieve policies";
    public static final String MISSING_PERMISSIONS_TO_VERIFY_POLICY = "Missing permissions to verify policy";
    public static final String MISSING_PERMISSIONS_TO_UPDATE_POLICY = "Missing permissions to update policy";

    public static final String CTIME_STRING = "ctime";

    @Inject
    @RestClient
    LightweightEngine lightweightEngine;

    @Inject
    OrgIdLatestUpdateRepository orgIdLatestUpdateRepository;

    @Context
    UriInfo uriInfo;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    RhIdPrincipal user;

    @Inject
    EntityManager entityManager;

    @Inject
    TransactionManager transactionManager;

    @Inject
    UUIDHelperBean uuidHelper;

    @Inject
    Validator validator;

    @Inject
    PoliciesHistoryRepository policiesHistoryRepository;

    @Inject
    ObjectMapper objectMapper;

    // workaround for returning generic types: https://github.com/swagger-api/swagger-core/issues/498#issuecomment-74510379
    // This class is used only for swagger return type
    private static class PagedResponseOfPolicy extends PagingUtils.PagedResponse<Policy> {
        private PagedResponseOfPolicy(Page<Policy> page) {
            super(page);
        }
    }

    @Operation(summary = "Return all policies for a given account")
    @GET
    @Path("/")
    @Parameters({
            @Parameter(
                    name = "offset",
                    in = ParameterIn.QUERY,
                    description = "Page number, starts 0, if not specified uses 0.",
                    schema = @Schema(type = SchemaType.INTEGER)
            ),
            @Parameter(
                    name = "limit",
                    in = ParameterIn.QUERY,
                    description = "Number of items per page, if not specified uses 50. " + Pager.NO_LIMIT + " can be used to specify an unlimited page, when specified it ignores the offset",
                    schema = @Schema(type = SchemaType.INTEGER)
            ),
            @Parameter(
                    name = "sortColumn",
                    in = ParameterIn.QUERY,
                    description = "Column to sort the results by",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "name",
                                    "description",
                                    "is_enabled",
                                    "mtime",
                                    "last_triggered"
                            }
                    )
            ),
            @Parameter(
                    name = "sortDirection",
                    in = ParameterIn.QUERY,
                    description = "Sort direction used",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "asc",
                                    "desc"
                            }
                    )
            ),
            @Parameter(
                    name = "filter[name]",
                    in = ParameterIn.QUERY,
                    description = "Filtering policies by the name depending on the Filter operator used.",
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "filter:op[name]",
                    in = ParameterIn.QUERY,
                    description = "Operations used with the filter",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "equal",
                                    "like",
                                    "ilike",
                                    "not_equal"
                            },
                            defaultValue = "equal"
                    )
            ),
            @Parameter(
                    name = "filter[description]",
                    in = ParameterIn.QUERY,
                    description = "Filtering policies by the description depending on the Filter operator used.",
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "filter:op[description]",
                    in = ParameterIn.QUERY,
                    description = "Operations used with the filter",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "equal",
                                    "like",
                                    "ilike",
                                    "not_equal"
                            },
                            defaultValue = "equal"
                    )
            ),
            @Parameter(
                    name = "filter[is_enabled]",
                    in = ParameterIn.QUERY,
                    description = "Filtering policies by the is_enabled field." +
                            "Defaults to true if no operand is given.",
                    schema = @Schema(type = SchemaType.STRING, defaultValue = "true", enumeration = {"true", "false"})
            ),
    })
    @APIResponse(responseCode = "400", description = "Bad parameter for sorting was passed")
    @APIResponse(responseCode = "404", description = "No policies found for customer")
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
    @APIResponse(responseCode = "200", description = "Policies found", content =
    @Content(schema = @Schema(implementation = PagedResponseOfPolicy.class)),
            headers = @Header(name = "TotalCount", description = "Total number of items found",
                    schema = @Schema(type = SchemaType.INTEGER)))
    public Response getPoliciesForCustomer() {

        if (!user.canReadPolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_RETRIEVE_POLICIES)).build();
        }

        Page<Policy> page;
        try {
            Pager pager = PagingUtils.extractPager(uriInfo);
            page = Policy.pagePoliciesForCustomer(entityManager, user.getOrgId(), pager);
        } catch (IllegalArgumentException iae) {
            return Response.status(400, iae.getLocalizedMessage()).build();
        }

        return PagingUtils.responseBuilder(page).build();
    }

    @Operation(summary = "Return all policy ids for a given account after applying the filters")
    @GET
    @Path("/ids")
    @Parameters({
            @Parameter(
                    name = "filter[name]",
                    in = ParameterIn.QUERY,
                    description = "Filtering policies by the name depending on the Filter operator used.",
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "filter:op[name]",
                    in = ParameterIn.QUERY,
                    description = "Operations used with the filter",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "equal",
                                    "like",
                                    "ilike",
                                    "not_equal"
                            },
                            defaultValue = "equal"
                    )
            ),
            @Parameter(
                    name = "filter[description]",
                    in = ParameterIn.QUERY,
                    description = "Filtering policies by the description depending on the Filter operator used.",
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "filter:op[description]",
                    in = ParameterIn.QUERY,
                    description = "Operations used with the filter",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "equal",
                                    "like",
                                    "ilike",
                                    "not_equal"
                            },
                            defaultValue = "equal"
                    )
            ),
            @Parameter(
                    name = "filter[is_enabled]",
                    in = ParameterIn.QUERY,
                    description = "Filtering policies by the is_enabled field." +
                            "Defaults to true if no operand is given.",
                    schema = @Schema(type = SchemaType.STRING, defaultValue = "true", enumeration = {"true", "false"})
            ),
    })
    @APIResponse(responseCode = "400", description = "Bad parameter for sorting was passed")
    @APIResponse(responseCode = "404", description = "No policies found for customer")
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
    @APIResponse(responseCode = "200", description = "PolicyIds found", content =
    @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = UUID.class)))
    public Response getPolicyIdsForCustomer() {

        if (!user.canReadPolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_RETRIEVE_POLICIES)).build();
        }

        List<UUID> uuids;
        try {
            Pager pager = PagingUtils.extractPager(uriInfo);
            uuids = Policy.getPolicyIdsForCustomer(entityManager, user.getOrgId(), pager);

        } catch (IllegalArgumentException iae) {
            return Response.status(400, iae.getLocalizedMessage()).build();
        }

        return Response.ok(uuids).build();
    }


    @Operation(summary = "Validate (and possibly persist) a passed policy for the given account")
    @Parameter(name = "alsoStore",
            description = "If passed and set to true, the passed policy is also persisted (if it is valid)")
    @APIResponses({
            @APIResponse(responseCode = "500", description = "Internal error"),
            @APIResponse(responseCode = "400", description = "No policy provided or policy validation failed",
                    content = @Content(schema = @Schema(implementation = Msg.class))),
            @APIResponse(responseCode = "409", description = "Persisting failed",
                    content = @Content(schema = @Schema(implementation = Msg.class))),
            @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action"),
            @APIResponse(responseCode = "201", description = "Policy persisted",
                    content = @Content(schema = @Schema(implementation = Policy.class))),
            @APIResponse(responseCode = "200", description = "Policy validated")
    })
    @POST
    @Path("/")
    @Transactional
    public Response storePolicy(@QueryParam("alsoStore") boolean alsoStore, @NotNull @Valid Policy policy) {

        if (!user.canReadPolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_VERIFY_POLICY)).build();
        }

        // We use the indirection, so that for testing we can produce known UUIDs
        policy.id = uuidHelper.getUUID();
        policy.customerid = user.getAccount();
        policy.orgId = user.getOrgId();

        Response invalidNameResponse = isNameUnique(policy);
        if (invalidNameResponse != null) {
            return invalidNameResponse;
        }

        try {
            lightweightEngine.validateCondition(policy.conditions);
        } catch (Exception e) {
            return Response.status(400, e.getMessage()).entity(getEngineExceptionMsg(e)).build();
        }

        if (!alsoStore) {
            return Response.status(200).entity(new Msg("Policy validated")).build();
        }

        if (!user.canWritePolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to store policy")).build();
        }

        policy.persist();
        setLatestToNow();

        // Policy is persisted. Return its location.
        URI location =
                UriBuilder.fromResource(PolicyCrudService.class).path(PolicyCrudService.class, "getPolicy").build(policy.id);
        return Response.created(location).entity(policy).build();
    }

    private Response getResponseSavingPolicyThrowable(Throwable t) {
        if (t instanceof PersistenceException && t.getCause() instanceof ConstraintViolationException) {
            return Response.status(409, t.getMessage()).entity(new Msg("Constraint violation")).build();
        } else {
            Log.warn("Getting response failed", t);
            return Response.status(500, t.getMessage()).build();
        }
    }

    private Msg getEngineExceptionMsg(Exception e) {
        Msg msg;
        if (e instanceof RuntimeException && e.getCause() instanceof ConnectException
                || e instanceof ProcessingException) {
            msg = new Msg("Connection to backend-engine failed. Please retry later");
        } else {
            msg = new Msg(e.getMessage());
        }
        return msg;
    }

    @Operation(summary = "Delete a single policy for a customer by its id")
    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "Policy deleted")
    @APIResponse(responseCode = "404", description = "Policy not found")
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
    @Parameter(name = "id", description = "UUID of the policy")
    @Transactional
    public Response deletePolicy(@PathParam("id") UUID policyId) {

        if (!user.canWritePolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to delete policy")).build();
        }

        Policy policy = findPolicy(policyId);

        if (policy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            policy.delete(policy);
            setLatestToNow();
            return Response.ok(policy).build();
        }
    }

    @Operation(summary = "Delete policies for a customer by the ids passed in the body. Result will be a list of deleted UUIDs")
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
    @APIResponse(responseCode = "200", description = "Policies deleted",
            content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = UUID.class)))
    @DELETE
    @Path("/ids")
    @Transactional
    public Response deletePolicies(List<UUID> uuids) {

        if (!user.canWritePolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to delete policy")).build();
        }

        boolean dbUpdated = false;
        for (UUID uuid : uuids) {
            Policy policy = findPolicy(uuid);
            if (policy != null) {
                policy.delete();
                dbUpdated = true;
            }
        }
        if (dbUpdated) {
            setLatestToNow();
        }
        return Response.ok(uuids).build();
    }

    @Operation(summary = "Enable/disable a policy")
    @Parameter(name = "id", description = "ID of the Policy")
    @Parameter(name = "enabled",
            schema = @Schema(type = SchemaType.BOOLEAN, defaultValue = "false"),
            description = "Should the policy be enabled (true) or disabled (false, default)")
    @APIResponse(responseCode = "200", description = "Policy updated")
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
    @APIResponse(responseCode = "404", description = "Policy not found")
    @APIResponse(responseCode = "500", description = "Updating failed")
    @POST
    @Path("/{id:[0-9a-fA-F-]+}/enabled")
    @Transactional
    public Response setEnabledStateForPolicy(@PathParam("id") UUID policyId, @QueryParam("enabled") boolean shouldBeEnabled) {
        if (!user.canWritePolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_UPDATE_POLICY)).build();
        }

        Policy storedPolicy = findPolicy(policyId);

        if (storedPolicy == null) {
            return Response.status(404, "Original policy not found").build();
        } else {
            storedPolicy.isEnabled = shouldBeEnabled;
            storedPolicy.setMtimeToNow();
            storedPolicy.persist();
            setLatestToNow();
            return Response.ok().build();
        }
    }

    @Operation(summary = "Enable/disable policies identified by list of uuid in body")
    @Parameter(name = "uuids", schema = @Schema(type = SchemaType.ARRAY, implementation = UUID.class))
    @APIResponse(responseCode = "200", description = "Policy updated", content = @Content(
            schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = UUID.class
            )
    ))
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
    @POST
    @Path("/ids/enabled")
    @Transactional
    public Response setEnabledStateForPolicies(@QueryParam("enabled") boolean shouldBeEnabled, @NotEmpty List<UUID> uuids) {
        if (!user.canWritePolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_UPDATE_POLICY)).build();
        }

        List<UUID> changed = new ArrayList<>(uuids.size());
        for (UUID uuid : uuids) {
            Policy storedPolicy = findPolicy(uuid);
            if (storedPolicy != null) {
                storedPolicy.isEnabled = shouldBeEnabled;
                storedPolicy.setMtimeToNow();
                storedPolicy.persist();
                changed.add(uuid);
            }
        }
        if (!changed.isEmpty()) {
            setLatestToNow();
        }
        return Response.ok(changed).build();
    }

    @Operation(summary = "Update a single policy for a customer by its id")
    @PUT
    @Path("/{policyId}")
    @APIResponse(responseCode = "200", description = "Policy updated or policy validated",
            content = @Content(schema = @Schema(implementation = Policy.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid or no policy provided")
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
    @APIResponse(responseCode = "404", description = "Policy did not exist - did you store it before?")
    @APIResponse(responseCode = "409", description = "Persisting failed",
            content = @Content(schema = @Schema(implementation = Msg.class))
    )
    @Transactional
    public Response updatePolicy(@QueryParam("dry") boolean dryRun, @PathParam("policyId") UUID policyId,
                                 @NotNull @Valid Policy policy) {

        if (!user.canWritePolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_UPDATE_POLICY)).build();
        }

        Policy storedPolicy = findPolicy(policyId);

        ResponseBuilder builder;
        if (storedPolicy == null) {
            builder = Response.status(404, "Original policy not found");
        } else {
            if (!policy.id.equals(policyId)) {
                builder = Response.status(400, "Invalid policy");
            } else {

                Response invalidNameResponse = isNameUnique(policy);
                if (invalidNameResponse != null) {
                    return invalidNameResponse;
                }

                try {
                    lightweightEngine.validateCondition(policy.conditions);
                } catch (Exception e) {
                    return Response.status(400, e.getMessage()).entity(getEngineExceptionMsg(e)).build();
                }

                if (dryRun) {
                    return Response.status(200).entity(new Msg("Policy validated")).build();
                }

                // All is good, we can now do the real work
                try {
                    storedPolicy.populateFrom(policy);
                    storedPolicy.setMtimeToNow();

                    setLatestToNow();
                    return Response.ok(storedPolicy).build();
                } catch (Throwable t) {
                    try {
                        transactionManager.setRollbackOnly();
                    } catch (SystemException ex) {
                        throw new RuntimeException(ex);
                    }

                    return getResponseSavingPolicyThrowable(t);
                }
            }
        }

        return builder.build();
    }

    @Operation(summary = "Validates a Policy condition")
    @POST
    @Path("/validate")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Condition validated", content = @Content(schema = @Schema(implementation = Msg.class))),
            @APIResponse(responseCode = "400", description = "No policy provided or condition not valid", content = @Content(schema = @Schema(implementation = Msg.class))),
            @APIResponse(responseCode = "500", description = "Internal error")
    })
    public Response validateCondition(@Valid @NotNull Policy policy) {

        if (!user.canReadPolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_VERIFY_POLICY)).build();
        }

        try {
            lightweightEngine.validateCondition(policy.conditions);
        } catch (Exception e) {
            return Response.status(400, e.getMessage()).entity(getEngineExceptionMsg(e)).build();
        }

        return Response.status(200).entity(new Msg("Policy.condition validated")).build();

    }

    @Operation(summary = "Validates the Policy.name and verifies if it is unique.")
    @POST
    @Path("/validate-name")
    @RequestBody(content = {@Content(schema = @Schema(type = SchemaType.STRING))})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Name validated", content = @Content(schema = @Schema(implementation = Msg.class))),
            @APIResponse(responseCode = "400", description = "Policy validation failed", content = @Content(schema = @Schema(implementation = Msg.class))),
            @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action", content = @Content(schema = @Schema(implementation = Msg.class))),
            @APIResponse(responseCode = "409", description = "Name not unique"),
            @APIResponse(responseCode = "500", description = "Internal error")
    })
    @Parameter(name = "id", description = "UUID of the policy")
    public Response validateName(@NotNull String jsonPolicyName, @QueryParam("id") UUID id) {
        if (!user.canReadPolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_VERIFY_POLICY)).build();
        }

        String policyName;
        try {
            policyName = objectMapper.readValue(jsonPolicyName, String.class);
        } catch (JsonProcessingException jpe) {
            return Response.status(400, "Invalid policy name received in body").build();
        }

        Policy policy = new Policy();
        policy.id = id;
        policy.name = policyName;

        Set<ConstraintViolation<Policy>> result = validator.validateProperty(policy, "name");

        if (result.size() > 0) {
            String error = String.join(
                    ";",
                    result.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet())
            );
            return Response.status(400).entity(new Msg(error)).build();
        }

        Response isNameValid = isNameUnique(policy);
        if (isNameValid != null) {
            return isNameValid;
        }

        return Response.status(200).entity(new Msg("Policy.name validated")).build();
    }


    @Operation(summary = "Retrieve a single policy for a customer by its id")
    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "200", description = "Policy found", content =
    @Content(schema = @Schema(implementation = Policy.class)))
    @APIResponse(responseCode = "404", description = "Policy not found")
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action", content = @Content(schema = @Schema(implementation = Msg.class)))
    @Parameter(name = "id", description = "UUID of the policy")
    public Response getPolicy(@PathParam("id") UUID policyId) {

        if (!user.canReadPolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg(MISSING_PERMISSIONS_TO_RETRIEVE_POLICIES)).build();
        }

        Policy policy = findPolicy(policyId);

        ResponseBuilder builder;
        if (policy == null) {
            builder = Response.status(Response.Status.NOT_FOUND);
        } else {
            builder = Response.ok(policy);
            EntityTag etag = new EntityTag(String.valueOf(policy.hashCode()));
            builder.header("ETag", etag);
        }

        return builder.build();
    }

    // workaround for returning generic types: https://github.com/swagger-api/swagger-core/issues/498#issuecomment-74510379
    // This class is used only for swagger return type
    private static class PagedResponseOfHistoryItem extends PagingUtils.PagedResponse<HistoryItem> {
        private PagedResponseOfHistoryItem(Page<HistoryItem> page) {
            super(page);
        }
    }

    @Operation(summary = "Retrieve the trigger history of a single policy")
    @APIResponse(responseCode = "200", description = "History could be retrieved",
            content = @Content(schema = @Schema(implementation = PagedResponseOfHistoryItem.class)),
            headers = @Header(name = "TotalCount", description = "Total number of items found",
                    schema = @Schema(type = SchemaType.INTEGER)))
    @APIResponse(responseCode = "400", description = "Bad parameters passed")
    @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
    @APIResponse(responseCode = "404", description = "Policy not found")
    @APIResponse(responseCode = "500", description = "Retrieval of History failed")
    @Parameters({
            @Parameter(
                    name = "offset",
                    in = ParameterIn.QUERY,
                    description = "Page number, starts 0, if not specified uses 0.",
                    schema = @Schema(type = SchemaType.INTEGER)
            ),
            @Parameter(
                    name = "limit",
                    in = ParameterIn.QUERY,
                    description = "Number of items per page, if not specified uses 50. Maximum value is 200.",
                    schema = @Schema(type = SchemaType.INTEGER)
            ),
            @Parameter(
                    name = "filter[name]",
                    in = ParameterIn.QUERY,
                    description = "Filtering history entries by the host name depending on the Filter operator used.",
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "filter:op[name]",
                    in = ParameterIn.QUERY,
                    description = "Operations used with the name filter",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "equal",
                                    "like",
                                    "not_equal"
                            },
                            defaultValue = "equal"
                    )
            ),
            @Parameter(
                    name = "filter[id]",
                    in = ParameterIn.QUERY,
                    description = "Filtering history entries by the id depending on the Filter operator used.",
                    schema = @Schema(type = SchemaType.STRING)
            ),
            @Parameter(
                    name = "filter:op[id]",
                    in = ParameterIn.QUERY,
                    description = "Operations used with the name filter",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "equal",
                                    "not_equal",
                                    "like"
                            },
                            defaultValue = "equal"
                    )
            ),
            @Parameter(
                    name = "sortColumn",
                    in = ParameterIn.QUERY,
                    description = "Column to sort the results by",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "hostName",
                                    CTIME_STRING
                            },
                            defaultValue = CTIME_STRING
                    )
            ),
            @Parameter(
                    name = "sortDirection",
                    in = ParameterIn.QUERY,
                    description = "Sort direction used",
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {
                                    "asc",
                                    "desc"
                            }
                    )
            ),
            @Parameter(name = "id", description = "UUID of the policy")
    })
    @GET
    @Path("/{id}/history/trigger")
    public Response getTriggerHistoryForPolicy(@PathParam("id") UUID policyId) {
        if (!user.canReadPolicies()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to retrieve the policy history")).build();
        }

        ResponseBuilder builder;

        Policy policy = findPolicy(policyId);

        if (policy == null) {
            builder = Response.status(Response.Status.NOT_FOUND);
        } else {

            Span span = Span.current();
            try {
                Pager pager = PagingUtils.extractPager(uriInfo);
                builder = buildHistoryResponse(policyId, pager);
            } catch (IllegalArgumentException iae) {
                span.setStatus(StatusCode.ERROR);
                builder = Response.status(400, iae.getMessage());
            } catch (Exception e) {
                span.setStatus(StatusCode.ERROR);
                span.recordException(e);
                String msg = "Retrieval of history failed with: " + e.getMessage();
                Log.warn(msg);
                builder = Response.serverError().entity(msg);
            }
        }
        return builder.build();
    }

    private ResponseBuilder buildHistoryResponse(UUID policyId, Pager pager) {
        List<HistoryItem> items;

        long totalCount = policiesHistoryRepository.count(user.getOrgId(), user.getHostGroupIds(), policyId, pager);

        if (totalCount > 0) {
            items = policiesHistoryRepository.find(user.getOrgId(), user.getHostGroupIds(), policyId, pager)
                    .stream().map(historyEntry ->
                            new HistoryItem(historyEntry.getCtime(), historyEntry.getHostId(), historyEntry.getHostName())
                    ).collect(Collectors.toList());
        } else {
            items = Collections.emptyList();
        }

        Page<HistoryItem> itemsPage = new Page<>(items, pager, totalCount);
        return PagingUtils.responseBuilder(itemsPage);
    }

    private Response isNameUnique(Policy policy) {
        Policy tmp = Policy.findByName(user.getOrgId(), policy.name);

        if (tmp != null && !tmp.id.equals(policy.id)) {
            return Response.status(409).entity(new Msg("Policy name is not unique")).build();
        }

        return null;
    }

    private Policy findPolicy(UUID policyId) {
        return Policy.findById(user.getOrgId(), policyId);
    }

    private void setLatestToNow() {
        orgIdLatestUpdateRepository.setLatestToNow(user.getOrgId());
    }
}
