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
package com.redhat.cloud.custompolicies.app;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.restassured.http.Header;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author hrupp
 */
public abstract class AbstractITest {

  static Header authHeader;
  static MockServerClient mockServerClient;
  static Header authRbacNoAccess; // Hans Dampf has no rbac access rights

  static final String API_BASE = "/api/custom-policies/v1.0";


  static void setupPostgres(PostgreSQLContainer postgreSQLContainer) {
    postgreSQLContainer.start();
    // Now that postgres is started, we need to get its URL and tell Quarkus
    System.err.println("JDBC URL :" + postgreSQLContainer.getJdbcUrl());
    System.setProperty("quarkus.datasource.url", postgreSQLContainer.getJdbcUrl());
    System.setProperty("quarkus.datasource.username","test");
    System.setProperty("quarkus.datasource.password","test");

  }

  static void setupRhId() {
    // provide rh-id
    String rhid = HeaderHelperTest.getStringFromFile("rhid.txt",false);
    authHeader = new Header("x-rh-identity", rhid);
    rhid = HeaderHelperTest.getStringFromFile("rhid_hans.txt",false);
    authRbacNoAccess = new Header("x-rh-identity", rhid);
  }

  static void setupMockEngine(MockServerContainer mockEngineServer) {
     // set up mock engine
     mockEngineServer.start();
     String mockServerUrl = "http://" + mockEngineServer.getContainerIpAddress() + ":" + mockEngineServer.getServerPort();
     System.err.println("Mock engine at " + mockServerUrl);
     mockServerClient = new MockServerClient(mockEngineServer.getContainerIpAddress(), mockEngineServer.getServerPort());
     mockServerClient
         .when(request()
             .withPath("/hawkular/alerts/triggers/trigger")
             .withHeader("Hawkular-Tenant","1234")
         )
         .respond(response()
             .withStatusCode(200)
             .withHeader("Content-Type","application/json")
             .withBody("{ \"msg\" : \"ok\" }")
         );
     mockServerClient
         .when(request()
             .withPath("/hawkular/alerts/triggers/.*")
             .withMethod("DELETE")
             .withHeader("Hawkular-Tenant","1234")
         )
         .respond(response()
             .withStatusCode(200)
             .withHeader("Content-Type","application/json")
             .withBody("{ \"msg\" : \"ok\" }")
         );
     mockServerClient
         .when(request()
             .withPath("/hawkular/alerts/triggers/trigger/.*")
             .withMethod("PUT")
             .withHeader("Hawkular-Tenant","1234")
         )
         .respond(response()
             .withStatusCode(200)
             .withHeader("Content-Type","application/json")
             .withBody("{ \"msg\" : \"ok\" }")
         );

     // RBac server
     String fullAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_full_access.json", false);
     String noAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_no_access.json", false);
     RestApiTest.mockServerClient
         .when(request()
                   .withPath("/api/rbac/v1/access/")
                   .withQueryStringParameter("application","custom-policies")
                   .withHeader("x-rh-identity",".*2UtZG9lLXVzZXIifQ==") // normal user all allowed
         )
         .respond(HttpResponse.response()
                      .withStatusCode(200)
                      .withHeader("Content-Type","application/json")
                      .withBody(fullAccessRbac)

         );
     RestApiTest.mockServerClient
         .when(request()
                   .withPath("/api/rbac/v1/access/")
                   .withQueryStringParameter("application","custom-policies")
                   .withHeader("x-rh-identity",".*kYW1wZi11c2VyIn0=") // hans dampf user nothing allowed
         )
         .respond(HttpResponse.response()
                      .withStatusCode(200)
                      .withHeader("Content-Type","application/json")
                      .withBody(noAccessRbac)
         );

     System.setProperty("engine/mp-rest/url",mockServerUrl);
     System.setProperty("rbac/mp-rest/url",mockServerUrl);

  }

}
