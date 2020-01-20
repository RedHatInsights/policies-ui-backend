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
package com.redhat.cloud.custompolicies.app;

import com.redhat.cloud.custompolicies.app.model.FullTrigger;
import com.redhat.cloud.custompolicies.app.model.Msg;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Interface to the backend engine
 * @author hrupp
 */
@Path("/hawkular/alerts/triggers/trigger")
@RegisterRestClient(configKey = "engine")
@RegisterProvider(value = EngineResponseExceptionMapper.class,
                  priority = 50)
public interface VerifyEngine {

  /**
   * Store and/or verify the FullTrigger, which is a wrapped Policy.
   * @param trigger Policy wrapped in a data structure, the engine expects
   * @param isDryRun If true, the engine will only verify the Policy, but not store it
   * @param customerId Id of the customer this policy belongs to.
   * @return A message with verification/store results.
   */
  @POST
  @Consumes("application/json")
  @Produces("application/json")
  Msg store(FullTrigger trigger,
            @QueryParam("dry") boolean isDryRun,
            @HeaderParam("Hawkular-Tenant" ) String customerId
  );
}
