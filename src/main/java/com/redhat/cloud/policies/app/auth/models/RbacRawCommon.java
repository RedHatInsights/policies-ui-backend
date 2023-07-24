package com.redhat.cloud.policies.app.auth.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RbacRawCommon {

    @JsonIgnore
    public static final String ANY = "*";

    @JsonIgnore
    public static final String READ_OPERATION = "read";

    @JsonIgnore
    public static final String WRITE_OPERATION = "write";

    @JsonIgnore
    public static final String[] INVENTORY_HOSTS_READ = "inventory:hosts:read".split(":");

}
