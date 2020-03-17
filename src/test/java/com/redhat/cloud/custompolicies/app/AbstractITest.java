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
package com.redhat.cloud.custompolicies.app;


import io.restassured.http.Header;
import org.junit.jupiter.api.BeforeAll;

/**
 * Test base for a few common things.
 * The heavy lifting of mock-setup is done in the {@link TestLifecycleManager}
 * @author hrupp
 */
public abstract class AbstractITest {

  static Header authHeader;       // User with access rights
  static Header authRbacNoAccess; // Hans Dampf has no rbac access rights

  static final String API_BASE = "/api/custom-policies/v1.0";

  @BeforeAll
  static void setupRhId() {
    // provide rh-id
    String rhid = HeaderHelperTest.getStringFromFile("rhid.txt",false);
    authHeader = new Header("x-rh-identity", rhid);
    rhid = HeaderHelperTest.getStringFromFile("rhid_hans.txt",false);
    authRbacNoAccess = new Header("x-rh-identity", rhid);
  }

}
