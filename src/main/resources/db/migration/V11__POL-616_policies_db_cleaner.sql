-- This stored procedure deletes the policies history entries that are no longer needed.
-- It is executed from an OpenShift CronJob.
CREATE PROCEDURE cleanPoliciesHistory() AS $$
DECLARE
deleted INTEGER;
BEGIN
    RAISE INFO '% Policies history purge starting. Entries older than 14 days will be deleted.', NOW();
DELETE FROM policies_history WHERE ctime < EXTRACT(EPOCH FROM NOW() AT TIME ZONE 'UTC' - INTERVAL '14 days') * 1000;
GET DIAGNOSTICS deleted = ROW_COUNT;
RAISE INFO '% Policies history purge ended. % entries were deleted from the database.', NOW(), deleted;
END;
$$ LANGUAGE PLPGSQL;
