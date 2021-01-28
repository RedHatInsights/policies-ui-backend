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

/**
 * Simple implementation of {@link Principal}
 * @author hrupp
 */
public class RhIdPrincipal implements Principal {

  private String name;
  private String account;
  private boolean canReadPolicies;
  private boolean canWritePolicies;
  private String rawRhId;

  public RhIdPrincipal() {
  }

  public RhIdPrincipal(String name, String account) {
    this.name = name;
    this.account = account;
  }

  void setRbac(boolean canReadPolicies, boolean canWritePolicies) {

    this.canReadPolicies = canReadPolicies;
    this.canWritePolicies = canWritePolicies;
  }
  @Override
  public String getName() {
    return name;
  }

  public String getAccount() {
    return account;
  }

  public boolean canReadPolicies() {
    return canReadPolicies;
  }

  public boolean canWritePolicies() {
    return canWritePolicies;
  }


  public void setRawRhIdHeader(String rawRhId) {
    this.rawRhId = rawRhId;
  }

  public String getRawRhIdHeader() {
    return rawRhId;
  }
}
