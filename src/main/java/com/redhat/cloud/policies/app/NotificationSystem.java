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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "notifications")
@Produces("application/json")
@Consumes("application/json")
public interface NotificationSystem {

    /*
    Using @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class) didn't work for the return value of `getUserPreferences`
    Test were always returning "null" on the values. Looks like it needs something else (or maybe is a bug?) to pick the snake_case
    for the Restclient.
    - Tried @Schema(name=instant_email")
     */
    class UserPreferences {
        public Boolean instant_email;
        public Boolean daily_email;
    }

    @GET
    @Path("/api/notifications/v1.0/user-config/notification-preference/{bundleName}/{applicationName}")
    UserPreferences getUserPreferences(
            @PathParam("bundleName") String bundleName,
            @PathParam("applicationName") String applicationName,
            @HeaderParam("x-rh-identity") String rhIdentity);

}
