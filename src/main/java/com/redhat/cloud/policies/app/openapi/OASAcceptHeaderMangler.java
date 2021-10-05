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
package com.redhat.cloud.policies.app.openapi;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

/**
 * Change the accept header if needed for Openapi requests
 */
@SuppressWarnings("unused")
public class OASAcceptHeaderMangler {

    /*
     * CPOL-107
     * Default return format for openapi is .yml
     * If the user requests 'openapi.json', the user assumes
     * that a JSON format is returned. Unfortunately does Quarkus not
     * honor the '.json' suffix but either requires a correct Accept
     * header or the use of a query parameter.
     *
     * We now look at the path and if it ends in .json, replace the
     * existing Accept heder with one that requests Json format.
     */
    @RouteFilter(401)
    void oasAcceptHeaderMangler(RoutingContext rc) {
        if (rc.normalizedPath().endsWith("openapi.json")) {
            rc.request().headers().remove("Accept");
            rc.request().headers().add("Accept", "application/json");
        }
        rc.next();
    }
}
