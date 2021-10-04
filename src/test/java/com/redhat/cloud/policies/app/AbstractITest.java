/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.cloud.policies.app;


import io.restassured.http.Header;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.client.MockServerClient;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Map;

/**
 * Test base for a few common things.
 * The heavy lifting of mock-setup is done in the {@link TestLifecycleManager}
 */
public abstract class AbstractITest {

    static Header authHeader;       // User with access rights
    static Header authRbacNoAccess; // Hans Dampf has no rbac access rights
    static Header authHeaderNoAccount; // Account number is empty

    static final String API_BASE_V1_0 = "/api/policies/v1.0";
    static final String API_BASE_V1 = "/api/policies/v1";
    public MockServerClient mockServerClient;
    @Inject
    EntityManager entityManager;

    @BeforeAll
    static void setupRhId() {
        // provide rh-id
        String rhid = HeaderHelperTest.getStringFromFile("rhid.txt", false);
        authHeader = new Header("x-rh-identity", rhid);
        rhid = HeaderHelperTest.getStringFromFile("rhid_hans.txt", false);
        authRbacNoAccess = new Header("x-rh-identity", rhid);
        rhid = HeaderHelperTest.getStringFromFile("rhid_no_account.txt", false);
        authHeaderNoAccount = new Header("x-rh-identity", rhid);
    }

    protected void extractAndCheck(Map<String, String> links, String rel, int limit, int offset) {
        String url = links.get(rel);
        Assert.assertNotNull("Rel [" + rel + "] not found", url);
        String tmp = String.format("limit=%d&offset=%d", limit, offset);
        Assert.assertTrue("Url for rel [" + rel + "] should end in [" + tmp + "], but was [" + url + "]", url.endsWith(tmp));
    }

    protected long countPoliciesInDB() {
        Query q = entityManager.createQuery("SELECT count(p) FROM Policy p WHERE p.customerid = :cid");
        q.setParameter("cid", "1234");
        long count = (long) q.getSingleResult();
        return count;
    }
}
