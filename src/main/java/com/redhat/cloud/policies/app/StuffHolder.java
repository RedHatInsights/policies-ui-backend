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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StuffHolder {

    private static StuffHolder tokenHolder;

    private final String token;
    private boolean adminDown;
    private boolean degraded;
    private Map<String, String> statusInfo;

    private StuffHolder() {
        token = UUID.randomUUID().toString();
        adminDown = false;
        System.out.println("Token: " + token);
        statusInfo = new HashMap<>();
    }

    public boolean compareToken(String input) {
        return token.equals(input);
    }

    public static StuffHolder getInstance() {
        if (tokenHolder == null) {
            tokenHolder = new StuffHolder();
        }
        return tokenHolder;
    }

    public boolean isAdminDown() {
        return adminDown;
    }

    public void setAdminDown(boolean status) {
        this.adminDown = status;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public Map<String, String> getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(Map<String, String> statusInfo) {
        this.statusInfo = statusInfo;
    }
}
