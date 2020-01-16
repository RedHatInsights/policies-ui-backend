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
package com.redhat.cloud.custompolicies.app.model.pager;

import io.quarkus.panache.common.Sort;

public class Pager {

    private final int page;
    private final int itemsPerPage;
    private final Sort sort;

    public Pager(int page, int itemsPerPage, Sort sort) {
        this.page = page;
        this.itemsPerPage = itemsPerPage;
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public Sort getSort() {
        return sort;
    }

    public static PagerBuilder builder() {
        return new PagerBuilder();
    }

    public static class PagerBuilder {

        private int page;
        private int itemsPerPage;
        private Sort sort;

        private PagerBuilder() {
            this.page = 0;
            this.itemsPerPage = 10;
            this.sort = null;
        }

        public PagerBuilder page(int page) {
            this.page = page;
            return this;
        }

        public PagerBuilder itemsPerPage(int itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
            return this;
        }

        public PagerBuilder addSort(String column, Sort.Direction direction) {
            if (this.sort == null) {
                this.sort = Sort.by(column, direction);
            } else {
                this.sort.and(column, direction);
            }
            return this;
        }

        public Pager build() {
            return new Pager(this.page, this.itemsPerPage, this.sort);
        }

    }

}
