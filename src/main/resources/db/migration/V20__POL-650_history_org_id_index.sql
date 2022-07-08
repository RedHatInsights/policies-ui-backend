-- This will enable index-only scans when Postgres retrieves the policies history data.
CREATE INDEX ix_policies_history_index_only_scan
    ON policies_history (policy_id, org_id, tenant_id, ctime, host_id, host_name, id);
