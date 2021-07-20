-- POL-450 Policies history

create table policies_history (
    id uuid not null,
    tenant_id varchar(255) not null,
    policy_id varchar(255) not null,
    ctime bigint not null,
    host_id varchar(255),
    host_name varchar(255),
    constraint pk_policies_history primary key (id)
);
