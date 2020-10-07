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
package com.redhat.cloud.policies.app.model;

import java.util.List;

/**
 * A JsonPatch object
 * See https://tools.ietf.org/html/rfc6902
 * @author hrupp
 */
public class JsonPatch {

  public List<JsonPatchOp> operations;

  public JsonPatch() {
  }

  static public class JsonPatchOp {
    public Operation op;
    public String path;
    String value;

    public JsonPatchOp() {
    }
  }

  public enum Operation {
    add,
    remove,
    replace,
    move,
    copy,
    test
  }
}
