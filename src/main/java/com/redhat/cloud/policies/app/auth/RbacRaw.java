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
package com.redhat.cloud.policies.app.auth;

import java.util.List;
import java.util.Map;

public class RbacRaw {

    public static final String ANY = "*";

    public Map<String, String> links;
    public Map<String, Integer> meta;
    public List<Map<String, Object>> data;

    public boolean canRead(String path) {
        return findPermission(path, "read");
    }

    public boolean canWrite(String path) {
        return findPermission(path, "write");
    }

    public boolean canReadAll() {
        return canRead(ANY);
    }

    public boolean canWriteAll() {
        return canWrite(ANY);
    }

    public boolean canDo(String path, String permission) {
        return findPermission(path, permission);

    }

    private boolean findPermission(String path, String what) {
        if (data == null || data.size() == 0) {
            return false;
        }

        for (Map<String, Object> permissionEntry : data) {
            String[] fields = getPermissionFields(permissionEntry);
            if (fields[1].equals(path) || fields[1].equals(ANY)) {
                if (fields[2].equals(what) || fields[2].equals(ANY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String[] getPermissionFields(Map<String, Object> map) {
        String perms = (String) map.get("permission");
        return perms.split(":");
    }
}
