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

import java.io.File;
import java.util.List;


import javax.inject.Inject;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cloud.policies.app.auth.models.RbacRaw;

import io.quarkus.test.junit.QuarkusTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class RbacParsingTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    void testParseExample() throws Exception {
        File file = new File("src/test/resources/rbac_example.json");
        RbacRaw rbac = objectMapper.readValue(file, RbacRaw.class);
        assertEquals(3, rbac.data.size());

        assertTrue(rbac.canWrite("bar", "resname"));
        // We don't have explicit read permission for "resname" but we have bar:*:read.
        assertTrue(rbac.canRead("bar", "resname"));
        assertFalse(rbac.canWrite("bar", "no-perm"));
        assertFalse(rbac.canWrite("foo", "no-perm"));
        assertFalse(rbac.canWrite("hulla", "no-perm"));

    }

    @Test
    void testNoAccess() throws Exception {
        File file = new File("src/test/resources/rbac_example_no_access.json");
        RbacRaw rbac = objectMapper.readValue(file, RbacRaw.class);

        assertFalse(rbac.canWrite("foo", "foobar"));
        assertFalse(rbac.canRead("foo", "1337"));
    }

    @Test
    void testFullAccess() throws Exception {
        File file = new File("src/test/resources/rbac_example_full_access.json");
        RbacRaw rbac = objectMapper.readValue(file, RbacRaw.class);

        assertTrue(rbac.canRead("policies", "*"));
        assertTrue(rbac.canRead("policies", "anything"));
        assertTrue(rbac.canWrite("policies", "*"));
        assertTrue(rbac.canWrite("policies", "anything"));
    }

    @Test
    void testPartialAccess() throws Exception {
        File file = new File("src/test/resources/rbac_example_partial_access.json");
        RbacRaw rbac = objectMapper.readValue(file, RbacRaw.class);

        assertTrue(rbac.canDo("policies", "*", "execute"));
        assertTrue(rbac.canDo("policies", "foobar", "execute"));
        assertFalse(rbac.canDo("notpolicies", "policies", "execute"));
    }

    @Test
    void testResourceDefinitionsParsing() throws Exception {
        File file = new File("src/test/resources/rbac_example.json");
        RbacRaw rbac = objectMapper.readValue(file, RbacRaw.class);
        assertEquals(3, rbac.data.size());

        assertNotNull(rbac.data.get(0).resourceDefinitions);
        assertEquals(0, rbac.data.get(0).resourceDefinitions.size());

        assertNotNull(rbac.data.get(1).resourceDefinitions);
        assertEquals(1, rbac.data.get(1).resourceDefinitions.size());
        assertEquals(List.of("123456"), rbac.data.get(1).resourceDefinitions.get(0).attributeFilter.value);

        assertNotNull(rbac.data.get(2).resourceDefinitions);
        assertEquals(1, rbac.data.get(2).resourceDefinitions.size());
        assertEquals(List.of("654321"), rbac.data.get(2).resourceDefinitions.get(0).attributeFilter.value);
    }
}
