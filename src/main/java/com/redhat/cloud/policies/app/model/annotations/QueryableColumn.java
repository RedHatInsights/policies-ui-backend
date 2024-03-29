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
package com.redhat.cloud.policies.app.model.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 */
@Target({FIELD})
@Retention(RUNTIME)
public @interface QueryableColumn {

    /**
     * (Optional) The name of the sortable column. Defaults to
     * the jakarta Column name or field name.
     */
    String name() default "";

    /**
     * (Optional) Allow the column used in sorting queries.
     * Default is true.
     */
    boolean sortable() default true;

    /**
     * (Optional) Allow the column used in filter queries.
     * Default is false.
     */
    boolean filterable() default false;
}
