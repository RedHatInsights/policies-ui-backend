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

import static org.hamcrest.CoreMatchers.containsString;

import static io.restassured.RestAssured.given;

import com.redhat.cloud.custompolicies.app.model.Policy;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import java.sql.SQLException;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author hrupp
 */
@QuarkusTest
class RestApiTest {

  @ClassRule
  private static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres");

  @BeforeAll
  static void configurePostgres() throws SQLException, LiquibaseException {
    postgreSQLContainer.start();
    // Now that postgres is started, we need to get its URL and tell Quarkus
    System.err.println("JDBC URL :" + postgreSQLContainer.getJdbcUrl());
    System.setProperty("quarkus.datasource.url", postgreSQLContainer.getJdbcUrl());
    System.setProperty("quarkus.datasource.username","test");
    System.setProperty("quarkus.datasource.password","test");

    PGSimpleDataSource ds = new PGSimpleDataSource();

    // Datasource initialization
    ds.setUrl(postgreSQLContainer.getJdbcUrl());
    ds.setUser(postgreSQLContainer.getUsername());
    ds.setPassword(postgreSQLContainer.getPassword());

    DatabaseConnection dbconn = new JdbcConnection(ds.getConnection());
    ResourceAccessor ra = new FileSystemResourceAccessor("src/test/sql");
    Liquibase liquibase = new Liquibase("dbinit.sql",ra, dbconn);
    liquibase.dropAll();
    liquibase.update(new Contexts());
  }


  @AfterAll
  static void closePostgres() {
    postgreSQLContainer.stop();
  }

  @Test
  void testFactEndpoint() {
    given()
        .when().get("/api/v1/facts")
        .then()
        .statusCode(200)
        .body(containsString("rhelversion"));
  }

  @Test
  void testGetPolicies() {
    given()
        .when().get("/api/v1/policies/1")
        .then()
        .statusCode(200)
        .body(containsString("2nd policy"));
  }

  @Test
  void testGetPoliciesForUnknownAccount() {
    given()
        .when().get("/api/v1/policies/3")
        .then()
        .statusCode(404);
  }

  @Test
  void testGetOnePolicy() {
    JsonPath jsonPath =
    given()
        .when().get("/api/v1/policies/1/policy/1")
        .then()
        .statusCode(200)
        .body(containsString("1st policy"))
        .extract().jsonPath();

    Policy policy = jsonPath.getObject("",Policy.class);
    Assert.assertEquals("Action does not match", "EMAIL roadrunner@acme.org", policy.actions);
    Assert.assertEquals("Conditions do not match", "\"cores\" == 1", policy.conditions);
    Assert.assertTrue("Policy is not enabled", policy.isEnabled);
  }

  @Test
  void testGetOneBadPolicy() {
    given()
        .when().get("/api/v1/policies/1/policy/15")
        .then()
        .statusCode(404);
  }

}
