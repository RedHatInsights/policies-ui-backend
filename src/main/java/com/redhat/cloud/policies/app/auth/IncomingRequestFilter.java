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
package com.redhat.cloud.policies.app.auth;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

/**
 * Request filter. This runs on all incoming requests before method matching
 * and reads the x-rh-identity header. If that is not present, calls will
 * aborted with a "401 unauthorised" response.
 *
 * If the header is present and valid, we produce a {{@link java.security.Principal}}
 * and inject that into the call chain via {{@link SecurityContext}}. The
 * principal will also be made available for Injection, so that you can write in
 * your code.
 *
 * We don't yet query for RBAC here, as this filter is not part of the tracing
 * span, so we would not be able to trace the rbac calls.
 * See {@link RbacFilter} for this purpose.
 *
 * Usage in code:
 * <pre>{@code
 * @Inject
 * Principal user;
 * [...]
 * String username = user.getName();
 * }</pre>
 *
 * @author hrupp
 */
@PreMatching
@Provider
public class IncomingRequestFilter implements ContainerRequestFilter {

  private final Logger log = Logger.getLogger(this.getClass().getSimpleName());
  private final boolean logFine = log.isLoggable(Level.FINE);


  @Inject
  RhIdPrincipalProducer producer;

  volatile CurrentVertxRequest currentVertxRequest;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {

    RoutingContext routingContext = request().getCurrent();

    String normalisedPath = routingContext.normalisedPath();

    // The following are available to everyone
    if (normalisedPath.endsWith("openapi.json") ||
        normalisedPath.startsWith("/status") ||
        normalisedPath.startsWith("/admin")
    ) {
      return; // We are done here
    }

    String xrhid_header = requestContext.getHeaderString("x-rh-identity");

    if (xrhid_header==null || xrhid_header.isEmpty()) {
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
      if (logFine) {
        log.fine("No x-rh-identity header passed");
      }
      return;
    }

    // Now that we are sure that there is a header, we can use it.
    Optional<XRhIdentity> xrhid = HeaderHelper.getRhIdFromString(xrhid_header);
    if (xrhid.isPresent()) {
      // Basic sanity check
      XRhIdentity rhIdentity = xrhid.get();
      if (rhIdentity.getUsername() == null || rhIdentity.getUsername().isEmpty() ||
          rhIdentity.identity.accountNumber == null || rhIdentity.identity.accountNumber.isEmpty()
          )
      {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        if (logFine) {
          log.fine("X-rh-identity header has no user or account");
        }

        return;
      }
      // header was good, so now create the security context

      RhIdPrincipal rhPrincipal = new RhIdPrincipal(rhIdentity.getUsername(), rhIdentity.identity.accountNumber);
      rhPrincipal.setRawRhIdHeader(xrhid_header);

      // Attach account id to the context so we can log it later
      routingContext.put("x-rh-account",rhIdentity.identity.accountNumber);

      SecurityContext sctx = new RhIdSecurityContext(rhIdentity,rhPrincipal);
      requestContext.setSecurityContext(sctx);
      // And make the principal available for injection
      producer.setPrincipal(rhPrincipal);
    } else {
      // Header was present, but not parsable
      if (logFine) {
        log.fine("X-rh-identity header could not be parsed");
      }
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
    }
  }

  // Helper to get the vert.x routing context
  CurrentVertxRequest request() {
    if (currentVertxRequest == null) {
      currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }
    return currentVertxRequest;
  }

}
