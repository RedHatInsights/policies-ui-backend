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
package com.redhat.cloud.custompolicies.app.model;

import com.redhat.cloud.custompolicies.app.model.pager.Page;
import com.redhat.cloud.custompolicies.app.model.pager.Pager;
import com.redhat.cloud.custompolicies.app.model.filter.Filter;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author hrupp
 */
@Entity
public class Policy extends PanacheEntityBase {

  @GeneratedValue(generator = "UUID")
  @GenericGenerator(
      name = "UUID",
      strategy = "org.hibernate.id.UUIDGenerator"
  )
  @Id
  public
  UUID id;

  @JsonbTransient
  public String customerid;

  @NotNull
  @NotEmpty
  @Schema(description = "Name of the rule. Must be unique per customer account.")
  public String name;

  @Schema(description = "A short description of the policy.")
  public String description;
  @Column(name = "is_enabled")
  public boolean isEnabled;

  @Schema(description = "Condition string.",
          example = "arch = \"x86_64\"")
  @NotEmpty
  @NotNull
  public String conditions;

  @Schema(description = "String describing actions when the policy is evaluated to true.")
  public String actions;

  @Schema(type = SchemaType.STRING,
          description = "Last update time in a form like '2020-01-24 12:19:56.718', output only",
          readOnly = true,
          format = "yyyy-MM-dd hh:mm:ss.ddd",
          implementation = String.class)
  private Timestamp mtime=new Timestamp(System.currentTimeMillis());

  @Schema(type = SchemaType.STRING,
          description = "Last evaluation time in a form like '2020-01-24 12:19:56.718', output only",
          readOnly = true,
          format = "yyyy-MM-dd hh:mm:ss.ddd",
          implementation = String.class)
  @Transient
  private Timestamp lastEvaluation;

  @Schema (type = SchemaType.STRING,
          description = "Create time in a form like '2020-01-24 12:19:56.718', output only",
          readOnly = true,
          format = "yyyy-MM-dd hh:mm:ss.ddd",
          implementation = String.class)
  private Timestamp ctime=new Timestamp(System.currentTimeMillis());


  @JsonbTransient
  public void setMtime(String mtime) {
    this.mtime = Timestamp.valueOf(mtime);
  }

  public void setMtimeToNow() {
    this.mtime = new Timestamp(System.currentTimeMillis());
  }

  public String getMtime() {
    return mtime.toString();
  }

  @JsonbTransient
  public void setLastEvaluation(long lastEvaluation) {
    this.lastEvaluation = new Timestamp(lastEvaluation);
  }

  public String getLastEvaluation() {
    return lastEvaluation != null ? lastEvaluation.toString() : "";
  }

  @JsonbTransient
  public void setCtime(String ctime) {
    this.ctime = Timestamp.valueOf(ctime);
  }

  public String getCtime() {
    return ctime.toString();
  }


  public static Page<Policy> pagePoliciesForCustomer(EntityManager em, String customer, Pager pager) {

    for (Sort.Column column : pager.getSort().getColumns()) {
      SortableColumn.fromName(column.getName());
    }

    pager.getFilter()
            .getParameters()
            .map()
            .keySet()
            .forEach(FilterableColumn::fromName);

    Filter filter = pager.getFilter().and("customerid", Filter.Operator.EQUAL, customer);
//  Todo: There is an oingoing discussion about quarkus supporting a "range" paging
//  https://github.com/quarkusio/quarkus/issues/3870
//  Should make below code easier to read.
//
//    PanacheQuery<Policy> panacheQuery = find(
//            filter.getQuery(),
//            pager.getSort(),
//            filter.getParameters()
//    ).page(io.quarkus.panache.common.Page.of(pager.getPage(), pager.getItemsPerPage()));
//    return new Page<>(panacheQuery.list(), pager, panacheQuery.count());

    String qs = "SELECT p FROM Policy p WHERE ";
    String cond =  filter.getQuery() + sortToOrderBy(pager.getSort());
    qs = qs + cond;

    Query q = em.createQuery(qs);
    // Always set the customer id - even if that may be part of parameters below
    q.setParameter("customerid",customer);
    q.setFirstResult(pager.getOffset());
    q.setMaxResults(pager.getLimit());
    for (Map.Entry<String,Object> param : filter.getParameters().map().entrySet()) {
      q.setParameter(param.getKey(),param.getValue());
    }
    List<Policy> results = q.getResultList();

    qs = "SELECT count(p) FROM Policy p WHERE ";
    qs = qs + filter.getQuery();

    q = em.createQuery(qs);
    // Always set the customer id - even if that may be part of parameters below
    q.setParameter("customerid",customer);
    for (Map.Entry<String,Object> param : filter.getParameters().map().entrySet()) {
      q.setParameter(param.getKey(),param.getValue());
    }
    long count = (long) q.getSingleResult();

    return new Page<>(results,pager,count);
  }

  public static Policy findById(String customer, UUID theId) {
    return find("customerid = ?1 and id = ?2", customer, theId).firstResult();
  }

  public static Policy findByName(String customer, String name) {
    return find("customerid = ?1 and name = ?2", customer, name).firstResult();
  }

  public UUID store(String customer, Policy policy) {
    if (!customer.equals(policy.customerid)) {
      throw new IllegalArgumentException("Store: customer id do not match");
    }
    policy.persist();
    return id;
  }

  public void delete(Policy policy) {
    if (policy==null || !policy.isPersistent()) {
      throw new IllegalStateException("Policy was not persisted");
    }
    policy.delete();
    policy.flush();
  }

  public void populateFrom(Policy policy) {
    this.id = policy.id;
    this.name = policy.name;
    this.description = policy.description;
    this.actions = policy.actions;
    this.conditions = policy.conditions;
    this.isEnabled = policy.isEnabled;
    this.customerid = policy.customerid;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Policy{");
    sb.append("id=").append(id);
    sb.append(", customerid='").append(customerid).append('\'');
    sb.append(", name='").append(name).append('\'');
    sb.append(", mtime=").append(mtime);
    sb.append('}');
    return sb.toString();
  }

  enum SortableColumn {
    NAME("name"),
    DESCRIPTION("description"),
    IS_ENABLED("is_enabled"),
    MTIME("mtime");

    private final String name;

    SortableColumn(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static SortableColumn fromName(String columnName) {
      for (SortableColumn column : SortableColumn.values()) {
        if (column.getName().equals(columnName)) {
          return column;
        }
      }
      throw new IllegalArgumentException("Unknown Policy.SortableColumn requested: [" + columnName + "]");
    }

  }

  enum FilterableColumn {
    NAME("name"),
    DESCRIPTION("description"),
    IS_ENABLED("is_enabled");

    private final String name;

    FilterableColumn(final String name) {
      this.name = name;
    }

    public static FilterableColumn fromName(String columnName) {
      Optional<FilterableColumn> result = Arrays.stream(FilterableColumn.values())
              .filter(val -> val.name.equals(columnName))
              .findAny();
      if (result.isPresent()) {
        return result.get();
      }
      throw new IllegalArgumentException("Unknown Policy.FilterableColumn requested: [" + columnName + "]");
    }
  }

  static String sortToOrderBy(Sort sort) {
    var columns = sort.getColumns();
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        for (int i = 0; i < columns.size(); i++) {
            Sort.Column column = columns.get(i);
            if (i > 0)
                sb.append(" , ");
            sb.append(column.getName());
            if (column.getDirection() != Sort.Direction.Ascending)
                sb.append(" DESC");
        }
        return sb.toString();
    }
}
