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
package com.redhat.cloud.custompolicies;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

/**
 * Redirect routes with only the major version
 * to major.minor ones.
 * @author hrupp
 */
@SuppressWarnings("unused")
public class RouteRedirector {

  private static final String API_CUSTOM_POLICIES_V_1 = "/api/custom-policies/v1/";
  private static final String API_CUSTOM_POLICIES_V_1_0 = "/api/custom-policies/v1.0/";

  /**
   * If the requested route is the one with major version only,
   * send a 307 "Moved temporarily" with the location of the version
   * with major.minor.
   * See https://tools.ietf.org/html/rfc7231#section-6.4.7
   * Using a 308 code "Moved permanently" would make sense, but
   * it is not clear how much this code is supported in the wild.
   * @param rc RoutingContext from vert.x
   */
  @RouteFilter(400)
  void myRedirector(RoutingContext rc) {
    if (rc.normalisedPath().startsWith(API_CUSTOM_POLICIES_V_1)) {
      String remain = rc.normalisedPath().substring(API_CUSTOM_POLICIES_V_1.length());
      rc.response().putHeader("Location", API_CUSTOM_POLICIES_V_1_0 +remain);
      rc.fail(307);
    }
    rc.next();
  }
}
