/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Access extends RbacRawCommon {

    @JsonIgnore
    public static final String HOST_GROUPS_OPERATION = "in";

    @JsonIgnore
    public static final String HOST_GROUPS_KEY = "group.id";

    public String permission;
    public List<ResourceDefinition> resourceDefinitions;

    public String[] getPermissionFields() {
        if (permission == null) {
            return new String[3];
        }
        return Arrays.copyOf(permission.split(":"), 3);
    }

    public boolean isInventoryHostsRead() {
        String[] permissionFields = getPermissionFields();
        return Objects.equals(permissionFields[0], INVENTORY_HOSTS_READ[0])
            && (Objects.equals(permissionFields[1], INVENTORY_HOSTS_READ[1]) || Objects.equals(permissionFields[1], ANY))
            && (Objects.equals(permissionFields[2], INVENTORY_HOSTS_READ[2]) || Objects.equals(permissionFields[2], ANY));
    }

    public List<String> hostGroupIds() {
        if (resourceDefinitions == null || resourceDefinitions.size() == 0) {
            return null;
        }

        for (ResourceDefinition definition : resourceDefinitions) {
            var attributeFilter = definition.attributeFilter;
            if (attributeFilter != null && attributeFilter.isPresentWithValues()
                    && attributeFilter.operation.equals(HOST_GROUPS_OPERATION)
                    && attributeFilter.key.equals(HOST_GROUPS_KEY)
            ) {
                return attributeFilter.value;
            }
        }
        return List.of();
    }

}
