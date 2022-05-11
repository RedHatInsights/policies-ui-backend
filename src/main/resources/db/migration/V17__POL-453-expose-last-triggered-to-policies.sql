ALTER TABLE policy ADD column last_triggered bigint default 0;

UPDATE policy as p SET last_triggered = (SELECT MAX(ctime) FROM policies_history as ph WHERE p.id = uuid(ph.policy_id));

CREATE FUNCTION update_last_triggered() RETURNS TRIGGER AS $update_last_triggered$
    BEGIN
        UPDATE policy SET last_triggered = GREATEST(last_triggered, new.ctime) WHERE id = uuid(new.policy_id);
        RETURN NEW;
    END;
$update_last_triggered$ LANGUAGE plpgsql;

CREATE TRIGGER policies_update_last_triggered AFTER INSERT ON policies_history
    FOR EACH ROW EXECUTE PROCEDURE update_last_triggered();
