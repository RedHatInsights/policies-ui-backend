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
package com.redhat.cloud.custompolicies.app.auth;

import java.io.IOException;
import java.util.Optional;
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
 * your code
 *
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

  @Inject
  RhIdPrincipalProducer producer;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {

    String xrhid_header = requestContext.getHeaderString("x-rh-identity");

    if (xrhid_header==null || xrhid_header.isEmpty()) {
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
      return;
    }

    // Now that we are sure that there is a header, we can use it.
    Optional<XRhIdentity> xrhid = HeaderHelper.getRhIdFromString(xrhid_header);
    if (xrhid.isPresent()) {
      // header was good, so now create the security context
      XRhIdentity rhIdentity = xrhid.get();
      SecurityContext sctx = new RhIdSecurityContext(rhIdentity);
      requestContext.setSecurityContext(sctx);
      // And make the principal available for injection
      producer.setPrincipal((RhIdPrincipal) sctx.getUserPrincipal());
    } else {
      // Header was present, but not correct
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
    }
  }
}
