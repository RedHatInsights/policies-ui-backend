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
package com.redhat.cloud.policies.app.rest.utils;

import static java.lang.Long.max;

import com.redhat.cloud.policies.app.model.pager.Page;
import com.redhat.cloud.policies.app.model.pager.Pager;
import com.redhat.cloud.policies.app.model.filter.Filter;
import io.quarkus.panache.common.Sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PagingUtils {

    private PagingUtils() {

    }

    public static Pager extractPager(@NotNull UriInfo uriInfo) {
        Pager.PagerBuilder pageBuilder = Pager.builder();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        final String QUERY_OFFSET = "offset";
        final String QUERY_LIMIT = "limit";
        final String QUERY_COLUMN = "sortColumn";
        final String QUERY_DIRECTION = "sortDirection";
        final Pattern FILTER_PATTERN = Pattern.compile("filter\\[(.+)\\]");
        final String FILTER_OP = "filter:op";

        String itemsPerPage = queryParams.getFirst(QUERY_LIMIT);
        boolean usingNoLimit = false;
        if (itemsPerPage != null) {
            try {
                int limit = Integer.parseInt(itemsPerPage);
                if (limit == Pager.NO_LIMIT) {
                    usingNoLimit = true;
                    pageBuilder.page(0);
                }
                pageBuilder.itemsPerPage(limit);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(String.format(
                        "%s expects an int but found [%s]",
                        QUERY_LIMIT,
                        itemsPerPage
                ), nfe);
            }
        }

        if (!usingNoLimit) {
            String page = queryParams.getFirst(QUERY_OFFSET);
            if (page != null) {
                try {
                    pageBuilder.page(Integer.parseInt(page));
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(String.format(
                            "%s expects an int but found [%s]",
                            QUERY_OFFSET,
                            page
                    ), nfe);
                }
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

        for (String key: queryParams.keySet()) {
            Matcher filterMatcher = FILTER_PATTERN.matcher(key);
            if (filterMatcher.find()) {
                String column = filterMatcher.group(1);
                String value = queryParams.getFirst(key);
                String operatorString = queryParams.getFirst(String.format("%s[%s]", FILTER_OP, column));
                Filter.Operator operator = Filter.Operator.EQUAL;
                if (operatorString != null) {
                    operator = Filter.Operator.fromName(operatorString);
                }
                pageBuilder.filter(column, operator, value);
            }
        }

        return pageBuilder.build();
    }

    public static <T>ResponseBuilder responseBuilder(Page<T> page) {
        ResponseBuilder builder;

        if (page.isEmpty()) {
            builder = Response.status(Response.Status.NOT_FOUND);
        } else {
            builder = Response.ok(new PagedResponse<>(page));
            EntityTag etag = new EntityTag(String.valueOf(page.hashCode()));
            builder.header("ETag",etag);
            builder.header("TotalCount", Long.toString(page.getTotalCount()));
        }

        return builder;
    }

    /**
     * Provide a paged response in the desired format.
     * Links need to look like:<br/>
     *
     <pre>
    "first": "/api/myapp/v1/collection/?limit=5&offset=0",
    "last": "/api/myapp/v1/collection/?limit=5&offset=10",
    "next": "/api/myapp/v1/collection/?limit=5&offset=10",
    "prev": "/api/myapp/v1/collection/?limit=5&offset=0"
     </pre>
     */
    public static class PagedResponse<T> {
      public Map<String,Long> meta = new HashMap<>(1);
      public Map<String,String> links = new HashMap<>(3);
      public List<T> data = new ArrayList<>();

        public PagedResponse(Page<T> page) {
            meta.put("count", page.getTotalCount());
            data.addAll(page);


            String location = "/api/policies/v1.0/policies";
            String format = "%s?limit=%d&offset=%d";

            Pager pager = page.getPager();
            int limit = pager.getLimit();
            links.put("first", String.format(format, location, limit, 0));
            if (limit == Pager.NO_LIMIT) {
                links.put("last", String.format(format, location, limit, 0));
            } else {
                links.put("last", String.format(format, location, limit, (page.getTotalCount() / limit) * limit));
            }
            if (limit != Pager.NO_LIMIT) {
                if (pager.getOffset() < page.getTotalCount() - limit) {
                    links.put("next", String.format(format, location, limit, pager.getOffset() + limit));
                }
                if (pager.getOffset() > 0) {
                    links.put("prev", String.format(format, location, limit,
                            max(0,pager.getOffset() - limit)));
                }
            }
        }
    }
}
