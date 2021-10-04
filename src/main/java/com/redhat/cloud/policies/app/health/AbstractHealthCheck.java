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
package com.redhat.cloud.policies.app.health;

import com.redhat.cloud.policies.app.StuffHolder;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

public abstract class AbstractHealthCheck {

    HealthCheckResponseBuilder getBuilder(String name) {
        boolean degraded = StuffHolder.getInstance().isAdminDown();

        HealthCheckResponseBuilder builder = HealthCheckResponse.named(name);

        if (degraded) {
            builder.withData("status", "admin-down");
            builder.down();
        } else {
            builder.up();
        }
        return builder;
    }
}
