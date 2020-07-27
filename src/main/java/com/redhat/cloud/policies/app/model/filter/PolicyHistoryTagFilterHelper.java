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

import java.util.Iterator;
import java.util.List;

import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.LIKE;

/**
 * @author hrupp
 */
public class PolicyHistoryTagFilterHelper {

  public static String getTagsFilterFromPager(Pager pager) {
    StringBuilder sb = new StringBuilder();
    Filter filter = pager.getFilter();
    List<Filter.FilterItem> items = filter.getItems();
    Iterator<Filter.FilterItem> iterator = items.iterator();
    while(iterator.hasNext()) {
      Filter.FilterItem item = iterator.next();

      switch (item.field) {
        case "name":
          sb.append("display_name");
          break;
        case "id":
          sb.append("inventory_id");
          break;
        default:
          throw new IllegalArgumentException("Unknown filter field: " + item.field);
      }
      sb.append(' ');
      switch (item.operator) {
        case EQUAL:
        case LIKE:
          sb.append("=");
          break;
        case NOT_EQUAL:
          sb.append("!=");
          break;
        default:
          throw new IllegalArgumentException("Unknown operator: " + item.operator.toString());
      }
      sb.append(" '");
      if (item.operator.equals(LIKE)) {
        sb.append(".*");
      }
      sb.append(item.value);
      if (item.operator.equals(LIKE)) {
        sb.append(".*");
      }
      sb.append("'");
      if (iterator.hasNext()) {
        sb.append(" AND ");
      }
    }

    return sb.length() == 0 ? null : sb.toString();
  }

}
