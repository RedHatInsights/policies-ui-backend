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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.HashMap;
import java.util.Map;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author hrupp
 */
public class TestLifecycleManager implements QuarkusTestResourceLifecycleManager {

  PostgreSQLContainer postgreSQLContainer ;
  MockServerContainer mockEngineServer;


  @Override
  public Map<String, String> start() {
    System.err.println("++++  TestLifecycleManager start +++");
    Map<String,String> properties = new HashMap<>();
    setupPostgres(properties);
    setupMockEngine(properties);

    System.out.println(" -- Running with properties: " + properties);
    return properties;
  }

  @Override
  public void stop() {
    postgreSQLContainer.stop();
    // Helper to debug mock server issues
   //    System.err.println(mockServerClient.retrieveLogMessages(request()));
   //    System.err.println(mockServerClient.retrieveRecordedRequests(request()));
  }


  void setupPostgres(Map<String, String> props) {
    postgreSQLContainer =
         new PostgreSQLContainer("postgres");
    postgreSQLContainer.start();
    // Now that postgres is started, we need to get its URL and tell Quarkus
    // quarkus.datasource.driver=io.opentracing.contrib.jdbc.TracingDriver
    // Driver needs a 'tracing' in the middle like jdbc:tracing:postgresql://localhost:5432/postgres
    String jdbcUrl = postgreSQLContainer.getJdbcUrl();
    props.put("quarkus.datasource.url", jdbcUrl);
    props.put("quarkus.datasource.username","test");
    props.put("quarkus.datasource.password","test");

  }

  void setupMockEngine(Map<String, String> props) {
    mockEngineServer = new MockServerContainer();

    MockServerClient mockServerClient;
    // set up mock engine
    mockEngineServer.start();
    String mockServerUrl = "http://" + mockEngineServer.getContainerIpAddress() + ":" + mockEngineServer.getServerPort();
    mockServerClient = new MockServerClient(mockEngineServer.getContainerIpAddress(), mockEngineServer.getServerPort());
    mockServerClient
        .when(request()
                  .withPath("/hawkular/alerts/triggers/trigger")
                  .withHeader("Hawkular-Tenant", "1234")
        )
        .respond(response()
                     .withStatusCode(200)
                     .withHeader("Content-Type", "application/json")
                     .withBody("{ \"msg\" : \"ok\" }")
        );
    mockServerClient
        .when(request()
                  .withPath("/hawkular/alerts/triggers/.*")
                  .withMethod("DELETE")
                  .withHeader("Hawkular-Tenant", "1234")
        )
        .respond(response()
                     .withStatusCode(200)
                     .withHeader("Content-Type", "application/json")
                     .withBody("{ \"msg\" : \"ok\" }")
        );
    mockServerClient
        .when(request()
                  .withPath("/hawkular/alerts/triggers/trigger/.*")
                  .withMethod("PUT")
                  .withHeader("Hawkular-Tenant", "1234")
        )
        .respond(response()
                     .withStatusCode(200)
                     .withHeader("Content-Type", "application/json")
                     .withBody("{ \"msg\" : \"ok\" }")
        );

    // RBac server
    String fullAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_full_access.json", false);
    String noAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_no_access.json", false);
    mockServerClient
        .when(request()
                  .withPath("/api/rbac/v1/access/")
                  .withQueryStringParameter("application", "custom-policies")
                  .withHeader("x-rh-identity", ".*2UtZG9lLXVzZXIifQ==") // normal user all allowed
        )
        .respond(HttpResponse.response()
                     .withStatusCode(200)
                     .withHeader("Content-Type", "application/json")
                     .withBody(fullAccessRbac)

        );
    mockServerClient
        .when(request()
                  .withPath("/api/rbac/v1/access/")
                  .withQueryStringParameter("application", "custom-policies")
                  .withHeader("x-rh-identity", ".*kYW1wZi11c2VyIn0=") // hans dampf user nothing allowed
        )
        .respond(HttpResponse.response()
                     .withStatusCode(200)
                     .withHeader("Content-Type", "application/json")
                     .withBody(noAccessRbac)
        );

    props.put("engine/mp-rest/url", mockServerUrl);
    props.put("rbac/mp-rest/url", mockServerUrl);
  }

}
