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

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

public class PagingUtils {

    private PagingUtils() {

    }

    public static Pager extractPager(@NotNull UriInfo uriInfo) {
        Pager.PagerBuilder pageBuilder = Pager.builder();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        try {
            pageBuilder.page(Integer.parseInt(queryParams.getFirst("page")));
        } catch (NumberFormatException nfe) {
            // Ignore the exception, the builder will use the default value
        }

        try {
            pageBuilder.itemsPerPage(Integer.parseInt(queryParams.getFirst("pageSize")));
        } catch (NumberFormatException nfe) {
            // Ignore the exception, the builder will use the default value
        }

        return pageBuilder.build();
    }

    public static <T>ResponseBuilder responseBuilder(Page<T> page) {
        ResponseBuilder builder;

        if (page.isEmpty()) {
            builder = Response.status(Response.Status.NO_CONTENT);
        } else {
            builder = Response.ok(page);
            EntityTag etag = new EntityTag(String.valueOf(page.hashCode()));
            builder.header("ETag",etag);
            builder.header("TotalCount", Long.toString(page.getTotalCount()));
        }

        return builder;
    }
}
