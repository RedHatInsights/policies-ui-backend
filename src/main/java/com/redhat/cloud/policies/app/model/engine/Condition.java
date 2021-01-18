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

/**
 * @author hrupp
 */
public class Condition {
  public String conditionId;
  public String triggerMode = "FIRING";
  public String type = "EVENT";
  // The dataId must stay as is, and be the same as in Receiver.java in the engine
  public String dataId = "platform.inventory.host-egress";
  public String expression;
  public long lastEvaluation;

  public Condition() {
    // needed for (de)serialization purposes
  }
}
