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
package com.redhat.cloud.policies.app.auth;

import com.redhat.cloud.policies.app.RbacServer;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.quarkus.cache.CacheResult;
import java.io.IOException;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * @author hrupp
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR +1)
public class RbacFilter implements ContainerRequestFilter {

  @Inject
  Tracer tracer;

  @Inject
  @RestClient
  RbacServer rbac;

  @Inject
  RhIdPrincipal user;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    RbacRaw result;
    try (Scope ignored = tracer.buildSpan("getRBac").startActive(true)){
      result = getRbacInfo(user.getRawRhIdHeader());
    } catch (Throwable e) {
      requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
      return;
    }

    user.setRbac(result.canReadAll(),result.canWriteAll());
    RhIdPrincipal userPrincipal = (RhIdPrincipal) requestContext.getSecurityContext().getUserPrincipal();
    userPrincipal.setRbac(result.canReadAll(),result.canWriteAll());
  }

  /*
   * This code is on purpose in a separate method and not inside the main
   * filter method so that the caching annotation can be applied. This speeds
   * up the user experience, as results are returned from the cache.
   * TTL of the cache items is defined in application.properties
   * quarkus.cache.caffeine.rbac-cache.expire-after-write
   */
  @CacheResult(cacheName = "rbac-cache")
  RbacRaw getRbacInfo(String xrhidHeader) throws Exception {
    RbacRaw result;
    result = rbac.getRbacInfo("policies", xrhidHeader);
    return result;
  }
}
