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

import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Optional;

/**
 * Request filter. This runs on all incoming requests before method matching
 * and reads the x-rh-identity header. If that is not present, calls will
 * aborted with a "401 unauthorised" response.
 * <p>
 * If the header is present and valid, we produce a {{@link java.security.Principal}}
 * and inject that into the call chain via {{@link SecurityContext}}. The
 * principal will also be made available for Injection, so that you can write in
 * your code.
 * <p>
 * We don't yet query for RBAC here, as this filter is not part of the tracing
 * span, so we would not be able to trace the rbac calls.
 * See {@link RbacFilter} for this purpose.
 * <p>
 * Usage in code:
 * <pre>{@code
 * @Inject
 * Principal user;
 * [...]
 * String username = user.getName();
 * }</pre>
 */
@PreMatching
@Provider
public class IncomingRequestFilter implements ContainerRequestFilter {

    public static final String X_RH_ACCOUNT = "x-rh-account";
    public static final String X_RH_ORG_ID = "x-rh-rbac-org-id";
    public static final String X_RH_USER = "x-rh-user";

    @Inject
    RhIdPrincipalProducer producer;

    CurrentVertxRequest currentVertxRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        RoutingContext routingContext = request().getCurrent();

        String normalisedPath = routingContext.normalizedPath();

        // The following are available to everyone
        if (normalisedPath.endsWith("openapi.json") ||
                normalisedPath.equals("/api/policies/v1.0/status") ||
                normalisedPath.startsWith("/admin")
        ) {
            return; // We are done here
        }

        // Get the x-rh-identity header and parse it.
        String xrhid_header = requestContext.getHeaderString("x-rh-identity");
        XRhIdentity rhIdentity = determineXRhIdentity(xrhid_header);
        if (rhIdentity == null) {
            // Header was somehow bad
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        // header was good, so now create the security context
        RhIdPrincipal rhPrincipal = new RhIdPrincipal(rhIdentity.getUsername(), rhIdentity.identity.accountNumber, rhIdentity.identity.orgId);
        rhPrincipal.setRawRhIdHeader(xrhid_header);

        // Attach account id, org id and user to the context so we could log it later
        if (rhPrincipal.getAccount() != null) {
            routingContext.put(X_RH_ACCOUNT, rhPrincipal.getAccount());
        }
        if (rhPrincipal.getOrgId() != null) {
            routingContext.put(X_RH_ORG_ID, rhPrincipal.getOrgId());
        }
        routingContext.put(X_RH_USER, rhIdentity.getUsername());

        // Provide the security identity so that we can get it in the access log
        SecurityIdentity si = QuarkusSecurityIdentity.builder()
            .setPrincipal(rhPrincipal)
            .build();
        routingContext.setUser(new QuarkusHttpUser(si));

        SecurityContext sctx = new RhIdSecurityContext(rhIdentity, rhPrincipal);
        requestContext.setSecurityContext(sctx);
        // And make the principal available for injection
        producer.setPrincipal(rhPrincipal);
    }

    private XRhIdentity determineXRhIdentity(String xrhid_header) {
        if (xrhid_header == null || xrhid_header.isEmpty()) {
            logIfNeeded("No x-rh-identity header passed");
            return null;
        }

        // Now that we are sure that there is a header, we can use it.
        Optional<XRhIdentity> xrhid = HeaderHelper.getRhIdFromString(xrhid_header);
        if (xrhid.isEmpty()) {
            // Header was present, but could not be parsed
            logIfNeeded("X-rh-identity header could not be parsed");
            return null;

        }

        // Basic sanity check
        XRhIdentity rhIdentity = xrhid.get();
        if (rhIdentity.getUsername() == null || rhIdentity.getUsername().isEmpty() ||
                rhIdentity.identity.accountNumber == null || rhIdentity.identity.accountNumber.isEmpty()
        ) {
            logIfNeeded("X-rh-identity header has no user or account");
            return null;
        }
        return rhIdentity;
    }

    private void logIfNeeded(String logMessage) {
        Log.debug(logMessage);
    }

    // Helper to get the vert.x routing context
    private CurrentVertxRequest request() {
        if (currentVertxRequest == null) {
            currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
        }
        return currentVertxRequest;
    }
}
