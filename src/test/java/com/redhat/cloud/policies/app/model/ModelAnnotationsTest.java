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

import com.redhat.cloud.policies.app.model.annotations.QueryableColumn;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ModelAnnotationsTest {

    class TestModel {

        public String invisible;

        @QueryableColumn
        public String defaultQueryable;

        @QueryableColumn(name = "other_name", sortable = false, filterable = true)
        public String queryableWithParams;

        @Column(name = "with_jakarta")
        @QueryableColumn(filterable = true)
        public String withJakarta;

        @QueryableColumn
        private String privateQueryable;

        public String otherParam;

    }

    @Test
    void testDefaultQueryableField() {
        ColumnInfo column = new ColumnGetter(TestModel.class).get("defaultQueryable");
        assertNotNull(column);
        assertEquals("defaultQueryable", column.getName());
        assertEquals("defaultQueryable", column.getFieldName());
        assertEquals(false, column.isFilterable());
        assertEquals(true, column.isSortable());
    }

    @Test
    void testQueryableFieldWithParams() {
        ColumnInfo column = new ColumnGetter(TestModel.class).get("other_name");
        assertNotNull(column);
        assertEquals("other_name", column.getName());
        assertEquals("queryableWithParams", column.getFieldName());
        assertEquals(true, column.isFilterable());
        assertEquals(false, column.isSortable());
    }

    @Test
    void testQueryableFieldWithJakartaColumn() {
        ColumnInfo column = new ColumnGetter(TestModel.class).get("with_jakarta");
        assertNotNull(column);
        assertEquals("with_jakarta", column.getName());
        assertEquals("withJakarta", column.getFieldName());
        assertEquals(true, column.isFilterable());
        assertEquals(true, column.isSortable());
    }

    @Test
    void testPrivateQueryableField() {
        ColumnInfo column = new ColumnGetter(TestModel.class).get("privateQueryable");
        assertNotNull(column);
        assertEquals("privateQueryable", column.getName());
        assertEquals("privateQueryable", column.getFieldName());
        assertEquals(false, column.isFilterable());
        assertEquals(true, column.isSortable());
    }

    @Test
    void testUnmatchedQueryableField() {
        ColumnGetter columnGetter = new ColumnGetter(TestModel.class);
        assertNull(columnGetter.get("nonExistent"));
        assertNull(columnGetter.get("otherParam"));
    }

}
