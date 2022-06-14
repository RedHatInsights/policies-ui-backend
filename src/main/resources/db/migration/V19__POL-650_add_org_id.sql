ALTER TABLE policies_history
  ADD COLUMN org_id text;

CREATE INDEX ix_policies_history_org_id ON public.policies_history (org_id);