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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Data model of the representation of a x-rh-identity header.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XRhIdentity {

    public Map<String, Object> entitlements;
    public Identity identity;

    public String getUsername() {
        if (identity == null || identity.user == null) {
            return null;
        }
        return identity.user.username;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Identity {

        @JsonProperty("account_number")
        public String accountNumber;

        @JsonProperty("org_id")
        public String orgId;

        public String type;
        public User user;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {

        public String email;
        @JsonProperty("first_name")
        public String firstName;
        @JsonProperty("last_name")
        public String lastName;
        public String username;
        @JsonProperty("is_active")
        public boolean isActive;
        @JsonProperty("is_internal")
        public boolean isInternal;
        @JsonProperty("is_org_admin")
        public boolean isOrgAdmin;
    }
}
