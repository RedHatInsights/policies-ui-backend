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
package com.redhat.cloud.custompolicies.app.rest;

import com.redhat.cloud.custompolicies.app.NotificationSystem;
import com.redhat.cloud.custompolicies.app.auth.RhIdPrincipal;
import com.redhat.cloud.custompolicies.app.model.SettingsValues;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * @author hrupp
 */
@Path("/settings")
@Produces("application/json")
@Consumes("application/json")
@Timed
@RequestScoped
public class SettingsService {


  @SuppressWarnings("CdiInjectionPointsInspection")
  @Inject
  RhIdPrincipal user;

  @Inject
  @RestClient
  NotificationSystem notifications;

  @Operation(summary = "Save or update settings from the settings UI")
  @APIResponse(responseCode = "200", description = "Saving was ok")
  @POST
  @Path("/")
  @Transactional
  public Response saveSettings(@Valid SettingsValues values) {

    values.username = user.getName();
    values.accountId = user.getAccount();
    SettingsValues tmp = SettingsValues.findById(user.getName());
    if (tmp != null) {
      tmp.immediateEmail = values.immediateEmail;
      tmp.dailyEmail = values.dailyEmail;
    } else {
      values.persistAndFlush();
    }
    // Also send to notification service
    if (values.immediateEmail) {
      notifications.addNotification("custom-policies-instant-mail", user.getRawRhIdHeader());
    }
    else {
      notifications.removeNotification("custom-policies-instant-mail", user.getRawRhIdHeader());
    }
    if (values.dailyEmail) {
      notifications.addNotification("custom-policies-daily-mail", user.getRawRhIdHeader());
    }
    else {
      notifications.removeNotification("custom-policies-daily-mail", user.getRawRhIdHeader());
    }

    Response.ResponseBuilder builder = Response.ok();
    return builder.build();
  }


  @GET
  @Path("/")
  public Response getSettingsSchema() {

    String response ;
    try {
      URL resource = getClass().getClassLoader().getResource("settings-schema-template.json");
      response = new String(Files.readAllBytes(Paths.get(resource.toURI())));

      // Now we need to find the user record and populate the reply accordingly
      SettingsValues values = SettingsValues.findById(user.getName());
      if (values != null) {
        response = response.replace("%1", values.immediateEmail ? "true" : "false");
        response = response.replace("%2", values.dailyEmail ? "true" : "false");
      }
      else {
        // User's record does not yet exist, so use defaults
        response = response.replace("%1", "false");
        response = response.replace("%2", "false");
      }

    }
    catch (Exception e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
    Response.ResponseBuilder builder = Response.ok(response);
    EntityTag etag = new EntityTag(String.valueOf(response.hashCode()));
    builder.header("ETag",etag);
    return builder.build();
  }
}
