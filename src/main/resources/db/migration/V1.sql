
-- Fact key table for fact-key -> type mapping
create table fact (id integer primary key, name varchar unique not null , type varchar);

-- Policy store
create table policy (id uuid,
                     customerid varchar not null,
                     name varchar not null ,
                     description varchar,
                     is_enabled boolean not null default false,
                     conditions varchar not null,
                     mtime TIMESTAMP default now(),
                     actions varchar );

alter table policy ADD PRIMARY KEY (customerid,id);
create unique index pol_cus_nam_idx on policy (customerid,name);


-- create a sequence for hibernate id generator. Must start with a number higher than the id values
-- of above tables
create sequence hibernate_sequence start with 100 ;

