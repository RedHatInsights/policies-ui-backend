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
package com.redhat.cloud.policies.app.model.engine;

import com.redhat.cloud.policies.app.model.Policy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A trigger in the backend, that we send over
 * @author hrupp
 */
public class FullTrigger {

  private Map<String,String> actionToTriggerActionMap;

  public Trigger trigger;
  public List<Condition> conditions;

  public FullTrigger() {
    trigger = new Trigger();
    conditions = new ArrayList<>();
    trigger.actions = new HashSet<>();
    actionToTriggerActionMap = new HashMap<>();
    actionToTriggerActionMap.put("webhook","hooks");
    actionToTriggerActionMap.put("email","email");
  }

  public FullTrigger(Policy policy, boolean generatePseudoId) {
    this();
    if (generatePseudoId) {
      trigger.id = generateId();
    }
    else if (policy.id != null) {
      trigger.id = policy.id.toString();
    }
    trigger.name = policy.name;
    trigger.description = policy.description;
    trigger.enabled = policy.isEnabled;
    var cond = new Condition();
    cond.expression = policy.conditions;
    conditions.add(cond);
    storeActions(trigger, policy);
  }

  private void storeActions(Trigger trigger, Policy policy) {
    if (policy.actions == null) {
      return;
    }

    String[] actionsIn = policy.actions.split(";");
    for (String actionIn : actionsIn) {
      var ta = new TriggerAction();
      if (actionIn.trim().isEmpty()) {
        continue;
      }
      ta.actionPlugin = getActionName(actionIn);

      trigger.actions.add(ta);
    }
  }

  public FullTrigger(Policy policy) {
    this(policy,false);
  }

  private static String generateId() {
    return UUID.randomUUID().toString();
  }

  public void updateFromPolicy(Policy policy) {
    if (trigger.actions == null) {
      trigger.actions = new HashSet<>();
    }
    if (policy.actions == null || policy.actions.isEmpty()) {
      trigger.actions.clear();
    } else {
      String[] actionsIn = policy.actions.split(";");
      Set<TriggerAction> newActions = new HashSet<>();
      for (String actionIn : actionsIn) {
        if (actionIn.trim().isEmpty()) {
          continue;
        }
        String actionName;
        actionName = getActionName(actionIn);

        boolean found = false;
        for (TriggerAction ta : trigger.actions) {
          String taName = ta.actionPlugin;
          if (taName.equals(actionName)) {
            newActions.add(ta);
            found = true;
            break;
          }
        }
        if (!found) {
          var ta = new TriggerAction();
          ta.actionPlugin = actionName;
          newActions.add(ta);
        }
      }
      trigger.actions.removeIf(ta -> !newActions.contains(ta));
      trigger.actions.addAll(newActions);
    }

    trigger.enabled = policy.isEnabled;
    trigger.name = policy.name;
    trigger.description = policy.description;

    if (conditions.size()!=0) {
      conditions.get(0).expression = policy.conditions;
    }
    else {
      Condition condition = new Condition();
      condition.expression = policy.conditions;
      conditions.add(condition);
    }
  }

  private String getActionName(String actionIn) {
    String actionName = actionIn.toLowerCase();
    if (actionName.contains(" ")) {
      actionName = actionName.substring(0, actionName.indexOf(' '));
    }

    if (!actionToTriggerActionMap.containsKey(actionName)) {
      throw new IllegalArgumentException("Key " + actionName + " not found");
    }
    actionName = actionToTriggerActionMap.get(actionName);
    return actionName;
  }

}
