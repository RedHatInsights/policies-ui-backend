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
package com.redhat.cloud.policies.app.rest;

import com.redhat.cloud.policies.app.EnvironmentInfo;
import com.redhat.cloud.policies.app.NotificationSystem;
import com.redhat.cloud.policies.app.NotificationSystem.UserPreferences;
import com.redhat.cloud.policies.app.auth.RhIdPrincipal;
import com.redhat.cloud.policies.app.model.Msg;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;

import io.micrometer.core.annotation.Timed;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/policies/v1.0/user-config")
@Produces("application/json")
@Consumes("application/json")
@Timed("UserConfigSvc")
@RequestScoped
public class UserConfigService {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    RhIdPrincipal user;

    @Inject
    @RestClient
    NotificationSystem notifications;

    @ConfigProperty(name = "notifications.bundle", defaultValue = "rhel")
    String bundle;

    @ConfigProperty(name = "notifications.application", defaultValue = "policies")
    String application;

    @Inject
    EnvironmentInfo environmentInfo;

    @GET
    @Path("/preferences")
    public UserPreferences getSettingsSchema() {

        if (environmentInfo.isFedramp()) {
            throw new NotFoundException();
        }

        if (!user.canReadPolicies()) {
            throw new ForbiddenException("You don't have permission to read settings");
        }

        try {
            return notifications.getUserPreferences(bundle, application, user.getRawRhIdHeader());
        } catch (Exception e) {
            Log.warn("Retrieving settings failed: " + e.getMessage());
            throw new ServerErrorException(Response.serverError().entity(new Msg(e.getMessage())).build());
        }
    }
}
