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

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

/**
 * Produce Principals for injection
 * @author hrupp
 */
@Priority(1)
@Alternative
@RequestScoped
public class RhIdPrincipalProducer {

  private RhIdPrincipal principal;

  public void setPrincipal(RhIdPrincipal principal) {
    this.principal = principal;
  }

  @RequestScoped
  @Produces
  RhIdPrincipal currentPrincipal() {
    return this.principal;
  }
}
