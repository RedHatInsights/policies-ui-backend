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
package com.redhat.cloud.custompolicies.app;

import com.redhat.cloud.custompolicies.app.auth.HeaderHelper;
import com.redhat.cloud.custompolicies.app.auth.XRhIdentity;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * @author hrupp
 */
public class HeaderHelperTest {

  private static final String RHID = "x-rh-identity";

  @Test
  public void testNoHeader() {

    Optional<XRhIdentity> user = HeaderHelper.getRhIdFromHeader(null);
    Assert.assertFalse(user.isPresent());
  }

  @Test
  public void testSimpleHeaderNoRID() {
    MultivaluedMap<String,String> map = new MultivaluedHashMap<>();
    HttpHeaders headers = new ResteasyHttpHeaders(map);

    Optional<XRhIdentity> user = HeaderHelper.getRhIdFromHeader(headers);
    Assert.assertFalse(user.isPresent());

  }

  @Test
  public void testSimpleHeaderBadRID() {
    MultivaluedMap<String,String> map = new MultivaluedHashMap<>();
    map.putSingle(RHID, "frobnitz");
    HttpHeaders headers = new ResteasyHttpHeaders(map);

    Optional<XRhIdentity> user = HeaderHelper.getRhIdFromHeader(headers);
    Assert.assertFalse(user.isPresent());

  }

  @Test
  public void testSimpleHeaderGoodRID() {
    String rhid = getStringFromFile("rhid.txt",true);

    Optional<XRhIdentity> user = HeaderHelper.getRhIdFromString(rhid);
    Assert.assertTrue(user.isPresent());
    Assert.assertEquals("Username does not match","joe-doe-user",user.get().getUsername());
    Assert.assertEquals("Account does not match","1234",user.get().identity.accountNumber);

  }

  @Test
  public void testSimpleHeaderGoodRID2() {
    MultivaluedMap<String,String> map = new MultivaluedHashMap<>();
    String rhid = getStringFromFile("rhid.txt",true);
    map.putSingle(RHID, rhid);
    HttpHeaders headers = new ResteasyHttpHeaders(map);

    Optional<XRhIdentity> user = HeaderHelper.getRhIdFromHeader(headers);
    Assert.assertTrue(user.isPresent());
    Assert.assertEquals("Username does not match","joe-doe-user",user.get().getUsername());
    Assert.assertEquals("Account does not match","1234",user.get().identity.accountNumber);

  }

  @NotNull
  public static String getStringFromFile(String filename, boolean removeTrailingNewline) {
    String rhid = "";
    try (FileInputStream fis = new FileInputStream("src/test/resources/" + filename)) {
      Reader r = new InputStreamReader(fis, StandardCharsets.UTF_8);
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[1024];
      int chars_read = r.read(buf);
      while (chars_read >= 0) {
        sb.append(buf,0,chars_read);
        chars_read = r.read(buf);
      }
      r.close();
      rhid = sb.toString();
      if (removeTrailingNewline && rhid.endsWith("\n")) {
        rhid = rhid.substring(0,rhid.indexOf('\n'));
      }
    }
    catch (IOException ioe) {
      Assert.fail("File reading failed: " + ioe.getMessage());
    }
    return rhid;
  }
}
