/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.lang.reflect.Field;

import com.redhat.cloud.policies.app.model.annotations.QueryableColumn;

import jakarta.persistence.Column;

public class ColumnGetter {

    private Class<?> model;

    public ColumnGetter(Class<?> model) {
        this.model = model;
    }

    public ColumnInfo get(String queriedName) {
        for (Field field : model.getDeclaredFields()) {
            QueryableColumn ann = field.getAnnotation(QueryableColumn.class);
            if (ann == null) {
                continue;
            }

            String name = ann.name();
            if (name.isEmpty()) {
                Column jakartaAnn = field.getAnnotation(Column.class);
                if (jakartaAnn != null) {
                    name = jakartaAnn.name();
                }
                if (name == null || name.isEmpty()) {
                    name = field.getName();
                }
            }

            if (name.equals(queriedName)) {
                return new ColumnInfo(name, field.getName(), ann.filterable(), ann.sortable());
            }

        }
        return null;
    }

}
