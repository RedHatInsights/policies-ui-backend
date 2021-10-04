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

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;

/**
 * An implementation of SecurityContext, that gets the data
 * from the parsed x-rh-identity header.
 */
public class RhIdSecurityContext implements SecurityContext {

    private XRhIdentity rhIdentity;
    private RhIdPrincipal rhPrincipal;

    public RhIdSecurityContext(XRhIdentity rhIdentity, RhIdPrincipal rhPrincipal) {
        this.rhIdentity = rhIdentity;
        this.rhPrincipal = rhPrincipal;
    }

    @Override
    public Principal getUserPrincipal() {
        return rhPrincipal;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;  // TODO: Determine later by calling the backend.
    }

    @Override
    public boolean isSecure() {
        return false;  // TODO:determine from call?
    }

    @Override
    public String getAuthenticationScheme() {
        return "X-RH-IDENTITY";
    }
}
