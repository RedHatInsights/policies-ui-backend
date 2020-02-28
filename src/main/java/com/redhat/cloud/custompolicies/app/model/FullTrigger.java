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
package com.redhat.cloud.custompolicies.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A trigger in the backend, that we send over
 * @author hrupp
 */
public class FullTrigger {


  public Trigger trigger;
  public List<Condition> conditions;


  public FullTrigger() {
    trigger = new Trigger();
    conditions = new ArrayList<>();
  }

  public FullTrigger(Policy policy, boolean generatePseudoId) {
    trigger = new Trigger();
    if (generatePseudoId) {
      trigger.id = generateId();
    }
    else {
      trigger.id = policy.id.toString();
    }
    trigger.name = policy.name;
    trigger.description = policy.description;
    trigger.enabled = policy.isEnabled;
    conditions = new ArrayList<>(1);
    var cond = new Condition();
    cond.expression = policy.conditions;
    conditions.add(cond);
  }

  public FullTrigger(Policy policy) {
    this(policy,false);
  }

  private static String generateId() {
    return UUID.randomUUID().toString();
  }


  public class Trigger {
    public String id;
    public String name;
    public String description;
    public boolean enabled;


  }

  public class Condition {
    public String triggerMode = "FIRING";
    public String type = "EVENT";
    public String dataId = "insights_report";
    public String expression;
  }
}
