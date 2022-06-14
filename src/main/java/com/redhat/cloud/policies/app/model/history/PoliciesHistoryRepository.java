package com.redhat.cloud.policies.app.model.history;

import com.redhat.cloud.policies.app.model.filter.Filter;
import com.redhat.cloud.policies.app.model.pager.Pager;
import io.quarkus.panache.common.Sort;
import org.hibernate.Session;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.redhat.cloud.policies.app.model.filter.Filter.Operator.LIKE;

@ApplicationScoped
public class PoliciesHistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(PoliciesHistoryRepository.class);

    @Inject
    Session session;

    public long countOrgId(String orgId, UUID policyId, Pager pager) {
        // Base HQL query.
        String hql = "SELECT COUNT(*) FROM PoliciesHistoryEntry WHERE orgId = :orgId AND policyId = :policyId";

        hql = addFiltersConditions(hql, pager.getFilter().getItems());

        LOGGER.tracef("HQL query ready to be executed: %s", hql);

        TypedQuery<Long> query = session.createQuery(hql, Long.class)
                .setParameter("orgId", orgId)
                .setParameter("policyId", policyId.toString());

        setFiltersValues(query, pager.getFilter().getItems());

        return query.getSingleResult();
    }

    public long count(String tenantId, UUID policyId, Pager pager) {
        // Base HQL query.
        String hql = "SELECT COUNT(*) FROM PoliciesHistoryEntry WHERE tenantId = :tenantId AND policyId = :policyId";

        hql = addFiltersConditions(hql, pager.getFilter().getItems());

        LOGGER.tracef("HQL query ready to be executed: %s", hql);

        TypedQuery<Long> query = session.createQuery(hql, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("policyId", policyId.toString());

        setFiltersValues(query, pager.getFilter().getItems());

        return query.getSingleResult();
    }

    public List<PoliciesHistoryEntry> find(String tenantId, UUID policyId, Pager pager) {
        // Base HQL query.
        String hql = "FROM PoliciesHistoryEntry WHERE tenantId = :tenantId AND policyId = :policyId";

        hql = addFiltersConditions(hql, pager.getFilter().getItems());

        // The sorts from the pager are added to the HQL query.
        if (!pager.getSort().getColumns().isEmpty()) {
            List<String> orderByItems = new ArrayList<>();
            for (Sort.Column column : pager.getSort().getColumns()) {
                getEntityFieldName(column.getName()).ifPresent(entityFieldName -> {
                    String sortDirection = getSortDirection(column.getDirection());
                    orderByItems.add(entityFieldName + " " + sortDirection);
                });
            }
            if (!orderByItems.isEmpty()) {
                hql += " ORDER BY " + String.join(", ", orderByItems);
            }
        } else {
            hql += " ORDER BY ctime DESC, hostName ASC";
        }

        LOGGER.tracef("HQL query ready to be executed: %s", hql);

        TypedQuery<PoliciesHistoryEntry> query = session.createQuery(hql, PoliciesHistoryEntry.class)
                .setParameter("tenantId", tenantId)
                .setParameter("policyId", policyId.toString());

        setFiltersValues(query, pager.getFilter().getItems());

        if (pager.getLimit() > 0) {
            query.setMaxResults(pager.getLimit());
        }
        if (pager.getOffset() > 0) {
            query.setFirstResult(pager.getOffset());
        }

        return query.getResultList();
    }

    private static String addFiltersConditions(String hql, List<Filter.FilterItem> filterItems) {
        // The filters from the pager are added to the HQL query.
        for (Filter.FilterItem filterItem : filterItems) {
            String entityFieldName = getEntityFieldName(filterItem);
            String operator = getHqlOperator(filterItem);
            // To be consistent with the previous implementation, the condition is always case-insensitive.
            hql += " AND LOWER(" + entityFieldName + ")" + operator + ":" + entityFieldName;
        }
        return hql;
    }

    private static void setFiltersValues(Query query, List<Filter.FilterItem> filterItems) {
        for (Filter.FilterItem filterItem : filterItems) {
            String paramName = getEntityFieldName(filterItem);
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
    private static String getEntityFieldName(Filter.FilterItem filterItem) {
        switch (filterItem.field) {
            case "id":
                return "hostId";
            case "name":
                return "hostName";
            case "ctime":
                return "ctime";
            default:
                throw new IllegalArgumentException("Unknown filter field: " + filterItem.field);
        }
    }

    private static String getHqlOperator(Filter.FilterItem filterItem) {
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

    private static Optional<String> getEntityFieldName(String sortColumn) {
        switch (sortColumn) {
            case "id":
                return Optional.of("hostId");
            case "name":
                return Optional.of("hostName");
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
