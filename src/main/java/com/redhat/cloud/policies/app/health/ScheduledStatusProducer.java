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

import com.redhat.cloud.policies.app.PolicyEngine;
import com.redhat.cloud.policies.app.StuffHolder;
import com.redhat.cloud.policies.app.model.Policy;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * We gather the Status of ourselves and remotes.
 * This is then put into the StuffHolder from
 * where both the Gauge below and the Status rest
 * endpoint can fetch it.
 */
@ApplicationScoped
public class ScheduledStatusProducer {

    public static final String DUMMY = "dummy";

    @Inject
    @RestClient
    PolicyEngine engine;

    //  // Quarkus only activates this after the first REST-call to any method in this class
    @Gauge(name = "status_isDegraded_should_break_tests", unit = MetricUnits.NONE, absolute = true,
            description = "Returns 0 if good, value > 0 for number of entries in the status message")
    int isDegradedShouldBreakTests() {
        return StuffHolder.getInstance().getStatusInfo().size();
    }

    @Scheduled(every = "10s")
    void gather() {

        Map<String, String> issues;
        issues = new HashMap<>();

        // Admin has used the endpoint to signal degraded status
        boolean degraded = StuffHolder.getInstance().isDegraded();
        if (degraded) {
            issues.put("admin-degraded", "true");
        }

        // Now the normal checks
        try {
            Policy.findByName(DUMMY, "-dummy-");
        } catch (Exception e) {
            issues.put("backend-db", e.getMessage());
        }

        try {
            engine.findTriggersById(DUMMY, DUMMY);
        } catch (Exception e) {
            issues.put("engine", e.getMessage());
        }

        StuffHolder.getInstance().setStatusInfo(issues);
    }

    public void update() {
        gather();
    }
}
