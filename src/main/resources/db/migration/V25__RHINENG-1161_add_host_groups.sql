ALTER TABLE policies_history ADD column host_groups jsonb NOT NULL DEFAULT '[]'::jsonb;
CREATE INDEX policies_history_host_groups_idx ON policies_history USING gin (host_groups jsonb_path_ops);
