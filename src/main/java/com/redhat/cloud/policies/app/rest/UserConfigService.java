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
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.logging.Logger;

@Path("/api/policies/v1.0/user-config")
@Produces("application/json")
@Consumes("application/json")
@SimplyTimed(absolute = true, name = "UserConfigSvc")
@RequestScoped
public class UserConfigService {

    private final Logger log = Logger.getLogger(this.getClass().getSimpleName());

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
            log.warning("Retrieving settings failed: " + e.getMessage());
            throw new ServerErrorException(Response.serverError().entity(new Msg(e.getMessage())).build());
        }
    }
}
