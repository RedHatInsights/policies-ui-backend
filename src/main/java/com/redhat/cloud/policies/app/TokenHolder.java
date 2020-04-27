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
package com.redhat.cloud.policies.app;

import java.util.UUID;

/**
 * @author hrupp
 */
public class TokenHolder {

  private static TokenHolder tokenHolder;

  private String token;

  private TokenHolder() {
    token = UUID.randomUUID().toString();
    System.out.println("Token: " + token);
  }

  public boolean compareToken(String input) {
    return token.equals(input);
  }

  public static TokenHolder getInstance() {
    if (tokenHolder==null) {
      tokenHolder = new TokenHolder();
    }
    return tokenHolder;
  }
}
