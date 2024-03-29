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
package com.redhat.cloud.policies.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.cloud.policies.app.model.pager.Page;
import com.redhat.cloud.policies.app.model.pager.Pager;
import com.redhat.cloud.policies.app.model.annotations.QueryableColumn;
import com.redhat.cloud.policies.app.model.filter.Filter;
import com.redhat.cloud.policies.app.model.validation.ValidActionS;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Policy extends PanacheEntityBase {

    // The ID will be created by code.
    @Id
    public
    UUID id;

    @JsonIgnore
    public String customerid;

    @JsonIgnore
    @Column(name = "org_id")
    public String orgId;

    @NotNull
    @NotEmpty
    @Schema(description = "Name of the rule. Must be unique per customer organization.")
    @Size(max = 150)
    @QueryableColumn(filterable = true)
    public String name;

    @Schema(description = "A short description of the policy.")
    @QueryableColumn(filterable = true)
    public String description;

    @Column(name = "is_enabled")
    @QueryableColumn(filterable = true)
    public boolean isEnabled;

    @Schema(description = "Condition string.",
            example = "arch = \"x86_64\"")
    @NotEmpty
    @NotNull
    public String conditions;

    @Schema(description = "String describing actions separated by ';' when the policy is evaluated to true." +
            "Allowed values is 'notification'")
    @ValidActionS
    public String actions;

    @Schema(type = SchemaType.STRING,
            description = "Last update time in a form like '2020-01-24 12:19:56.718', output only",
            readOnly = true,
            format = "yyyy-MM-dd hh:mm:ss.ddd",
            implementation = String.class)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @QueryableColumn
    private Timestamp mtime = new Timestamp(System.currentTimeMillis());

    @Schema(type = SchemaType.STRING,
            description = "Create time in a form like '2020-01-24 12:19:56.718', output only",
            readOnly = true,
            format = "yyyy-MM-dd hh:mm:ss.ddd",
            implementation = String.class)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Timestamp ctime = new Timestamp(System.currentTimeMillis());

    @Column(name = "last_triggered", insertable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @QueryableColumn
    private long lastTriggered;

    public void setMtime(String mtime) {
        this.mtime = Timestamp.valueOf(mtime);
    }

    public void setMtimeToNow() {
        this.mtime = new Timestamp(System.currentTimeMillis());
    }

    public String getMtime() {
        return mtime.toString();
    }

    public void setLastTriggered(long tTime) {
        lastTriggered = tTime;
    }

    public long getLastTriggered() {
        return lastTriggered;
    }

    public void setCtime(String ctime) {
        this.ctime = Timestamp.valueOf(ctime);
    }

    public String getCtime() {
        return ctime.toString();
    }

    public static Page<Policy> pagePoliciesForCustomer(EntityManager em, String orgid, Pager pager) {
        Filter filter = pager.getFilter().and("orgId", Filter.Operator.EQUAL, orgid);

        PanacheQuery<Policy> panacheQuery = find(
                filter.getQuery(),
                pager.getSort(),
                filter.getParameters()
        );

        if (pager.getLimit() != Pager.NO_LIMIT) {
            panacheQuery.range(pager.getOffset(), pager.getOffset() + pager.getLimit() - 1);
        }

        return new Page<>(
                panacheQuery.list(),
                pager,
                panacheQuery.count()
        );
    }

    public static List<UUID> getPolicyIdsForCustomer(EntityManager em, String orgId, Pager pager) {
        Filter filter = pager.getFilter().and("orgId", Filter.Operator.EQUAL, orgId);


        PanacheQuery<Policy> panacheQuery = find(
                filter.getQuery(),
                filter.getParameters()
        );

        return panacheQuery.project(PolicyId.class).list().stream().map(policyId -> policyId.id).collect(Collectors.toList());
    }

    public static Policy findById(String orgId, UUID theId) {
        return find("orgId = ?1 and id = ?2", orgId, theId).firstResult();
    }

    public static Policy findByName(String orgId, String name) {
        return find("orgId = ?1 and name = ?2", orgId, name).firstResult();
    }

    public void delete(Policy policy) {
        if (policy == null || !policy.isPersistent()) {
            throw new IllegalStateException("Policy was not persisted");
        }
        policy.delete();
        policy.flush();
    }

    public void populateFrom(Policy policy) {
        this.name = policy.name;
        this.description = policy.description;
        this.actions = policy.actions;
        this.conditions = policy.conditions;
        this.isEnabled = policy.isEnabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Policy{");
        sb.append("id=").append(id);
        sb.append(", customerid='").append(customerid).append('\'');
        sb.append(", orgid='").append(orgId).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", mtime=").append(mtime);
        sb.append('}');
        return sb.toString();
    }
}

@RegisterForReflection
class PolicyId {
    public final UUID id;

    public PolicyId(UUID id) {
        this.id = id;
    }
}
