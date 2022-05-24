ALTER TABLE policy ADD COLUMN last_triggered bigint DEFAULT 0 NOT NULL;

CREATE RULE rule_policies_history_last_triggered AS ON INSERT TO policies_history
DO UPDATE policy
   SET last_triggered = GREATEST(last_triggered, NEW.ctime)
   WHERE id = uuid(NEW.policy_id) and customerid = NEW.tenant_id;

-- Inits the last_triggered
UPDATE policy as p SET last_triggered = (
    SELECT MAX(ctime)
    FROM policies_history AS ph
    WHERE p.id = uuid(ph.policy_id) AND p.customerid = ph.tenant_id
);
