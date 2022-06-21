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
package com.redhat.cloud.policies.app.health;

import com.redhat.cloud.policies.app.StuffHolder;
import io.quarkus.logging.Log;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provide a /status endpoint, that returns a 200 if all is cool
 * and a 500 with list of issues if not.
 */
@Path("/api/policies/v1.0/status")
@ApplicationScoped
public class StatusEndpoint {

    @GET
    @Produces("application/json")
    public Response getStatus() {
        Map<String, String> issues = StuffHolder.getInstance().getStatusInfo();

        if (!issues.isEmpty()) {
            Log.error("Status reports: " + makeReadable(issues));
            return Response.serverError().entity(issues).build();
        }
        return Response.ok().build();
    }

    private String makeReadable(Map<String, String> issues) {
        return issues.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("; "));
    }
}
