-- the tables have been set up by the server already
-- so only populate test data
-- V999 should be high enough to always be processed last after all other migrations
--    from the application code

insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c', '1234', '1st policy', 'Just a test', true, '"cores" == 1','EMAIL roadrunner@acme.org');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', '2nd policy', 'Another test', false, '"cpu" != "intel"','HOOK http://localhost:8080');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('e3bdc9dd-18d4-4900-805d-7f59b3c736f7', '1234', '3rd policy', 'Another test', true, '"rhelversion" >= "8" OR "cores" == 5','EMAIL toor@root.org');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', '4th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', 'Detect Nice box', 'Test for os and arch', true, '"os_version" == "7.5" AND "arch" == "x86_64"', 'NOTIFY; EMAIL foo@acme.org');

insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', '5th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', '6th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', '7th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', '8th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', '9th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values (uuid_generate_v4(), '1234', 'Xth policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');


