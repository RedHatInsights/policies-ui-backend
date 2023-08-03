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

public class Access {

    public String permission;
    public List<ResourceDefinition> resourceDefinitions;

    public String[] getPermissionFields() {
        if (permission == null) {
            return new String[3];
        }
        return Arrays.copyOf(permission.split(":"), 3);
    }

}
