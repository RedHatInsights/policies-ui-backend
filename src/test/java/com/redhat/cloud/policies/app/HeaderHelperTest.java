/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import com.redhat.cloud.policies.app.auth.HeaderHelper;
import com.redhat.cloud.policies.app.auth.XRhIdentity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class HeaderHelperTest {

    private static final String RHID = "x-rh-identity";

    @Test
    void testNoHeader() {
        Optional<XRhIdentity> user = HeaderHelper.getRhIdFromHeader(null);
        assertFalse(user.isPresent());
    }

    @Test
    void testSimpleHeaderNoRID() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        HttpHeaders headers = new ResteasyHttpHeaders(map);

        Optional<XRhIdentity> user = HeaderHelper.getRhIdFromHeader(headers);
        assertFalse(user.isPresent());
    }

    @Test
    void testSimpleHeaderBadRID() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.putSingle(RHID, "frobnitz");
        HttpHeaders headers = new ResteasyHttpHeaders(map);

        Optional<XRhIdentity> user = HeaderHelper.getRhIdFromHeader(headers);
        assertFalse(user.isPresent());
    }

    @Test
    void testSimpleHeaderGoodRID() {
        String rhid = getStringFromFile("rhid.txt", true);

        Optional<XRhIdentity> user = HeaderHelper.getRhIdFromString(rhid);
        assertTrue(user.isPresent());
        assertEquals("joe-doe-user", user.get().getUsername(), "Username does not match");
        assertEquals("1234", user.get().identity.accountNumber, "Account does not match");
    }

    @Test
    void testSimpleHeaderGoodRID2() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        String rhid = getStringFromFile("rhid.txt", true);
        map.putSingle(RHID, rhid);
        HttpHeaders headers = new ResteasyHttpHeaders(map);

        Optional<XRhIdentity> user = HeaderHelper.getRhIdFromHeader(headers);
        assertTrue(user.isPresent());
        assertEquals("joe-doe-user", user.get().getUsername(), "Username does not match");
        assertEquals("1234", user.get().identity.accountNumber, "Account does not match");
    }

    @NotNull
    static String getStringFromFile(String filename, boolean removeTrailingNewline) {
        String rhid = "";
        try (FileInputStream fis = new FileInputStream("src/test/resources/" + filename)) {
            Reader r = new InputStreamReader(fis, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int chars_read = r.read(buf);
            while (chars_read >= 0) {
                sb.append(buf, 0, chars_read);
                chars_read = r.read(buf);
            }
            r.close();
            rhid = sb.toString();
            if (removeTrailingNewline && rhid.endsWith("\n")) {
                rhid = rhid.substring(0, rhid.indexOf('\n'));
            }
        } catch (IOException ioe) {
            fail("File reading failed: " + ioe.getMessage());
        }
        return rhid;
    }
}
