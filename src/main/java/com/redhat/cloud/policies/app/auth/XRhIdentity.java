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

import java.util.Map;
import javax.json.bind.annotation.JsonbProperty;

/**
 * Data model of the representation of a x-rh-identity header.
 * @author hrupp
 */
public class XRhIdentity {

  public Map<String, Object> entitlements;
  public Identity identity;

  public String getUsername() {
    if (identity==null || identity.user==null) {
      return null;
    }
    return identity.user.username;
  }

  public static class Identity {

    @JsonbProperty("account_number")
    public String accountNumber;
    public String type;
    public User user;
    public Internal internal;
  }

  public static class User {

    public String email;
    @JsonbProperty("first_name")
    public String firstName;
    @JsonbProperty("last_name")
    public String lastName;
    public String username;
    @JsonbProperty("is_active")
    public boolean isActive;
    @JsonbProperty("is_internal")
    public boolean isInternal;
    @JsonbProperty("is_org_admin")
    public boolean isOrgAdmin;
  }

  public static class Internal {

    @JsonbProperty("org_id")
    public String orgId;
  }
}
