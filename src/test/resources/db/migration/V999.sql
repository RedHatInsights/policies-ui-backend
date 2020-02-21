-- the tables have been set up by the server already
-- so only populate test data
-- V999 should be high enough to always be processed last after all other migrations
--    from the application code
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (1, '1234', '1st policy', 'Just a test', true, '"cores" == 1','EMAIL roadrunner@acme.org','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (2, '1234', '2nd policy', 'Another test', false, '"cpu" != "intel"','HOOK http://localhost:8080','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (3, '1234', '3rd policy', 'Another test', true, '"rhelversion" >= "8" OR "cores" == 5','EMAIL toor@root.org','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (4, '1234', '4th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (5, '1234', 'Detect Nice box', 'Test for os and arch', true, '"os_version" == "7.5" AND "arch" == "x86_64"', 'NOTIFY; EMAIL foo@acme.org','123-abc');

insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (6, '1234', '5th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (7, '1234', '6th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (8, '1234', '7th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (9, '1234', '8th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (10, '1234', '9th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel','123-abc');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions, triggerId) values (11, '1234', 'Xth policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel','123-abc');


