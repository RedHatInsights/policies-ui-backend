CREATE INDEX ix_policies_history_policy_id_org_id
    ON policies_history (policy_id, org_id);
