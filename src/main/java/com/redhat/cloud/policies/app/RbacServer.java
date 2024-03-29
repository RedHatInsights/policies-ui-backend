/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.cloud.policies.app.auth.models.RbacRaw;

@Path("/api/rbac/v1")
@RegisterRestClient(configKey = "rbac")
public interface RbacServer {

    @GET
    @Path("/access/") // trailing slash is required by api
    @Consumes("application/json")
    @Produces("application/json")
    RbacRaw getRbacInfo(@QueryParam("application") String application,
                        @QueryParam("limit") int limit,
                        @HeaderParam("x-rh-identity") String rhIdentity

    );
}
