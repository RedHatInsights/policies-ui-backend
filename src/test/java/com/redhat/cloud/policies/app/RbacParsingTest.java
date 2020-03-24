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

import com.redhat.cloud.policies.app.auth.RbacRaw;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

/**
 * @author hrupp
 */
public class RbacParsingTest {

  @Test
  void testParseExample() throws Exception {
    File file = new File("src/test/resources/rbac_example.json");
    Jsonb jb = JsonbBuilder.create();
    RbacRaw rbac = jb.fromJson(new FileInputStream(file), RbacRaw.class);
    assert rbac.data.size()==2;

    assert rbac.canWrite("resname");
    assert !rbac.canRead("resname");
    assert !rbac.canWriteAll();
    assert !rbac.canWrite("no-perm");

  }

  @Test
  void testNoAccess() throws Exception {
    File file = new File("src/test/resources/rbac_example_no_access.json");
    Jsonb jb = JsonbBuilder.create();
    RbacRaw rbac = jb.fromJson(new FileInputStream(file), RbacRaw.class);

    assert !rbac.canReadAll();
    assert !rbac.canWriteAll();
  }

  @Test
  void testFullAccess() throws Exception {
    File file = new File("src/test/resources/rbac_example_full_access.json");
    Jsonb jb = JsonbBuilder.create();
    RbacRaw rbac = jb.fromJson(new FileInputStream(file), RbacRaw.class);

    assert rbac.canRead("*");
    assert rbac.canReadAll();
    assert rbac.canWrite("*");
    assert rbac.canWriteAll();
  }

  @Test
  void testPartialAccess() throws FileNotFoundException {
    File file = new File("src/test/resources/rbac_example_partial_access.json");
    Jsonb jb = JsonbBuilder.create();
    RbacRaw rbac = jb.fromJson(new FileInputStream(file), RbacRaw.class);

    assert rbac.canReadAll();
    assert !rbac.canWriteAll();
    assert rbac.canDo("*","execute");
  }
}
