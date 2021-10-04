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
package com.redhat.cloud.policies.app;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Redirect routes with only the major version
 * to major.minor ones.
 */
@SuppressWarnings("unused")
public class RouteRedirector {

    private static final String API_POLICIES_V_1 = "/api/policies/v1/";
    private static final String API_POLICIES_V_1_0 = "/api/policies/v1.0/";

    Logger log = Logger.getLogger(this.getClass().getSimpleName());

    /**
     * If the requested route is the one with major version only,
     * we rewrite it on the fly.
     * We need to take the URI from the underlying http request, as the
     * normalised path does not contain query parameters.
     *
     * @param rc RoutingContext from vert.x
     */
    @RouteFilter(400)
    void myRedirector(RoutingContext rc) {
        String uri = rc.request().uri();
        if (log.isLoggable(Level.FINER)) {
            uri = uri.replaceAll("[\n|\r|\t]", "_");
            log.finer("Incoming uri: " + uri);
        }
        if (uri.startsWith(API_POLICIES_V_1)) {
            String remain = uri.substring(API_POLICIES_V_1.length());
            if (log.isLoggable(Level.FINER)) {
                remain = remain.replaceAll("[\n|\r|\t]", "_");
                log.finer("Rerouting to :" + API_POLICIES_V_1_0 + remain);
            }

            rc.reroute(API_POLICIES_V_1_0 + remain);
            return;
        }
        rc.next();
    }
}
