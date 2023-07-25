package com.redhat.cloud.policies.app.auth;

import com.redhat.cloud.policies.app.RbacServer;
import com.redhat.cloud.policies.app.auth.models.RbacRaw;

import io.quarkus.cache.CacheResult;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class RbacClient {

    @Inject
    @RestClient
    RbacServer rbac;

    /*
     * This code is on purpose in a separate method and not inside the main
     * filter method so that the caching annotation can be applied. This speeds
     * up the user experience, as results are returned from the cache.
     * TTL of the cache items is defined in application.properties
     * quarkus.cache.caffeine.rbac-cache.expire-after-write
     *
     * Also it is important to Exceptions for the remote bubble out the method,
     * as if an Exception is thrown, the cache will not store the result.
     * Catching and returning null would end up in the next call directly
     * return null from the cache without retrying the remote call.
     *
     * Inventory permissions are also queried to get the list of host groups
     * that the user has access to.
     */
    @CacheResult(cacheName = "rbac-cache")
    RbacRaw getRbacInfo(String xrhidHeader) {
        return rbac.getRbacInfo("policies,inventory", 100, xrhidHeader);
    }
}
