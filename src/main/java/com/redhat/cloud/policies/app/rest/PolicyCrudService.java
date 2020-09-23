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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cloud.policies.app.PolicyEngine;
import com.redhat.cloud.policies.app.TokenHolder;
import com.redhat.cloud.policies.app.auth.RhIdPrincipal;
import com.redhat.cloud.policies.app.model.UUIDHelperBean;
import com.redhat.cloud.policies.app.model.engine.FullTrigger;
import com.redhat.cloud.policies.app.model.Msg;
import com.redhat.cloud.policies.app.model.Policy;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.JsonString;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.redhat.cloud.policies.app.model.engine.HistoryItem;
import com.redhat.cloud.policies.app.model.engine.Trigger;
import com.redhat.cloud.policies.app.model.pager.Page;
import com.redhat.cloud.policies.app.model.pager.Pager;
import com.redhat.cloud.policies.app.rest.utils.PagingUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.quarkus.panache.common.Sort;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
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

import static com.redhat.cloud.policies.app.model.filter.PolicyHistoryTagFilterHelper.getTagsFilterFromPager;
import static java.lang.Integer.min;

/**
 * @author hrupp
 */
@Path("/policies")
@Produces("application/json")
@Consumes("application/json")
@SimplyTimed(absolute = true, name="PolicySvc")
@RequestScoped
public class PolicyCrudService {

  private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
  private static final ObjectMapper OM = new ObjectMapper();

  @Inject
  @RestClient
  PolicyEngine engine;

  @Context
  UriInfo uriInfo;

  @SuppressWarnings("CdiInjectionPointsInspection")
  @Inject
  RhIdPrincipal user;

  @Inject
  EntityManager entityManager;

  @Inject
  UUIDHelperBean uuidHelper;

  @Inject
  Validator validator;

  @Inject
  Tracer tracer;

  // workaround for returning generic types: https://github.com/swagger-api/swagger-core/issues/498#issuecomment-74510379
  // This class is used only for swagger return type
  private static class PagedResponseOfPolicy extends PagingUtils.PagedResponse<Policy> {
    @SuppressWarnings("unused")
    private List<Policy> data;
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
                  description = "Number of items per page, if not specified uses 50. " + Pager.NO_LIMIT + " can be used to specify an unlimited page, when specified it ignores the offset" ,
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
                                  "mtime"
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
                  name="filter:op[name]",
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
                  name="filter:op[description]",
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
              schema = @Schema(type = SchemaType.STRING, defaultValue = "true", enumeration = {"true","false"})
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

    if (!user.canReadAll()) {
      return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to retrieve policies")).build();
    }

