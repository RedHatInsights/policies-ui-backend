create user sarah password 'connor';
create table fact (id integer primary key, name varchar unique not null , type varchar);
ALTER TABLE fact OWNER TO sarah;
insert into fact VALUES(10, 'last_boot_time', 'STRING');
insert into fact VALUES(11, 'enabled_services', 'LIST');
insert into fact VALUES(12, 'number_of_sockets', 'INT');
insert into fact VALUES(13, 'insights_egg_version', 'STRING');
insert into fact VALUES(14, 'running_processes', 'LIST');
insert into fact VALUES(15, 'yum_repos', 'LIST');
insert into fact VALUES(16, 'bios_release_date', 'STRING');
insert into fact VALUES(17, 'os_release', 'STRING');
insert into fact VALUES(18, 'installed_packages', 'LIST');
insert into fact VALUES(19, 'infrastructure_type', 'STRING');
insert into fact VALUES(20, 'cores_per_socket', 'INT');
insert into fact VALUES(21, 'bios_version', 'STRING');
insert into fact VALUES(22, 'os_kernel_version', 'STRING');
insert into fact VALUES(23, 'cpu_flags', 'LIST');
insert into fact VALUES(24, 'installed_services', 'LIST');
insert into fact VALUES(25, 'network_interfaces', 'LIST');
insert into fact VALUES(26, 'bios_vendor', 'STRING');
insert into fact VALUES(27, 'number_of_cpus', 'INT');
insert into fact VALUES(28, 'insights_client_version', 'STRING');
insert into fact VALUES(29, 'kernel_modules', 'LIST');
insert into fact VALUES(30, 'system_memory_bytes', 'INT');
insert into fact VALUES(31, 'arch', 'STRING');
insert into fact VALUES(32, 'satellite_managed', 'BOOLEAN');
insert into fact VALUES(33, 'infrastructure_vendor', 'STRING');

create table policy (id integer,
                     customerid varchar not null,
                     name varchar not null ,
                     description varchar,
                     is_enabled boolean not null default false,
                     conditions varchar not null,
                     actions varchar );
alter table policy OWNER to sarah;
alter table policy ADD PRIMARY KEY (customerid,id);
create unique index pol_cus_nam_idx on policy (customerid,name); -- TODO does this make the name unique per customer (?)


insert into policy values (1, '1234', '1st policy', 'Just a test', true, '"cores" == 1','EMAIL roadrunner@acme.org');
insert into policy values (2, '1234', '2nd policy', 'Another test', false, '"cpu" != "intel"','HOOK http://localhost:8080');
insert into policy values (3, '1234', '3rd policy', 'Another test', true, '"rhelversion" >= "8" OR "cores" == 5','EMAIL toor@root.org');
insert into policy values (4, '1234', '4th policy', 'Test for account2', true, '"cores" > 4','SLACK slack://foo.slack-com/#channel');
insert into policy values (5, '1234', 'Detect Nice box', 'Test for os and arch', true, '"os_version" == "7.5" AND "arch" == "x86_64"', 'NOTIFY; EMAIL foo@acme.org');

-- create a sequence for hibernate id generator. Must start with a number higher than the id values
-- of above tables
create sequence hibernate_sequence start with 100 ;
alter sequence hibernate_sequence owner to sarah;

