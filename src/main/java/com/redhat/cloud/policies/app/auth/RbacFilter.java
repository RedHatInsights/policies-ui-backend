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

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.cloud.policies.app.auth.models.RbacRaw;

@Provider
@Priority(Priorities.HEADER_DECORATOR + 1)
public class RbacFilter implements ContainerRequestFilter {

    public static final String APPLICATION = "policies";
    public static final String RESOURCE = "policies";

    @Inject
    RbacClient rbacClient;

    @Inject
    RhIdPrincipal user;

    @ConfigProperty(name = "warn.rbac.slow", defaultValue = "true")
    Instance<Boolean> warnSlowRbac;

    @ConfigProperty(name = "warn.rbac.tolerance", defaultValue = "1S")
    Instance<Duration> warnSlowRbacTolerance;

    @ConfigProperty(name = "rbac.enabled", defaultValue = "true")
    Instance<Boolean> isRbacEnabled;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!isRbacEnabled.get()) {
            // Allow all
            setPermissionsOnPrincipals(requestContext, true, true, null);
            return;
        }

        String path = requestContext.getUriInfo().getPath(true);
        if (path.startsWith("/admin") || path.equals("/api/policies/v1.0/status")) {
            return;
        }

        RbacRaw result = getRbacResult();
        if (result == null) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
        }

        List<UUID> hostGroupIds = null;
        try {
            hostGroupIds = hostGroupsToUUIDs(result.hostGroupIds());
        } catch (Throwable e) {
            Log.warnf("RBAC Host group parsing failed when reading %s: %s", result.hostGroupIds(), e);
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
        }

        boolean canReadPolicies = result.canRead(APPLICATION, RESOURCE);
        boolean canWritePolicies = result.canWrite(APPLICATION, RESOURCE);
        setPermissionsOnPrincipals(requestContext, canReadPolicies, canWritePolicies, hostGroupIds);
    }

    private RbacRaw getRbacResult() {
        RbacRaw result;
        long t1 = System.currentTimeMillis();
        try {
            result = rbacClient.getRbacInfo(user.getRawRhIdHeader());
        } catch (Throwable e) {
            Log.warn("RBAC call failed", e);
            return null;
        } finally {
            long t2 = System.currentTimeMillis();
            if (warnSlowRbac.get() && (t2 - t1) > warnSlowRbacTolerance.get().toMillis()) {
                Log.warnf("Call to RBAC took %d ms for orgId %s", t2 - t1, user.getOrgId());
            }
        }

        return result;
    }

    private void setPermissionsOnPrincipals(ContainerRequestContext requestContext,
                                            boolean canReadPolicies, boolean canWritePolicies,
                                            List<UUID> hostGroupIds) {
        user.setRbac(canReadPolicies, canWritePolicies, hostGroupIds);
        RhIdPrincipal userPrincipal = (RhIdPrincipal) requestContext.getSecurityContext().getUserPrincipal();
        userPrincipal.setRbac(canReadPolicies, canWritePolicies, hostGroupIds);
    }

    public static List<UUID> hostGroupsToUUIDs(List<String> hostGroupIds) throws IllegalArgumentException {
        if (hostGroupIds == null) {
            return null;
        }

        return hostGroupIds.stream().map(
            (String gid) -> gid != null ? UUID.fromString(gid) : null
        ).distinct().toList();
    }
}
