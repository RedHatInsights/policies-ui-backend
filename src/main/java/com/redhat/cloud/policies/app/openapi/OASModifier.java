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
package com.redhat.cloud.policies.app.openapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.media.Schema;

/**
 * Modify the path in the openapi document to not
 * have the prefix, which is already in the
 * servers part of the document.
 */
public class OASModifier implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        Set<String> keySet = paths.getPathItems().keySet();
        Map<String, PathItem> replacementItems = new HashMap<>();
        for (String key : keySet) {
            PathItem p = paths.getPathItem(key);
            String mangledName = mangleName(key);
            if (!mangledName.startsWith("/user-config") && // POL-230 this is a private api, so don't show it.
                    !mangledName.startsWith("/policies/sync") &&
                    !mangledName.startsWith("/admin") &&
                    !mangledName.startsWith("/status")
            ) { // POL-277 private api
                replacementItems.put(mangledName, p);
            }

            Map<PathItem.HttpMethod, Operation> operations = p.getOperations();
            for (Map.Entry<PathItem.HttpMethod, Operation> entry : operations.entrySet()) {
                Operation op = entry.getValue();

                if (op.getOperationId() == null || op.getOperationId().isEmpty()) {

                    String id = toCamelCase(mangledName);
                    id = entry.getKey().toString().toLowerCase() + id;
                    op.setOperationId(id);
                }
            }
        }
        paths.setPathItems(replacementItems);

        // Settings values should not be shown in openapi export
        Components components = openAPI.getComponents();
        Map<String, Schema> existingSchemas = components.getSchemas();
        Map<String, Schema> modifiedSchemas = new HashMap<>(existingSchemas);
        modifiedSchemas.remove("SettingsValues");
        components.setSchemas(modifiedSchemas);
    }

    private String toCamelCase(String mangledName) {
        StringBuilder sb = new StringBuilder();
        boolean needsUpper = false;
        for (char c : mangledName.toCharArray()) {
            if (c == '/') {
                needsUpper = true;
                continue;
            }
            if (c == '}') {
                continue;
            }
            if (c == '{') {
                sb.append("By");
                needsUpper = true;
                continue;
            }
            if (needsUpper) {
                sb.append(Character.toUpperCase(c));
                needsUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String mangleName(String key) {
        // Base Filler - would report as empty otherwise
        if (key.equals("/api/policies/v1.0")) {
            return "/";
        }
        return key.replace("/api/policies/v1.0", "");
    }
}