    Page<Policy> page;
    try {
      Pager pager = PagingUtils.extractPager(uriInfo);
      page = Policy.pagePoliciesForCustomer(entityManager, user.getAccount(), pager);

      // Get lastTriggeredTime and add it to the policies
      String idsToFetch = page.stream().map(p -> p.id.toString()).collect(Collectors.joining(","));
      try {
        List<Trigger> list = engine.findTriggersById(idsToFetch,   user.getAccount());

        // Put the lastTriggered time (= creation time ) of the alert in a triggerId-alert map
        Map<String, Long> idTimeMap = new HashMap<>(page.size());
        for (Trigger trigger : list) {
          long lastTriggered = getLastTriggeredFromTriggerLifecycle(trigger);
          idTimeMap.put(trigger.id,lastTriggered);
        }
        // Now loop over the map and transfer the times to the policies
        page.forEach(p -> {
          if (idTimeMap.containsKey(p.id.toString())) {
            long lastTriggerTime = idTimeMap.get(p.id.toString());
            p.setLastTriggered(lastTriggerTime);
          }
        });
      } catch (ProcessingException| WebApplicationException pe ) {
        log.warning("Getting lastTrigger time failed: " + pe.getMessage());
      }
    } catch (IllegalArgumentException iae) {
      return Response.status(400,iae.getLocalizedMessage()).build();
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
                  name="filter:op[name]",
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
                  name="filter:op[description]",
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
                  schema = @Schema(type = SchemaType.STRING, defaultValue = "true", enumeration = {"true","false"})
          ),
  })
  @APIResponse(responseCode = "400", description = "Bad parameter for sorting was passed")
  @APIResponse(responseCode = "404", description = "No policies found for customer")
  @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
  @APIResponse(responseCode = "200", description = "PolicyIds found", content =
                 @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = UUID.class)))
  public Response getPolicyIdsForCustomer() {

    if (!user.canReadAll()) {
      return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to retrieve policies")).build();
    }

    List<UUID> uuids;
    try {
      Pager pager = PagingUtils.extractPager(uriInfo);
      uuids = Policy.getPolicyIdsForCustomer(entityManager, user.getAccount(), pager);

    } catch (IllegalArgumentException iae) {
      return Response.status(400,iae.getLocalizedMessage()).build();
    }

    return Response.ok(uuids).build();
  }


  @Operation(summary = "Validate (and possibly persist) a passed policy for the given account")
  @Parameter(name = "alsoStore",
             description = "If passed and set to true, the passed policy is also persisted (if it is valid)")
  @APIResponses({
      @APIResponse(responseCode = "500", description = "Internal error"),
      @APIResponse(responseCode = "400", description = "No policy provided or policy validation failed",
                   content = @Content(schema = @Schema(implementation = Msg.class ))),
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
  public Response storePolicy(@QueryParam ("alsoStore") boolean alsoStore, @NotNull @Valid Policy policy) {

    if (!user.canReadAll()) {
      return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to verify policy")).build();
    }

    // We use the indirection, so that for testing we can produce known UUIDs
    policy.id = uuidHelper.getUUID();
    policy.customerid = user.getAccount();

    Response invalidNameResponse = isNameUnique(policy);
    if (invalidNameResponse != null) {
      return invalidNameResponse;
    }

    try {
      FullTrigger trigger = new FullTrigger(policy,true);
      engine.storeTrigger(trigger, true, user.getAccount());
    } catch (Exception e) {
      return Response.status(400,e.getMessage()).entity(getEngineExceptionMsg(e)).build();
    }

    if (!alsoStore) {
      return Response.status(200).entity(new Msg("Policy validated")).build();
    }

    if (!user.canWriteAll()) {
      return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to store policy")).build();
    }

    // Basic validation was successful, so try to persist.
    // This may still fail du to unique name violation, so
    // we need to check for that.
    UUID id;
    try {
      FullTrigger trigger = new FullTrigger(policy);
      try {
        engine.storeTrigger(trigger, false, user.getAccount());
        id = policy.store(user.getAccount(), policy);
      } catch (Exception e) {
        Msg engineExceptionMsg = getEngineExceptionMsg(e);
        log.warning("Storing policy in engine failed: " + engineExceptionMsg.msg);
        return Response.status(400,e.getMessage()).entity(engineExceptionMsg).build();
      }
    } catch (Throwable t) {
      return getResponseSavingPolicyThrowable(t);
    }

    // Policy is persisted. Return its location.
    URI location =
        UriBuilder.fromResource(PolicyCrudService.class).path(PolicyCrudService.class, "getPolicy").build(id);
    ResponseBuilder builder = Response.created(location).entity(policy);
    return builder.build();

  }

  private Response getResponseSavingPolicyThrowable(Throwable t) {
    if (t instanceof PersistenceException && t.getCause() instanceof ConstraintViolationException) {
      return Response.status(409, t.getMessage()).entity(new Msg("Constraint violation")).build();
    } else {
      t.printStackTrace();
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

    if (!user.canWriteAll()) {
       return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to delete policy")).build();
    }
    Policy policy = Policy.findById(user.getAccount(), policyId);

    ResponseBuilder builder = Response.ok();
    if (policy==null) {
      builder = Response.status(Response.Status.NOT_FOUND);
    } else {
      boolean deletedOnEngine = false;
      try {
        engine.deleteTrigger(policy.id, user.getAccount());
        deletedOnEngine = true;
      } catch (NotFoundException nfe) {
        // Engine does not have it - we can delete anyway
        deletedOnEngine = true;
      } catch (Exception e) {
        log.warning("Deletion on engine failed because of " + e.getMessage());
        builder = Response.serverError().entity(new Msg(e.getMessage()));
      }
      if (deletedOnEngine) {
        policy.delete(policy);
        builder = Response.ok(policy);
      }
    }

    return builder.build();

  }

  @Operation(summary = "Delete policies for a customer by the ids passed in the body. Result will be a list of deleted UUIDs")
  @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
  @APIResponse(responseCode = "200", description = "Policies deleted",
      content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = UUID.class)))
  @DELETE
  @Path("/ids")
  @Transactional
  public Response deletePolicies(List<UUID> uuids) {

    if (!user.canWriteAll()) {
       return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to delete policy")).build();
    }

    List<UUID> deleted = new ArrayList<>(uuids.size());

    for (UUID uuid : uuids) {
      Policy policy = Policy.findById(user.getAccount(), uuid);
      if (policy == null ) {
        // Nothing to do for us
        deleted.add(uuid);
      } else {
        boolean deletedOnEngine = false;
        try {
          engine.deleteTrigger(policy.id, user.getAccount());
          deletedOnEngine = true;
        } catch (NotFoundException nfe) {
          // Engine does not have it - we can delete anyway
          deletedOnEngine = true;
        } catch (Exception e) {
          log.warning("Deletion on engine failed because of " + e.getMessage());
        }
        if (deletedOnEngine) {
          policy.delete();
          deleted.add(uuid);
        }
      }
    }

    return Response.ok(deleted).build();
  }

  @Operation(summary = "Enable/disable a policy")
  @Parameter(name = "id", description = "ID of the Policy")
  @Parameter(name = "enabled",
      schema =  @Schema(type = SchemaType.BOOLEAN, defaultValue = "false"),
      description = "Should the policy be enabled (true) or disabled (false, default)")
  @APIResponse(responseCode = "200", description = "Policy updated")
  @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action")
  @APIResponse(responseCode = "404", description = "Policy not found")
  @APIResponse(responseCode = "500", description = "Updating failed")
  @POST
  @Path("/{id:[0-9a-fA-F-]+}/enabled")
  @Transactional
  public Response setEnabledStateForPolicy(@PathParam("id") UUID policyId, @QueryParam("enabled") boolean shouldBeEnabled) {
    if (!user.canWriteAll()) {
       return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to update policy")).build();
     }

    Policy storedPolicy = Policy.findById(user.getAccount(), policyId);

    ResponseBuilder builder ;
    if (storedPolicy==null) {
      builder = Response.status(404, "Original policy not found");}
    else {
      try {
        if (shouldBeEnabled) {
          engine.enableTrigger(storedPolicy.id, user.getAccount());
        } else {
          engine.disableTrigger(storedPolicy.id, user.getAccount());
        }
        storedPolicy.isEnabled = shouldBeEnabled;
        storedPolicy.setMtimeToNow();
        storedPolicy.persist();
        builder = Response.ok();
      } catch (NotFoundException nfe) {
        builder = Response.status(404, "Policy not found in engine");
        log.warning("Enable/Disable failed, policy [" + storedPolicy.id + "] not found in engine");
      }
      catch (Exception e ) {
        builder = Response.status(500, "Update failed: " + e.getMessage());
      }
    }
    return builder.build();
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
  public Response setEnabledStateForPolicies(@QueryParam("enabled") boolean shouldBeEnabled, List<UUID> uuids) {
    if (!user.canWriteAll()) {
        return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to update policy")).build();
    }
    List<UUID> changed = new ArrayList<>(uuids.size());
    try {
      for (UUID uuid : uuids) {
        Policy storedPolicy = Policy.findById(user.getAccount(), uuid);
        boolean wasChanged = false;
        if (storedPolicy != null) {
          try {
            if (shouldBeEnabled) {
              engine.enableTrigger(storedPolicy.id, user.getAccount());
            } else {
              engine.disableTrigger(storedPolicy.id, user.getAccount());
            }
            wasChanged = true;
          } catch (Exception e) {
            log.warning("Changing state in engine failed: " + e.getMessage());
          }
          if (wasChanged) {
            storedPolicy.isEnabled = shouldBeEnabled;
            storedPolicy.setMtimeToNow();
            storedPolicy.persist();
            changed.add(uuid);
          }
        }
      }
      return Response.ok(changed).build();
    } catch (Throwable e) {
      e.printStackTrace();  // TODO: Customise this generated block
      return Response.serverError().build();
    }
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
  public Response updatePolicy(@QueryParam ("dry") boolean dryRun, @PathParam("policyId") UUID policyId,
                               @NotNull @Valid Policy policy) {

    if (!user.canWriteAll()) {
       return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to update policy")).build();
     }

    Policy storedPolicy = Policy.findById(user.getAccount(), policyId);

    ResponseBuilder builder ;
    if (storedPolicy==null) {
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
          FullTrigger trigger = new FullTrigger(policy);
          engine.updateTrigger(policy.id, trigger, true, user.getAccount());
        } catch (Exception e) {
          return Response.status(400,e.getMessage()).entity(getEngineExceptionMsg(e)).build();
        }

        if (dryRun) {
          return Response.status(200).entity(new Msg("Policy validated")).build();
        }

        // All is good, we can now do the real work
        // The engine requires that we update existing structures,
        // so we need to first poll from it.
        try {
          FullTrigger existingTrigger;
          try {
            existingTrigger = engine.fetchTrigger(storedPolicy.id, user.getAccount());
          } catch (Exception e) {
            return Response.status(400, e.getMessage()).entity(getEngineExceptionMsg(e)).build();
          }

          storedPolicy.populateFrom(policy);
          storedPolicy.customerid = user.getAccount();
          storedPolicy.setMtimeToNow();

          existingTrigger.updateFromPolicy(storedPolicy);
          try {
            engine.updateTrigger(storedPolicy.id, existingTrigger, false, user.getAccount());
            Policy.persist(storedPolicy);
          } catch (Exception e) {
            return Response.status(400, e.getMessage()).entity(getEngineExceptionMsg(e)).build();
          }
          Policy.persist(storedPolicy);
        } catch (Throwable t) {
          return getResponseSavingPolicyThrowable(t);
        }

        builder = Response.ok(storedPolicy);
      }
    }

    return builder.build();
  }

  @Operation(summary = "Validates a Policy condition")
  @POST
  @Path("/validate")
  @APIResponses({
          @APIResponse(responseCode = "200", description = "Condition validated", content = @Content( schema = @Schema (implementation = Msg.class))),
          @APIResponse(responseCode = "400", description = "No policy provided or condition not valid", content = @Content( schema = @Schema (implementation = Msg.class))),
          @APIResponse(responseCode = "500", description = "Internal error")
  })
  public Response validateCondition(@Valid @NotNull Policy policy) {

    if (!user.canReadAll()) {
      return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to verify policy")).build();
    }

    policy.customerid = user.getAccount();

    try {
      FullTrigger trigger = new FullTrigger(policy, policy.id == null);
      if (policy.id == null) {
        engine.storeTrigger(trigger, true, user.getAccount());
      } else {
        engine.updateTrigger(policy.id, trigger, true, user.getAccount());
      }
    } catch (Exception e) {
      return Response.status(400,e.getMessage()).entity(getEngineExceptionMsg(e)).build();
    }

    return Response.status(200).entity(new Msg("Policy.condition validated")).build();

  }

  @Operation(summary = "Validates the Policy.name and verifies if it is unique.")
  @POST
  @Path("/validate-name")
  @RequestBody(content = { @Content( schema = @Schema( type = SchemaType.STRING )) })
  @APIResponses({
          @APIResponse(responseCode = "200", description = "Name validated", content = @Content( schema = @Schema (implementation = Msg.class))),
          @APIResponse(responseCode = "400", description = "Policy validation failed", content = @Content( schema = @Schema (implementation = Msg.class))),
          @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action", content = @Content( schema = @Schema (implementation = Msg.class))),
          @APIResponse(responseCode = "409", description = "Name not unique"),
          @APIResponse(responseCode = "500", description = "Internal error")
  })
  @Parameter(name = "id", description = "UUID of the policy")
  public Response validateName(@NotNull JsonString policyName, @QueryParam("id") UUID id) {
    if (!user.canReadAll()) {
      return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to verify policy")).build();
    }

    Policy policy = new Policy();
    policy.id = id;
    policy.name = policyName.getString();

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
  @APIResponse(responseCode = "403", description = "Individual permissions missing to complete action", content = @Content( schema = @Schema (implementation = Msg.class)))
  @Parameter(name = "id", description = "UUID of the policy")
  public Response getPolicy(@PathParam("id") UUID policyId) {

    if (!user.canReadAll()) {
      return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to retrieve policies")).build();
    }

    Policy policy = Policy.findById(user.getAccount(), policyId);

    ResponseBuilder builder ;
    if (policy==null) {
      builder = Response.status(Response.Status.NOT_FOUND);
    } else {
      try {
        List<Trigger> triggers = engine.findTriggersById(policyId.toString(), user.getAccount());
        if (triggers != null && !triggers.isEmpty()) {
          long lastTriggered = getLastTriggeredFromTriggerLifecycle(triggers.get(0));
          policy.setLastTriggered(lastTriggered);
        }
      } catch (Exception e) {
        log.warning("Retrieving lastTrigger time for [ " + policyId + "] failed: " + e.getMessage());
      }
      builder = Response.ok(policy);
      EntityTag etag = new EntityTag(String.valueOf(policy.hashCode()));
      builder.header("ETag",etag);
    }

    return builder.build();
  }

  // workaround for returning generic types: https://github.com/swagger-api/swagger-core/issues/498#issuecomment-74510379
    // This class is used only for swagger return type
    private static class PagedResponseOfHistoryItem extends PagingUtils.PagedResponse<HistoryItem> {
      @SuppressWarnings("unused")
      private List<HistoryItem> data;
      private PagedResponseOfHistoryItem(Page<HistoryItem> page) {
        super(page);
      }
    }

  @Operation(summary = "Retrieve the trigger history of a single policy")
  @APIResponse(responseCode = "200", description = "History could be retrieved",
      content = @Content (schema = @Schema(implementation = PagedResponseOfHistoryItem.class)),
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
              name="filter:op[name]",
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
              name="filter:op[id]",
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
                              "id",
                              "ctime"
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
      @Parameter(name = "id", description = "UUID of the policy")
  })
  @GET
  @Path("/{id}/history/trigger")
  public Response getTriggerHistoryForPolicy(@PathParam("id") UUID policyId) {
    if (!user.canReadAll()) {
       return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to retrieve the policy history")).build();
     }

    ResponseBuilder builder ;

    Policy policy = Policy.findById(user.getAccount(), policyId);
     if (policy==null) {
       builder = Response.status(Response.Status.NOT_FOUND);
     } else {

       try {
         Pager pager = PagingUtils.extractPager(uriInfo);

         int limit = pager.getLimit();
         // We dont allow unlimited or values > 200
         limit = limit == Pager.NO_LIMIT ? 50 :  min(limit,200);
         int pageNum = pager.getOffset() / limit;

         // Once Quarkus supports MP rest client 2.0 we can use
         // String[] here and use some special query-param style to
         // support this
         String sort1=null;
         String sort2 = null;
         String order1 = null;
         String order2= null;

         if (pager.getSort()!= null) {
           List<Sort.Column> columns = pager.getSort().getColumns();
           if (columns.size() > 0) {
             sort1 = getSortFromPageColumn(columns.get(0));
             order1 = getOrderFromPageColumn(columns.get(0));
           }
           if (columns.size() > 1) {
             sort2 = getSortFromPageColumn(columns.get(1));
             order2 = getOrderFromPageColumn(columns.get(1));
           }
         }

         String tagQuery = getTagsFilterFromPager(pager);

         long totalCount;
         Response response = null;
         String alerts;
         Span span = tracer.buildSpan("fetchLastTriggeredFromEngine").asChildOf(tracer.activeSpan()).start();
         Scope scope = tracer.scopeManager().activate(span,false);
         try {
           response = engine.findLastTriggered(policyId.toString(),
               true, // thin
               pageNum, limit,
               sort1, sort2, order1, order2, // See Comment in engine def
               tagQuery,
               user.getAccount());
           String countHeader = response.getHeaderString("X-Total-Count");
           totalCount = Long.parseLong(countHeader);
           alerts = response.readEntity(String.class);
         }
         catch (Exception e) {
           String msg = "Retrieval of history from engine failed with: " + e.getMessage();
           span.log(msg);
           span.setTag("error",true);
           log.warning(msg);
           return Response.serverError().entity(msg).build();
         }
         finally{
           if (response!=null) {
             response.close();
           }
           if (span!=null) {
             span.finish();
           }
           if (scope!=null) {
             scope.close();
           }
         }

         List<HistoryItem> items = new ArrayList<>();

         // The engine may not have returned data to us
         if (totalCount > 0) {
           span = tracer.buildSpan("extractTriggerHistory").asChildOf(tracer.activeSpan()).start();
           try (Scope ignored = tracer.scopeManager().activate(span,true)) {
             parseHistoryFromEngine(alerts, items);
           }
         }
         Page<HistoryItem> itemsPage = new Page<>(items,pager,totalCount);
         builder = PagingUtils.responseBuilder(itemsPage);
       } catch (IllegalArgumentException iae) {
         tracer.activeSpan().setTag("error",true);
         builder = Response.status(400,iae.getMessage());
       } catch (Exception e) {
         tracer.activeSpan().setTag("error",true);
         String msg = "Retrieval of history failed with: " + e.getMessage();
         log.warning(msg);
         builder = Response.serverError().entity(msg);
       }
     }
     return builder.build();
  }

  public static void parseHistoryFromEngine(String alerts, List<HistoryItem> items) throws JsonProcessingException {

    List<Map<String,Object>> data = OM.readValue(alerts, new TypeReference<>() {});
    for (Map<String,Object> value :data) {
      Long ctime = (Long) value.get("ctime");
      List<Map<String, String>> tags = (List<Map<String, String>>) value.get("tags");
      String inventory_id = null;
      String name = null;
      for (Map<String,String> tag: tags) {
        if (tag.containsKey("inventory_id")) {
          inventory_id = tag.get("inventory_id");
        }
        if (tag.containsKey("display_name")) {
          name = tag.get("display_name");
        }
      }
      if (name==null||inventory_id==null) {
        throw new IllegalStateException("Name or inventory_id not found in tags");
      }
      HistoryItem hi = new HistoryItem(ctime, inventory_id, name);
      items.add(hi);
    }
  }

  private String getSortFromPageColumn(Sort.Column column) {
    String sort = column.getName();
    switch (sort) {
      case "id":
        sort = "tags.inventory_id"; break;
      case "name":
        sort = "tags.display_name"; break;
      default:
        sort = "ctime";
    }
    return sort;
  }

  private String getOrderFromPageColumn(Sort.Column column) {
    String order = column.getDirection().equals(Sort.Direction.Ascending) ? "asc" : "desc";
    return order;
  }

  /*
   Return last triggered time from Trigger.lifecycle if it exists
   Return 0 otherwise.
   We need to go through the lifecycle list, as this is sorted in
   ascending order with potentially other events in-between.
   */
  private long getLastTriggeredFromTriggerLifecycle(Trigger trigger) {

    long tTime = 0;

    for (Map<String, Object> map : trigger.lifecycle) {
      if (map.containsKey("status") && map.get("status").equals("ALERT_GENERATE")) {
        BigDecimal aTime = (BigDecimal) map.get("stime");
        long tse =  aTime.longValue();
        if (tse > tTime) {
          tTime = tse;
      }
    }
    }
    return tTime;
  }

  private Response isNameUnique(Policy policy) {
    Policy tmp = Policy.findByName(user.getAccount(), policy.name);

    if (tmp != null && !tmp.id.equals(policy.id)) {
      return Response.status(409).entity(new Msg("Policy name is not unique")).build();
    }

    return null;
  }

  @Path("/sync")
  @POST
  @Transactional
  public Response syncToEngine(@QueryParam("token") String token) {

    boolean validToken = TokenHolder.getInstance().compareToken(token);
    if (!validToken) {
      return Response.status(Response.Status.FORBIDDEN).entity("You don't have permission for this").build();
    }

    final int[] count = {0};
    try (Stream<Policy> policies = Policy.streamAll()) {
      policies.forEach(p -> {
        FullTrigger fullTrigger;
        try {
          fullTrigger = engine.fetchTrigger(p.id, p.customerid);
        }
        catch (NotFoundException nfe) {
          fullTrigger=null;
        }
        if (fullTrigger == null) { // Engine does not have the trigger
          log.info("Trigger " + p.id + " not found, syncing");
          FullTrigger ft = new FullTrigger(p);
          engine.storeTrigger(ft, false, p.customerid);
          log.info("   done");
          count[0]++;
        }
        else {
          log.info("Trigger " + p.id + " already in engine, skipping");
        }
      });
    }
    log.info("Stored " + count[0] + " triggers");
    return Response.ok().build();
  }

}
