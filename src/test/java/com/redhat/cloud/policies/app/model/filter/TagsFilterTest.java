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

import com.redhat.cloud.policies.app.model.pager.Pager;
import com.redhat.cloud.policies.app.rest.utils.PagingUtils;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * @author hrupp
 */
public class TagsFilterTest {

  @Test
  void filter1() throws URISyntaxException {
    UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM"));
    Pager pager = PagingUtils.extractPager(info);
    String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
    assertEquals("display_name = 'VM'", query);
  }

  @Test
  void filter2() throws URISyntaxException {
    UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM&filter:op[name]=not_equal"));
    Pager pager = PagingUtils.extractPager(info);
    String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
    assertEquals("display_name != 'VM'", query);
  }

  @Test
  void filter3() throws URISyntaxException {
    UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM&filter:op[name]=like"));
    Pager pager = PagingUtils.extractPager(info);
    String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
    assertEquals("display_name = '.*VM.*'", query);
  }


  @Test
  void filter4() throws URISyntaxException {
    UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM&filter[id]=123"));
    Pager pager = PagingUtils.extractPager(info);
    String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
    assertEquals("display_name = 'VM' AND inventory_id = '123'", query);
  }

  @Test
  void filter4_2() throws URISyntaxException {
    UriInfo info = new ResteasyUriInfo(
        new URI("https://foo?filter[name]=VM&filter[id]=123&filter:op[name]=not_equal"));
    Pager pager = PagingUtils.extractPager(info);
    String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
    assertEquals("display_name != 'VM' AND inventory_id = '123'", query);
  }

  @Test
  void filter5() throws URISyntaxException {
    UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[id]=123-45&filter:op[id]=like"));
    Pager pager = PagingUtils.extractPager(info);
    String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
    assertEquals("inventory_id = '.*123-45.*'", query);
  }

  @Test
  void filter6() throws URISyntaxException {
    UriInfo info = new ResteasyUriInfo(new URI("https://foo?filter[name]=VM&filter:op[id]=not_equal"));
    Pager pager = PagingUtils.extractPager(info);
    String query = PolicyHistoryTagFilterHelper.getTagsFilterFromPager(pager);
    assertEquals("display_name = 'VM'", query);
  }

}
