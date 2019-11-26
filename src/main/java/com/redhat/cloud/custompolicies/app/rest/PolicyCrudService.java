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
import com.redhat.cloud.custompolicies.app.model.Msg;
import com.redhat.cloud.custompolicies.app.model.Policy;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hibernate.exception.ConstraintViolationException;

/**
 * @author hrupp
 */
@Path("/api/v1/policies")
@Produces("application/json")
@Consumes("application/json")
@Timed
public class PolicyCrudService {

  private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  private static final String ALLOWED_ORIGINS = "*"; // TODO needs to change
  @Inject
  @RestClient
  VerifyEngine engine;

  @Context
  UriInfo uriInfo;

  @GET
  @Path("/{customer}")
  public Response getPoliciesForCustomer(@PathParam("customer") String customer) {

    ResponseBuilder builder ;
    List<Policy> policies = Policy.listPoliciesForCustomer(customer);

    if (policies.isEmpty()) {
      builder = Response.status(Response.Status.NOT_FOUND);
    } else {
      builder = Response.ok().entity(policies);
      EntityTag etag = new EntityTag(String.valueOf(policies.hashCode()));
      builder.header("ETag",etag);
    }
    builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGINS);

    return builder.build();
  }

  @POST
  @Path("/{customer}")
  @Transactional
  public Response storePolicy(@PathParam("customer") String customer, @Valid Policy policy) {

    if (policy==null) {
      return Response.status(500, "No policy passed").header(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGINS).build();
    }

    policy.id = null;

    Msg msg = new Msg(policy.conditions);
    try {
      msg = engine.verify(msg);
    }
    catch (Exception e) {
      System.err.println("Rule verification failed: " + e.getMessage() + " -> " + msg);
      return Response.status(400,e.getMessage()).entity(msg).header(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGINS).build();
    }

    // Basic validation was successful, so try to persist.
    // This may still fail du to unique name violation, so
    // we need to check for that.
    Long id;
    try {
      id = policy.store(customer, policy);
    } catch (Throwable t) {
      if (t instanceof PersistenceException &&  t.getCause() instanceof ConstraintViolationException) {
        return Response.status(409, t.getMessage()).entity(new Msg("Constraint violation")).header(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGINS).build();
      }
      else {
        t.printStackTrace();
        return Response.status(500, t.getMessage()).header(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGINS).build();
      }
    }

    // Policy is persisted. Return its location.
    URI location =
        UriBuilder.fromMethod(PolicyCrudService.class, "getPolicy").build(customer, id);
    ResponseBuilder builder = Response.created(location);
    return builder.build();

  }

  @GET
  @Path("/{customer}/policy/{id}")
  public Response getPolicy(@PathParam("customer") String customerId, @PathParam("id") Long policyId) {
    Policy policy = Policy.findById(customerId, policyId);

    ResponseBuilder builder ;
    if (policy==null) {
      builder = Response.status(Response.Status.NOT_FOUND);
    } else {
      builder = Response.ok(policy);
      EntityTag etag = new EntityTag(String.valueOf(policy.hashCode()));
      builder.header("ETag",etag);
    }
    builder.header(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGINS);
    return builder.build();
  }

}
