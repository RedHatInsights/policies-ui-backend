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
package com.redhat.cloud.custompolicies.app.rest.utils;

import com.redhat.cloud.custompolicies.app.model.pager.Page;
import com.redhat.cloud.custompolicies.app.model.pager.Pager;
import io.quarkus.panache.common.Sort;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;

public class PagingUtils {

    private PagingUtils() {

    }

    public static Pager extractPager(@NotNull UriInfo uriInfo) {
        Pager.PagerBuilder pageBuilder = Pager.builder();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        final String QUERY_PAGE = "page";
        final String QUERY_PAGE_SIZE = "pageSize";
        final String QUERY_COLUMN = "sortColumn";
        final String QUERY_DIRECTION = "sortDirection";

        String page = queryParams.getFirst(QUERY_PAGE);
        if (page != null) {
            try {
                pageBuilder.page(Integer.parseInt(page));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(String.format(
                        "%s expects an int but found [%s]",
                        QUERY_PAGE,
                        page
                ), nfe);
            }
        }

        String itemsPerPage = queryParams.getFirst(QUERY_PAGE_SIZE);
        if (itemsPerPage != null) {
            try {
                pageBuilder.itemsPerPage(Integer.parseInt(itemsPerPage));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(String.format(
                        "%s expects an int but found [%s]",
                        QUERY_PAGE_SIZE,
                        itemsPerPage
                ), nfe);
            }
        }

        List<String> columns = queryParams.get(QUERY_COLUMN);
        List<String> directions = queryParams.get(QUERY_DIRECTION);
        if (columns != null) {
            for (int i = 0; i < columns.size(); ++i) {
                String column = columns.get(i);
                Sort.Direction direction = Sort.Direction.Ascending;
                if (directions != null && i < directions.size()) {
                    switch(directions.get(i).toLowerCase()) {
                        case "asc":
                            direction = Sort.Direction.Ascending;
                            break;
                        case "desc":
                            direction = Sort.Direction.Descending;
                            break;
                        default:
                            throw new IllegalArgumentException("Unexpected sort order found: [" + columns.get(i) + "]");
                    }
                }
                pageBuilder.addSort(column, direction);
            }
        }
        else {
            // default sort is by mtime descending, so that newest end up on top
            pageBuilder.addSort("mtime",Sort.Direction.Descending);
        }

        return pageBuilder.build();
    }

    public static <T>ResponseBuilder responseBuilder(Page<T> page) {
        ResponseBuilder builder;

        if (page.isEmpty()) {
            builder = Response.status(Response.Status.NOT_FOUND);
        } else {
            builder = Response.ok(page);
            EntityTag etag = new EntityTag(String.valueOf(page.hashCode()));
            builder.header("ETag",etag);
            builder.header("TotalCount", Long.toString(page.getTotalCount()));
        }

        return builder;
    }
}
