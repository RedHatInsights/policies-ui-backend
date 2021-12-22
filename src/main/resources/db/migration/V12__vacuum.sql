-- This script will reclaim storage occupied by dead tuples, then collect statistics that will help the query planner
-- determine the most efficient execution plan for queries.
-- See https://www.postgresql.org/docs/13/sql-vacuum.html and https://www.postgresql.org/docs/13/sql-analyze.html for more details.

VACUUM ANALYZE policy;
VACUUM ANALYZE policies_history;
