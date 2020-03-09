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
package com.redhat.cloud.custompolicies.app.openapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;

/**
 * Modify the path in the openapi document to not
 * have the prefix, which is already in the
 * servers part of the document.
 * @author hrupp
 */
public class OASModifier implements OASFilter {

  @Override
  public void filterOpenAPI(OpenAPI openAPI) {
    Paths paths = openAPI.getPaths();
    Set<String> keySet = paths.getPathItems().keySet();
    Map<String,PathItem> replacementItems = new HashMap<>();
    for (String key : keySet) {
      PathItem p = paths.getPathItem(key);
      String mangledName = mangleName(key);
      replacementItems.put(mangledName,p);

      Map<PathItem.HttpMethod, Operation> operations = p.getOperations();
      for (Map.Entry<PathItem.HttpMethod,Operation> entry: operations.entrySet()) {
        Operation op = entry.getValue();

        String id = mangledName.replace('/','_') ;
        id = entry.getKey().toString() + id ;
        op.setOperationId(id);
      }
    }
    paths.setPathItems(replacementItems);
  }

  private String mangleName(String key) {
    return key.replace("/api/custom-policies/v1.0","");
  }
}
