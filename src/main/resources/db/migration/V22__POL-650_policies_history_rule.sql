DROP RULE rule_policies_history_last_triggered ON policies_history;

CREATE RULE rule_policies_history_last_triggered AS ON INSERT TO policies_history
DO UPDATE policy
   SET last_triggered = GREATEST(last_triggered, NEW.ctime)
   WHERE id = uuid(NEW.policy_id) and org_id = NEW.org_id;
