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
package com.redhat.cloud.custompolicies.app;

import javax.persistence.Column;

/**
 * Stripped down policy object for testing purposes.
 * @author hrupp
 */
public class TestPolicy {

  public long id;
  public String customerid;

  public String name;
  public String description;
  @Column(name = "is_enabled")
  public boolean isEnabled;

  public String conditions;
  public String actions;

  public String mtime;
  public String triggerId;
}
