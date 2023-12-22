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
package com.redhat.cloud.policies.app.rest;

import com.redhat.cloud.policies.app.StuffHolder;
import com.redhat.cloud.policies.app.health.ScheduledStatusProducer;
import com.redhat.cloud.policies.app.model.Msg;
import com.redhat.cloud.policies.app.model.Policy;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Path("/admin")
@Produces("application/json")
@Consumes("application/json")
@RequestScoped
public class AdminService {

    @Inject
    EntityManager entityManager;

    @Inject
    ScheduledStatusProducer statusProducer;

    @ConfigProperty(name = "stats.filter.cid")
    Optional<String> filterIdsString;

    Set<String> filterIds = new HashSet<>();

    private static final String[] BUCKETS = {"1", "2", "3", "4", "5-10", "10+"};

    @PostConstruct
    void postConstruct() {

        if (filterIdsString.isPresent()) {
            String s = filterIdsString.get();
            String[] ids = s.split(",");
            filterIds.addAll(Arrays.asList(ids));
        }

    }

    /**
     * Signal health to outside world. Allowed values for state
     * * 'ok': all ok
     * * 'degraded': signal instance as degraded on status endpoint
     * * 'admin-down': signal health-checks as down. Signals k8s to kill the pod
     */
    @Path("/status")
    @POST
    public Response setAdminDown(@QueryParam("status") Optional<String> status) {

        Response.ResponseBuilder builder;

        StuffHolder th = StuffHolder.getInstance();

        switch (status.orElse("ok")) {
            case "ok":
                th.setDegraded(false);
                th.setAdminDown(false);
                builder = Response.ok()
                        .entity(new Msg("Reset state to ok"));
                break;
            case "degraded":
                th.setDegraded(true);
                builder = Response.ok()
                        .entity(new Msg("Set degraded state"));
                break;
            case "admin-down":
                th.setAdminDown(true);
                builder = Response.ok()
                        .entity(new Msg("Set admin down state"));
                break;
            default:
                builder = Response.status(Response.Status.BAD_REQUEST)
                        .entity(new Msg("Unknown status passed"));
        }

        statusProducer.update();

        return builder.build();
    }

    @Path("/stats")
    @GET
    public Response getStats() {
        long totalCount = Policy.count();
        Map<String, Integer> buckets = populateBuckets();

        List<Object[]> list = entityManager.createNativeQuery("select p.customerId,count (p) from policy p group by p.customerId")
                .getResultList();
        System.out.println(list.size());
        long cCount = list.size();

        Map<String, Object> idsMap = new HashMap<>();
        idsMap.put("ids", filterIds);
        long fcount = Policy.count("customerId in (:ids)", idsMap);

        final long[] filteredIdsCount = {0};
        list.forEach(i -> {
            String cid = (String) i[0];

            if (filterIds.contains(cid)) {
                filteredIdsCount[0]++;
            }
            int count = ((BigInteger) i[1]).intValue();
            putToBucket(buckets, count);
        });


        Map<String, Object> result = new HashMap<>();
        result.put("totalPoliciesCount", totalCount);
        result.put("filteredPoliciesCount", fcount);
        result.put("customerPoliciesCount", totalCount - fcount);
        result.put("totalCount", cCount);
        result.put("filteredIdsCount", filteredIdsCount[0]);
        result.put("customerIdsCount", cCount - filteredIdsCount[0]);
        result.put("buckets", buckets);

        return Response.ok().entity(result).build();
    }

    private Map<String, Integer> populateBuckets() {
        Map<String, Integer> map = new HashMap<>();
        Arrays.stream(BUCKETS).sequential().forEach(b -> map.put(b, 0));
        return map;
    }

    private void putToBucket(Map<String, Integer> buckets, int count) {
        String bucket;

        if (count < 5) {
            bucket = String.valueOf(count);
        } else if (count < 10) {
            bucket = "5-10";

        } else {
            bucket = "10+";
        }
        buckets.computeIfPresent(bucket, (b, c) -> buckets.get(b) + 1);
        buckets.putIfAbsent(bucket, 1);
    }
}
