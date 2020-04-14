-- the tables have been set up by the server already
-- so only populate test data
-- V999 should be high enough to always be processed last after all other migrations
--    from the application code

-- next ones are used in tests with specific IDs
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c', '1234', '1st policy', 'Just a test', true, '"cores" == 1','EMAIL roadrunner@acme.org');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('e3bdc9dd-18d4-4900-805d-7f59b3c736f7', '1234', '3rd policy', 'Another test', true, '"rhelversion" >= "8" OR "cores" == 5','EMAIL toor@root.org');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('c49e92c4-764c-4163-9200-245b31933e94', '1234', '2nd policy', 'Another test', false, '"cpu" != "intel"','webhook');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('f36aa564-ffc8-48c6-a27f-31ddd4c16c8b', '1234', 'Detect Nice box', 'Test for os and arch', true, '"os_version" == "7.5" AND "arch" == "x86_64"', 'email');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('cd6cceb8-65dd-4988-a566-251fd20d7e2c', '1234', '4th policy', 'Test for account2', true, '"cores" > 4','email');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('c49e92c4-dead-beef-9200-245b31933e94', '1234', '4th policy-2', 'Test for account2', true, '"cores" > 4','webhook');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('9b3b4429-1393-4120-95da-54c17a512367', '1234', '5th policy', 'Test for account2', true, '"cores" > 4','email');
-- end of test ones with specific id





insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('46534b18-0b00-4090-afc1-fdf04fe195a7', '1234', '6th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('867ce882-7b6a-4795-bef2-9ec2d6746f69', '1234', '7th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('841efb07-88c0-4515-8311-06fc356da20b', '1234', '8th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('f7577cce-bae9-4a5d-a304-c8a4cecec817', '1234', '9th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy (id, customerId, name, description, is_enabled, conditions, actions) values ('0b617670-c0f7-4f85-bf05-cae4da43a886', '1234', 'Xth policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');


