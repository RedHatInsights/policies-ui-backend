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

import com.redhat.cloud.policies.app.PolicyEngine;
import com.redhat.cloud.policies.app.StuffHolder;
import com.redhat.cloud.policies.app.model.Msg;
import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.engine.FullTrigger;
import com.redhat.cloud.policies.app.model.engine.Trigger;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hrupp
 */
@Path("/admin")
@Produces("application/json")
@Consumes("application/json")
@RequestScoped
public class AdminService {

  private final Logger log = Logger.getLogger(this.getClass().getSimpleName());

  @Inject
  @RestClient
  PolicyEngine engine;

  @Inject
  EntityManager entityManager;

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

    return builder.build();
  }

  @Path("/sync")
  @POST
  @Transactional
  public Response syncToEngine(@QueryParam("token") String token) {

    boolean validToken = StuffHolder.getInstance().compareToken(token);
    if (!validToken) {
      return Response.status(Response.Status.FORBIDDEN).entity("You don't have permission for this").build();
    }

    final int[] count = {0};
    try (Stream<Policy> policies = Policy.streamAll()) {
      policies.forEach(p -> {
        FullTrigger fullTrigger;
        try {
          fullTrigger = engine.fetchTrigger(p.id, p.customerid);
        }
        catch (NotFoundException nfe) {
          fullTrigger=null;
        }
        if (fullTrigger == null) { // Engine does not have the trigger
          log.info("Trigger " + p.id + " not found, syncing");
          FullTrigger ft = new FullTrigger(p);
          engine.storeTrigger(ft, false, p.customerid);
          log.info("   done");
          count[0]++;
        }
        else {
          log.info("Trigger " + p.id + " already in engine, skipping");
        }
      });
    }
    String s = "Stored " + count[0] + " triggers";
    log.info(s);
    return Response.ok().entity(new Msg(s)).build();
  }

  @GET
  @Path("/verify")
  public Response findOrphans() {

    Map<String,List<TTT>> orphanedPolicies = new HashMap<>();
    List<TTT> orphanedInDB = new ArrayList<>();
    List<TTT> orphanedInEngine = new ArrayList<>();

    // Find active policies that do not have a trigger in engine
    try (Stream<Policy> policies = Policy.streamAll()) {
      policies
          .forEach(p -> {
            try {
              engine.fetchTrigger(p.id, p.customerid);
            }
            catch (NotFoundException nfe) {
              orphanedInDB.add(new TTT(p.customerid,p.id));
            }
          });
    }
    orphanedPolicies.put("orphanedInDB",orphanedInDB);

    // Find triggers in engine for accounts and check if
    // they have an active trigger in the DB
    List<String> customersInDb;
    try (Stream<Policy> policies = Policy.streamAll()) {
      customersInDb = policies.map(p -> p.customerid)
          .distinct().collect(Collectors.toList());
    }
    customersInDb
        .forEach(cid-> {
          List<Trigger> triggers = engine.findTriggersForCustomer(cid);
          triggers.forEach(t -> {
            Policy pol = Policy.findById(cid, UUID.fromString(t.id));
            if (pol == null) {
              orphanedInEngine.add(new TTT(cid,t.id));
            }
          });
        });
    orphanedPolicies.put("orphanedInEngine",orphanedInEngine);

    return Response.ok().entity(orphanedPolicies).build();
  }

  @Path("/stats")
  @GET
  public Response getStats() {
    long totalCount = Policy.count();
    BigInteger cCount = (BigInteger) entityManager.createNativeQuery(
        "select count(X) from ( select distinct p.customerId from policy p )  as X")
        .getSingleResult();

    Map<String,Long> result = new HashMap<>();
    result.put("policiesCount",totalCount);
    result.put("customerCount",cCount.longValue());

    return Response.ok().entity(result).build();
  }

  // Trigger Tenant Tupple
  public static class TTT {
    String cid; // Tenant
    String tid; // TriggerId

    public TTT(String customerId, String id) {
      this.cid = customerId;
      this.tid = id;
    }

    public TTT(String customerid, UUID id) {
      this(customerid,id.toString());
    }

    public String getCid() {
      return cid;
    }

    public String getTid() {
      return tid;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("TTT{");
      sb.append("cId='").append(cid).append('\'');
      sb.append(", tId='").append(tid).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }
}
