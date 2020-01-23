
-- add a last updated column
alter table policy add column mtime TIMESTAMP default now();

update policy set mtime = now() where mtime is null;
