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

import com.redhat.cloud.policies.app.auth.RhIdPrincipal;
import com.redhat.cloud.policies.app.model.Fact;
import com.redhat.cloud.policies.app.model.Msg;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * @author hrupp
 */
@Path("/facts")
@Produces("application/json")
@Consumes("application/json")
@RequestScoped
public class FactService {

  @Inject
  RhIdPrincipal user;

  @GET
  @Operation(summary = "Retrieve a list of fact (keys) along with their data types")
  @APIResponse(responseCode = "200", description = "List of facts", content =
               @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = Fact.class)))
  public Response listFacts() {

    if (!user.canReadAll()) {
      return Response.status(Response.Status.FORBIDDEN).entity(new Msg("Missing permissions to retrieve facts")).build();
    }

    Response.ResponseBuilder builder = Response.ok();
    builder.entity(Fact.getFacts());
    return builder.build();
  }

}
