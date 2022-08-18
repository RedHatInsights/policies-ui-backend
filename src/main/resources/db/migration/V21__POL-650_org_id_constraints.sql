DROP INDEX pol_cus_nam_idx;

ALTER TABLE policy
    DROP CONSTRAINT policy_pkey,
    ALTER COLUMN customerid DROP NOT NULL,
    ALTER COLUMN org_id SET NOT NULL,
    ADD CONSTRAINT policy_pkey PRIMARY KEY (org_id, id);

CREATE UNIQUE INDEX policy_org_id_name_idx ON policy (org_id, name);

ALTER TABLE policies_history
    ALTER COLUMN tenant_id DROP NOT NULL,
    ALTER COLUMN org_id SET NOT NULL;
