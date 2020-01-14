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
package com.redhat.cloud.custompolicies.app.rest;

import com.redhat.cloud.custompolicies.app.VerifyEngine;
import com.redhat.cloud.custompolicies.app.auth.RhIdPrincipal;
import com.redhat.cloud.custompolicies.app.model.FullTrigger;
import com.redhat.cloud.custompolicies.app.model.Msg;
import com.redhat.cloud.custompolicies.app.model.Policy;
import java.net.URI;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.cloud.custompolicies.app.model.pager.Page;
import com.redhat.cloud.custompolicies.app.model.pager.Pager;
import com.redhat.cloud.custompolicies.app.rest.utils.PagingUtils;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hibernate.exception.ConstraintViolationException;

/**
 * @author hrupp
 */
@Path("/policies")
@Produces("application/json")
@Consumes("application/json")
@Timed
@RequestScoped
public class PolicyCrudService {

  @Inject
  @RestClient
  VerifyEngine engine;

  @Context
  UriInfo uriInfo;

  @Inject
  RhIdPrincipal user;

  @Operation(summary = "Return all policies for a given account")
  @GET
  @Path("/")
  @Parameters({
          @Parameter(
                  name = "page",
                  in = ParameterIn.QUERY,
                  description = "Page number, starts 0, if not specified uses 0."
          ),
          @Parameter(
                  name = "pageSize",
                  in = ParameterIn.QUERY,
                  description = "Number of items per page, if not specified uses 10."
          )
  })
  @APIResponse(responseCode = "404", description = "No policies found for customer")
  @APIResponse(responseCode = "200", description = "Policies found", content =
                 @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = Policy.class)),
                 headers = @Header(name = "TotalCount", description = "Total number of items found"))
  public Response getPoliciesForCustomer() {

    Pager pager = PagingUtils.extractPager(uriInfo);
    Page<Policy> page = Policy.pagePoliciesForCustomer(user.getAccount(), pager);

    return PagingUtils.responseBuilder(page).build();
  }

  @Operation(summary = "Persist a passed policy for the given account")
  @APIResponses({
      @APIResponse(responseCode = "500", description = "No policy provided or internal error"),
      @APIResponse(responseCode = "400", description = "Policy validation failed"),
      @APIResponse(responseCode = "409", description = "Persisting failed"),
      @APIResponse(responseCode = "201", description = "Policy persisted"),
      @APIResponse(responseCode = "200", description = "Policy validated")
                })
  @POST
  @Path("/")
  @Transactional
  public Response storePolicy(@QueryParam ("alsoStore") boolean alsoStore, @Valid Policy policy) {
    if (policy==null) {
      return Response.status(500, "No policy passed").build();
    }

    policy.id = null;
    policy.customerid = user.getAccount();

    Msg msg ;
    try {
      FullTrigger trigger = new FullTrigger(policy);
      engine.verify(trigger, true, user.getAccount());
    }
    catch (Exception e) {
      msg = new Msg(e.getMessage());
      return Response.status(400,e.getMessage()).entity(msg).build();
    }

    if (!alsoStore) {
      Policy tmp = Policy.findByName(user.getAccount(), policy.name);
      if (tmp != null) {
        return Response.status(409).entity(new Msg("Policy name is not unique")).build();
      }
      else {
        return Response.status(200).entity(new Msg("Policy validated")).build();
      }
    }

    // Basic validation was successful, so try to persist.
    // This may still fail du to unique name violation, so
    // we need to check for that.
    Long id;
    try {
      id = policy.store(user.getAccount(), policy);
    } catch (Throwable t) {
      if (t instanceof PersistenceException &&  t.getCause() instanceof ConstraintViolationException) {
        return Response.status(409, t.getMessage()).entity(new Msg("Constraint violation")).build();
      }
      else {
        t.printStackTrace();
        return Response.status(500, t.getMessage()).build();
      }
    }

    // Policy is persisted. Return its location.
    URI location =
        UriBuilder.fromMethod(PolicyCrudService.class, "getPolicy").build(id);
    ResponseBuilder builder = Response.created(location);
    return builder.build();

  }

  @Operation(summary = "Delete a single policy for a customer by its id")
  @DELETE
  @Path("/{id}")
  @APIResponse(responseCode = "200", description = "Policy deleted")
  @APIResponse(responseCode = "400", description = "Deletion failed")
  @Transactional
  public Response deletePolicy(@PathParam("id") Long policyId) {

    Policy policy = Policy.findById(user.getAccount(), policyId);

    ResponseBuilder builder ;
    if (policy==null) {
      builder = Response.status(Response.Status.BAD_REQUEST);
    } else {
      policy.delete(policy);
      builder = Response.ok(policy);
    }

    return builder.build();


  }

  @Operation(summary = "Retrieve a single policy for a customer by its id")
  @GET
  @Path("/{id}")
  @APIResponse(responseCode = "200", description = "Policy found", content =
                 @Content(schema = @Schema(implementation = Policy.class)))
  @APIResponse(responseCode = "404", description = "Policy not found")
  public Response getPolicy(@PathParam("id") Long policyId) {
    Policy policy = Policy.findById(user.getAccount(), policyId);

    ResponseBuilder builder ;
    if (policy==null) {
      builder = Response.status(Response.Status.NOT_FOUND);
    } else {
      builder = Response.ok(policy);
      EntityTag etag = new EntityTag(String.valueOf(policy.hashCode()));
      builder.header("ETag",etag);
    }

    return builder.build();
  }

}
