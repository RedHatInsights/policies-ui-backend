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
package com.redhat.cloud.policies.app.auth.models;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RbacRaw extends RbacRawCommon {

    public Map<String, String> links;
    public Map<String, Integer> meta;
    public List<Access> data;

    public boolean canRead(String application, String resource) {
        return findPermission(application, resource, READ_OPERATION);
    }

    public boolean canWrite(String application, String resource) {
        return findPermission(application, resource, WRITE_OPERATION);
    }

    public boolean canDo(String application, String resource, String operation) {
        return findPermission(application, resource, operation);

    }

    private boolean findPermission(String application, String resource, String operation) {
        if (data == null || data.size() == 0) {
            return false;
        }

        for (Access permissionEntry : data) {
            String[] fields = permissionEntry.getPermissionFields();
            if (Objects.equals(fields[0], application)) {
                if (Objects.equals(fields[1], resource) || Objects.equals(fields[1], ANY)) {
                    if (Objects.equals(fields[2], operation) || Objects.equals(fields[2], ANY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
