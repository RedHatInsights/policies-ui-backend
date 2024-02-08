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

import static java.lang.Integer.max;

import com.redhat.cloud.policies.app.model.pager.Page;
import com.redhat.cloud.policies.app.model.pager.Pager;
import com.redhat.cloud.policies.app.model.filter.Filter;
import io.quarkus.panache.common.Sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PagingUtils {

    final static String QUERY_OFFSET = "offset";
    final static String QUERY_LIMIT = "limit";
    final static String QUERY_COLUMN = "sortColumn";
    final static String QUERY_DIRECTION = "sortDirection";
    final static Pattern FILTER_PATTERN = Pattern.compile("^filter\\[(.+)\\]$");
    final static String FILTER_OP = "filter:op";

    private final Pager.PagerBuilder pageBuilder;

    private PagingUtils() {
        this.pageBuilder = Pager.builder();
    }

    public static Pager extractPager(UriInfo uriInfo) {
        return new PagingUtils().extract(uriInfo);
    }

    Pager extract(UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

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
                    String dir = directions.get(i);
                    direction = getDirection(dir, columns.get(i));
                }
                pageBuilder.addSort(column, direction);
            }
        } else {
            // default sort is by mtime descending, so that newest end up on top
            Sort.Direction direction = Sort.Direction.Descending;

            // check if the user specified a sort order (but no column, so mtime is meant)
            if (directions != null && directions.size() > 0) {
                direction = getDirection(directions.get(0), "mtime");
            }

            pageBuilder.addSort("mtime", direction);
        }

        for (String key : queryParams.keySet()) {
            Matcher filterMatcher = FILTER_PATTERN.matcher(key);
            if (filterMatcher.find()) {
                String column = filterMatcher.group(1);
                String value = queryParams.getFirst(key);
                Filter.Operator operator;
                if (column.equals("is_enabled")) {
                    column = "isEnabled";
                    operator = Filter.Operator.BOOLEAN_IS;
                    if (value == null || value.isEmpty()) {
                        value = "true";
                    } else {
                      if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("Bad value for filter[isEnabled]");
                      }
                      else {
                        value = String.valueOf(Boolean.parseBoolean(value));
                      }
                    }

                } else {
                    String operatorString = queryParams.getFirst(String.format("%s[%s]", FILTER_OP, column));
                    operator = Filter.Operator.EQUAL;
                    if (operatorString != null) {
                        if (operatorString.equalsIgnoreCase(Filter.Operator.BOOLEAN_IS.name())) {
                            throw new IllegalArgumentException("Invalid filter: Column [" + column + "] does not allow boolean_is");
                        }
                        operator = Filter.Operator.fromName(operatorString);
                    }
                }
                pageBuilder.filter(column, operator, value);
            }
        }

        return pageBuilder.build();
    }

    /**
     * Obtain the direction from the passed direction string. This string is
     * case insensitive and only the first 3 chars count. So 'asc' and 'ascending'
     * and 'Ascending' (or 'des*' will be ok)
     *
     * @param directionString Incoming direction string.
     * @param column          Column name for display purposes in case the passed direction is not valid
     * @return Direction
     */
    static Sort.Direction getDirection(String directionString, String column) {
        Sort.Direction direction;
        switch (directionString.toLowerCase().substring(0, 3)) {
            case "asc":
                direction = Sort.Direction.Ascending;
                break;
            case "des":
                direction = Sort.Direction.Descending;
                break;
            default:
                throw new IllegalArgumentException("Unexpected sort order found: [" + column + "]");
        }
        return direction;
    }

    public static <T> ResponseBuilder responseBuilder(Page<T> page) {
        ResponseBuilder builder;

        if (page.isEmpty()) {
            builder = Response.status(Response.Status.NOT_FOUND);
        } else {
            builder = Response.ok(new PagedResponse<>(page));
            EntityTag etag = new EntityTag(String.valueOf(page.hashCode()));
            builder.header("ETag", etag);
            builder.header("TotalCount", Long.toString(page.getTotalCount()));
        }

        return builder;
    }

    /**
     * Provide a paged response in the desired format.
     * Links need to look like:<br/>
     *
     * <pre>
     * "first": "/api/myapp/v1/collection/?limit=5&offset=0",
     * "last": "/api/myapp/v1/collection/?limit=5&offset=10",
     * "next": "/api/myapp/v1/collection/?limit=5&offset=10",
     * "prev": "/api/myapp/v1/collection/?limit=5&offset=0"
     * </pre>
     */
    public static class PagedResponse<T> {
        public Meta meta;
        public Map<String, String> links = new HashMap<>(3);
        public List<T> data = new ArrayList<>();

        public PagedResponse(Page<T> page) {
            meta = new Meta(page.getTotalCount());
            data.addAll(page);


            String location = "/api/policies/v1.0/policies";
            String format = "%s?limit=%d&offset=%d";

            Pager pager = page.getPager();
            int limit = pager.getLimit();
            links.put("first", String.format(format, location, limit, 0));
            if (limit == Pager.NO_LIMIT) {
                links.put("last", String.format(format, location, limit, 0));
            } else {
                long offset;
                if (page.getTotalCount() % limit == 0) {
                    offset = page.getTotalCount() - pager.getLimit();
                } else {
                    offset = (page.getTotalCount() / limit) * limit;
                }
                links.put("last", String.format(format, location, limit, offset));
            }
            if (limit != Pager.NO_LIMIT) {
                if (pager.getOffset() < page.getTotalCount() - limit) {
                    links.put("next", String.format(format, location, limit, pager.getOffset() + limit));
                }
                if (pager.getOffset() > 0) {
                    links.put("prev", String.format(format, location, limit,
                            max(0, pager.getOffset() - limit)));
                }
            }
        }
    }

    public static class Meta {
        public long count;

        public Meta(long count) {
            this.count = count;
        }
    }
}
