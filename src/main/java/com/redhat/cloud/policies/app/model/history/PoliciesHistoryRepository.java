package com.redhat.cloud.policies.app.model.history;

import com.redhat.cloud.policies.app.model.filter.Filter;
import com.redhat.cloud.policies.app.model.pager.Pager;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.LongType;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.hibernate.Session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Table;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.LIKE;

@ApplicationScoped
public class PoliciesHistoryRepository {

    @Inject
    Session session;

    private static final String tableName = PoliciesHistoryEntry.class.getAnnotation(Table.class).name();

    public long count(String orgId, List<UUID> hostGroupIds, UUID policyId, Pager pager) {
        // Base SQL query.
        String sql = String.format("SELECT COUNT(*) AS count FROM %s WHERE org_id = :orgId AND policy_id = :policyId",
                                   tableName);

        sql = addHostGroupsConditions(sql, hostGroupIds);
        sql = addFiltersConditions(sql, pager.getFilter().getItems());

        Log.tracef("SQL query ready to be executed: %s", sql);

        NativeQuery<?> query = session.createNativeQuery(sql)
                .addScalar("count", LongType.INSTANCE)
                .setParameter("orgId", orgId)
                .setParameter("policyId", policyId.toString());

        setHostGroupsValues(query, hostGroupIds);
        setFiltersValues(query, pager.getFilter().getItems());

        return (Long) query.getSingleResult();
    }

    public List<PoliciesHistoryEntry> find(String orgId,  List<UUID> hostGroupIds, UUID policyId, Pager pager) {
        // Base SQL query.
        String sql = String.format("SELECT * FROM %s WHERE org_id = :orgId AND policy_id = :policyId",
                                   tableName);

        sql = addHostGroupsConditions(sql, hostGroupIds);
        sql = addFiltersConditions(sql, pager.getFilter().getItems());

        // The sorts from the pager are added to the HQL query.
        if (!pager.getSort().getColumns().isEmpty()) {
            List<String> orderByItems = new ArrayList<>();
            for (Sort.Column column : pager.getSort().getColumns()) {
                getSortFieldName(column.getName()).ifPresent(entityFieldName -> {
                    String sortDirection = getSortDirection(column.getDirection());
                    orderByItems.add(entityFieldName + " " + sortDirection);
                });
            }
            if (!orderByItems.isEmpty()) {
                sql += " ORDER BY " + String.join(", ", orderByItems);
            }
        } else {
            sql += " ORDER BY ctime DESC, host_name ASC";
        }

        Log.tracef("SQL query ready to be executed: %s", sql);

        NativeQuery<PoliciesHistoryEntry> query = session
                .createNativeQuery(sql, PoliciesHistoryEntry.class)
                .setParameter("orgId", orgId)
                .setParameter("policyId", policyId.toString());

        setHostGroupsValues(query, hostGroupIds);
        setFiltersValues(query, pager.getFilter().getItems());

        if (pager.getLimit() > 0) {
            query.setMaxResults(pager.getLimit());
        }
        if (pager.getOffset() > 0) {
            query.setFirstResult(pager.getOffset());
        }

        return query.getResultList();
    }

    private static String addHostGroupsConditions(String sql, List<UUID> hostGroupIds) {
        if (hostGroupIds == null) {
            return sql;
        }

        List<String> conds = new ArrayList<String>();
        conds.add("1=0");
        int num = 0;
        for (UUID hostGroupId : hostGroupIds) {
            if (hostGroupId == null) {
                conds.add("host_groups = '[]'");
            } else {
                num++;
                conds.add(String.format("host_groups @> CAST(:hostGroup%d AS jsonb)", num));
            }
        }
        return String.format("%s AND (%s)", sql, String.join(" OR ", conds));
    }

    private static void setHostGroupsValues(Query query, List<UUID> hostGroupIds) {
        if (hostGroupIds == null) {
            return;
        }

        int num = 0;
        for (UUID hostGroupId : hostGroupIds) {
            if (hostGroupId != null) {
                num++;
                JsonArray value = JsonArray.of(JsonObject.of("id", hostGroupId.toString()));
                query.setParameter(String.format("hostGroup%d", num), value.toString());
            }
        }
    }

    private static String addFiltersConditions(String sql, List<Filter.FilterItem> filterItems) {
        // The filters from the pager are added to the HQL query.
        for (Filter.FilterItem filterItem : filterItems) {
            String fieldName = getFieldName(filterItem);
            String operator = getOperator(filterItem);
            // To be consistent with the previous implementation, the condition is always case-insensitive.
            sql += " AND LOWER(" + fieldName + ")" + operator + ":" + fieldName;
        }
        return sql;
    }

    private static void setFiltersValues(Query query, List<Filter.FilterItem> filterItems) {
        for (Filter.FilterItem filterItem : filterItems) {
            String paramName = getFieldName(filterItem);
            String paramValue = filterItem.value.toString().toLowerCase();
            if (filterItem.operator == LIKE) {
                paramValue = "%" + paramValue + "%";
            }
            query.setParameter(paramName, paramValue);
        }
    }

    /*
     * The following static methods may look like simple mappers, but some of them are also used to prevent SQL
     * injections by whitelisting field names.
     */
    private static String getFieldName(Filter.FilterItem filterItem) {
        switch (filterItem.field) {
            case "id":
                return "host_id";
            case "name":
                return "host_name";
            case "ctime":
                return "ctime";
            default:
                throw new IllegalArgumentException("Unknown filter field: " + filterItem.field);
        }
    }

    private static String getOperator(Filter.FilterItem filterItem) {
        switch (filterItem.operator) {
            case EQUAL:
                return " = ";
            case LIKE:
                return " LIKE ";
            case NOT_EQUAL:
                return " <> ";
            default:
                throw new IllegalArgumentException("Unknown operator: " + filterItem.operator);
        }
    }

    private static Optional<String> getSortFieldName(String sortColumn) {
        switch (sortColumn) {
            case "id":
                return Optional.of("host_id");
            case "name":
                return Optional.of("host_name");
            case "ctime":
                return Optional.of("ctime");
            case "mtime":
                // Pager may contain a default sort on `mtime` which is not a PoliciesHistoryEntry field.
                return Optional.empty();
            default:
                throw new IllegalArgumentException("Unknown sort column: " + sortColumn);

        }
    }

    private static String getSortDirection(Sort.Direction direction) {
        switch (direction) {
            case Ascending:
                return "ASC";
            case Descending:
                return "DESC";
            default:
                throw new IllegalArgumentException("Unknown sort direction: " + direction);
        }
    }
}
