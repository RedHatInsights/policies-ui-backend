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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.io.IOException;
import java.time.Duration;

import javax.annotation.Priority;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Provider
@Priority(Priorities.HEADER_DECORATOR + 1)
public class RbacFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(RbacFilter.class);

    @Inject
    Tracer tracer;

    @Inject
    RbacClient rbacClient;

    @Inject
    RhIdPrincipal user;

    @ConfigProperty(name = "warn.rbac.slow", defaultValue = "true")
    Instance<Boolean> warnSlowRbac;

    @ConfigProperty(name = "warn.rbac.tolerance", defaultValue = "1S")
    Duration warnSlowRbacTolerance;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        RbacRaw result;

        String path = requestContext.getUriInfo().getPath(true);
        if (path.startsWith("/admin") || path.equals("/api/policies/v1.0/status")) {
            return;
        }

        long t1 = System.currentTimeMillis();
        Span span = tracer.buildSpan("getRBac").start();
        try (Scope ignored = tracer.scopeManager().activate(span)) {
            result = rbacClient.getRbacInfo(user.getRawRhIdHeader());
        } catch (Throwable e) {
            LOGGER.warn("RBAC call failed", e);
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
        } finally {
            long t2 = System.currentTimeMillis();
            if (warnSlowRbac.get() && (t2 - t1) > warnSlowRbacTolerance.toMillis()) {
                LOGGER.warnf("Call to RBAC took %d ms", t2 - t1);
            }
            span.finish();
        }

        final String policiesPath = "policies";

        user.setRbac(result.canRead(policiesPath), result.canWrite(policiesPath));
        RhIdPrincipal userPrincipal = (RhIdPrincipal) requestContext.getSecurityContext().getUserPrincipal();
        userPrincipal.setRbac(result.canRead(policiesPath), result.canWrite(policiesPath));
    }
}
