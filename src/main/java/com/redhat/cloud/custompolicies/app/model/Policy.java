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

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author hrupp
 */
@Entity
public class Policy extends PanacheEntity {
  public String customerid;

  @NotNull
  @NotEmpty
  public String name;
  public String description;
  @Column(name = "is_enabled")
  public boolean isEnabled;

  @NotEmpty
  @NotNull
  public String conditions;
  public String actions;


  public static List<Policy> listPoliciesForCustomer(String customer) {
    return find("customerid", customer).list();
  }

  public static Policy findById(String customer, Long theId) {
    return find("customerid = ?1 and id = ?2", customer, theId).firstResult();
  }

  public static Policy findByName(String customer, String name) {
    return find("customerid = ?1 and name = ?2", customer, name).firstResult();
  }

  public Long store(String customer, Policy policy) {
    if (!customer.equals(policy.customerid)) {
      throw new IllegalArgumentException("Store: customer id do not match");
    }
    policy.persistAndFlush();
    return id;
  }

  public void delete(Policy policy) {
    if (policy==null || !policy.isPersistent()) {
      throw new IllegalStateException("Policy was not persisted");
    }
    policy.delete();
    policy.flush();
  }
}
