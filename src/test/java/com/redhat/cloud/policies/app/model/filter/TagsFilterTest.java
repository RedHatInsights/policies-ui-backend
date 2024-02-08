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
package com.redhat.cloud.policies.app.model.filter;

import com.redhat.cloud.policies.app.model.ColumnGetter;
import com.redhat.cloud.policies.app.model.ColumnInfo;
import com.redhat.cloud.policies.app.model.pager.Pager;
import com.redhat.cloud.policies.app.rest.utils.PagingUtils;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TagsFilterTest {

    ColumnGetter columnGetter;

    @BeforeEach
    void mockColumnGetter() {
        this.columnGetter = mock(ColumnGetter.class);

        when(columnGetter.get(eq("name"))).thenReturn(new ColumnInfo("name", "name", true, true));
        when(columnGetter.get(eq("id"))).thenReturn(new ColumnInfo("id", "id", true, true));
    }

    @Test
    void filter1() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM"));
        Pager pager = PagingUtils.extractPager(info, columnGetter);
        String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
        assertEquals("tags.display_name = 'vm'", query);
    }

    @Test
    void filter2() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM&filter:op[name]=not_equal"));
        Pager pager = PagingUtils.extractPager(info, columnGetter);
        String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
        assertEquals("tags.display_name != 'vm'", query);
    }

    @Test
    void filter3() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM&filter:op[name]=like"));
        Pager pager = PagingUtils.extractPager(info, columnGetter);
        String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
        assertEquals("tags.display_name MATCHES '*vm*'", query);
    }

    @Test
    void filter4() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM&filter[id]=123"));
        Pager pager = PagingUtils.extractPager(info, columnGetter);
        String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
        assertEquals("tags.display_name = 'vm' AND tags.inventory_id = '123'", query);
    }

    @Test
    void filter4_2() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(
                new URI("https://foo?filter[name]=VM&filter[id]=123&filter:op[name]=not_equal"));
        Pager pager = PagingUtils.extractPager(info, columnGetter);
        String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
        assertEquals("tags.display_name != 'vm' AND tags.inventory_id = '123'", query);
    }

    @Test
    void filter5() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[id]=123-45&filter:op[id]=like"));
        Pager pager = PagingUtils.extractPager(info, columnGetter);
        String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
        assertEquals("tags.inventory_id MATCHES '*123-45*'", query);
    }

    @Test
    void filter6() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM&filter:op[id]=not_equal"));
        Pager pager = PagingUtils.extractPager(info, columnGetter);
        String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
        assertEquals("tags.display_name = 'vm'", query);
    }

    @Test
    void filter7() throws URISyntaxException {
        UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=toLowerCaseString&filter:op[name]=like"));
        Pager pager = PagingUtils.extractPager(info, columnGetter);
        String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
        assertEquals("tags.display_name MATCHES '*tolowercasestring*'", query);
    }
}
