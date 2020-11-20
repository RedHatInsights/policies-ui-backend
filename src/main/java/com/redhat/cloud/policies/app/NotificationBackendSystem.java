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

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * @author hrupp
 */
@Path("/api/integrations/v1.0/")
@RegisterRestClient(configKey = "notifications-backend")
public interface NotificationBackendSystem {

  @PUT
  @Path("/endpoints/email/subscription/{event}")
  void addNotification(
          @PathParam("event") String event,
          @HeaderParam("x-rh-identity") String rhIdentity);

  @DELETE
  @Path("/endpoints/email/subscription/{event}")
  void removeNotification(
          @PathParam("event") String event,
          @HeaderParam("x-rh-identity") String rhIdentity);

}
