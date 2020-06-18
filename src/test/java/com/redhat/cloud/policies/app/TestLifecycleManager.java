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

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.redhat.cloud.policies.app.model.engine.FullTrigger;
import com.redhat.cloud.policies.app.model.engine.Trigger;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.RegexBody;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author hrupp
 */
public class TestLifecycleManager implements QuarkusTestResourceLifecycleManager {

  PostgreSQLContainer postgreSQLContainer ;
  MockServerContainer mockEngineServer;
  MockServerClient mockServerClient;

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
//       System.err.println(mockServerClient.retrieveLogMessages(request()));
//       System.err.println(mockServerClient.retrieveRecordedRequests(request()));
  }


  @Override
  public void inject(Object testInstance) {
    if (testInstance instanceof AbstractITest) {
      AbstractITest test = (AbstractITest) testInstance;
      test.mockServerClient = mockServerClient;
    }
  }

  void setupPostgres(Map<String, String> props) {
    postgreSQLContainer =
         new PostgreSQLContainer("postgres");
    postgreSQLContainer.start();
    // Now that postgres is started, we need to get its URL and tell Quarkus
    // quarkus.datasource.driver=io.opentracing.contrib.jdbc.TracingDriver
    // Driver needs a 'tracing' in the middle like jdbc:tracing:postgresql://localhost:5432/postgres
    String jdbcUrl = postgreSQLContainer.getJdbcUrl();
    jdbcUrl = "jdbc:tracing:" + jdbcUrl.substring(jdbcUrl.indexOf(':')+1);
    props.put("quarkus.datasource.url", jdbcUrl);
    props.put("quarkus.datasource.username","test");
    props.put("quarkus.datasource.password","test");
    props.put("quarkus.datasource.driver","io.opentracing.contrib.jdbc.TracingDriver");

  }

  void setupMockEngine(Map<String, String> props) {
    mockEngineServer = new MockServerContainer();

    // set up mock engine
    mockEngineServer.start();
    String mockServerUrl = "http://" + mockEngineServer.getContainerIpAddress() + ":" + mockEngineServer.getServerPort();
    mockServerClient = new MockServerClient(mockEngineServer.getContainerIpAddress(), mockEngineServer.getServerPort());

    mockRbac();
    mockEngine();
    mockNotifications();

    props.put("engine/mp-rest/url", mockServerUrl);
    props.put("rbac/mp-rest/url", mockServerUrl);
    props.put("notifications/mp-rest/url", mockServerUrl);

  }

  private void mockEngine() {

    mockServerClient
        .when(request()
          .withPath("/hawkular/alerts")
            .withQueryStringParameter("triggerIds",".*")
            .withQueryStringParameter("thin","true")
        )
        .respond(response()
            .withStatusCode(200)
            .withHeader("Content-Type", "application/json")
            .withBody("[{\"eventType\":\"ALERT\",\"tenantId\":\"901578\",\"id\":\"eb3c84e2-55bf-4121-ad99-68a363981f04-1587136858088-f5fb4bcf-c0f0-4974-8ec2-61fb284270de\",\"ctime\":1587136858088,\"dataSource\":\"_none_\",\"dataId\":\"eb3c84e2-55bf-4121-ad99-68a363981f04\",\"category\":\"ALERT\",\"text\":\"test-2\",\"trigger\":{\"tenantId\":\"901578\",\"id\":\"eb3c84e2-55bf-4121-ad99-68a363981f04\",\"name\":\"test-2\",\"type\":\"STANDARD\",\"eventType\":\"ALERT\",\"eventCategory\":null,\"eventText\":null,\"severity\":\"MEDIUM\",\"actions\":[{\"tenantId\":\"901578\",\"actionPlugin\":\"email\",\"actionId\":\"_managed-instance-email-35e48b6487d9568e\"}],\"autoDisable\":false,\"autoEnable\":false,\"autoResolve\":false,\"autoResolveAlerts\":false,\"autoResolveMatch\":\"ALL\",\"enabled\":true,\"firingMatch\":\"ALL\",\"source\":\"_none_\"},\"severity\":\"MEDIUM\",\"status\":\"OPEN\",\"lifecycle\":[{\"status\":\"OPEN\",\"user\":\"system\",\"stime\":1587136858088}]},{\"eventType\":\"ALERT\",\"tenantId\":\"901578\",\"id\":\"0bf486f4-cbe7-42b3-8036-36d3031a0a27-1587136858091-5022c6c0-3e93-4f4b-bcf6-d3b1445f06cb\",\"ctime\":1587136858091,\"dataSource\":\"_none_\",\"dataId\":\"0bf486f4-cbe7-42b3-8036-36d3031a0a27\",\"category\":\"ALERT\",\"text\":\"test-3\",\"trigger\":{\"tenantId\":\"901578\",\"id\":\"0bf486f4-cbe7-42b3-8036-36d3031a0a27\",\"name\":\"test-3\",\"type\":\"STANDARD\",\"eventType\":\"ALERT\",\"eventCategory\":null,\"eventText\":null,\"severity\":\"MEDIUM\",\"actions\":[{\"tenantId\":\"901578\",\"actionPlugin\":\"email\",\"actionId\":\"_managed-instance-email-35e48b6487d9568e\"}],\"autoDisable\":false,\"autoEnable\":false,\"autoResolve\":false,\"autoResolveAlerts\":false,\"autoResolveMatch\":\"ALL\",\"enabled\":true,\"firingMatch\":\"ALL\",\"source\":\"_none_\"},\"severity\":\"MEDIUM\",\"status\":\"OPEN\",\"lifecycle\":[{\"status\":\"OPEN\",\"user\":\"system\",\"stime\":1587136858091}]}]")
        );

    mockServerClient
        .when(request()
            // special case to simulate that the engine has a general failure.
            // must come before the more generic match below.
                  .withPath("/hawkular/alerts/triggers/c49e92c4-dead-beef-9200-245b31933e94/enable")
                  .withHeader("Hawkular-Tenant", "1234")
        )
        .respond(response()
                     .withStatusCode(500).withReasonPhrase("Internal server error")
                     .withHeader("Content-Type", "application/json")
                     .withBody("{ \"errorMessage\" : \"something went wrong\" }")
        );

    // -------------------------------

    List<Trigger> triggers = new ArrayList<>();
    Trigger trigger = new Trigger();
    trigger.id = "bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c";
    trigger.lifecycle = new ArrayList<>();
    Map<String,Object> ev = new HashMap<>();
    Calendar cal = Calendar.getInstance();
    cal.set(2020,04,8,10,00,00);
    ev.put("status","ALERT_GENERATE");
    ev.put("stime", cal.getTimeInMillis());
    trigger.lifecycle.add(ev);
    ev = new HashMap<>();
    cal.set(2020,04,9,10,00,00);
    ev.put("status","ALERT_GENERATE");
    ev.put("stime", cal.getTimeInMillis());
    trigger.lifecycle.add(ev);
    ev = new HashMap<>();
    cal.set(2020,04,10,10,00,00);
    ev.put("status","ALERT_GENERATE");
    ev.put("stime", cal.getTimeInMillis());
    trigger.lifecycle.add(ev);
    triggers.add(trigger);

    mockServerClient
        .when(request()
            .withPath("/hawkular/alerts/triggers")
            .withQueryStringParameter("triggerIds","bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
            .withHeader("Hawkular-Tenant","1234")
            .withMethod("GET")
        )
        .respond(response()
            .withStatusCode(200)
            .withHeader("Content-Type", "application/json")
            .withBody(JsonBody.json(triggers))
        );

    // -------------------------------

    String alertsHistory = HeaderHelperTest.getStringFromFile("alerts-history.json",false);
    mockServerClient
        .when(request()
            .withPath("/hawkular/alerts")
            .withQueryStringParameter("triggerIds","8671900e-9d31-47bf-9249-8f45698ede72")
            .withHeader("Hawkular-Tenant","1234")
            .withMethod("GET")
        )
        .respond(response()
            .withStatusCode(200)
            .withHeader("Content-Type", "application/json")
            .withHeader("X-Total-Count","2")
            .withBody(JsonBody.json(alertsHistory))
        );



    // -------------------------------

    FullTrigger ft = new FullTrigger();
    ft.trigger.id = "00000000-0000-0000-0000-000000000001";
    JsonBody ftBody = JsonBody.json(ft);

    mockServerClient
        .when(request()
            .withPath("/hawkular/alerts/triggers/trigger/00000000-0000-0000-0000-000000000001")
            .withMethod("GET")
        )
        .respond(response()
            .withStatusCode(200)
            .withHeader("Content-Type", "application/json")
            .withBody(ftBody)
        );

    mockServerClient
        .when(request()
          .withPath("/hawkular/alerts/triggers/.*/enable")
          .withMethod("PUT")
        )
        .respond(response().withStatusCode(200));
    mockServerClient
        .when(request()
          .withPath("/hawkular/alerts/triggers/.*/enable")
          .withMethod("DELETE")
        )
        .respond(response().withStatusCode(200));

    // Simulate internal engine issue
    mockServerClient
        .when(request()
                  .withPath("/hawkular/alerts/triggers/trigger")
            .withBody(new RegexBody(".*-dead-beef-9200-.*"))
                  .withHeader("Hawkular-Tenant", "1234")
        )
        .respond(response()
                     .withStatusCode(500)
                     .withHeader("Content-Type", "application/json")
                     .withBody("{ \"msg\" : \"ok\" }")
        );


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
            // special case to simulate that the engine has a general failure. CPOL-130
            // must come before the more generic match below.
                  .withPath("/hawkular/alerts/triggers/c49e92c4-dead-beef-9200-245b31933e94")
                  .withHeader("Hawkular-Tenant", "1234")
        )
        .respond(response()
                     .withStatusCode(500)
                     .withHeader("Content-Type", "application/json")
                     .withBody("{ \"errorMessage\" : \"something went wrong\" }")
        );
    mockServerClient
        .when(request()
            // special case to simulate that the engine does not have the policy. CPOL-130
            // must come before the more generic match below.
                  .withPath("/hawkular/alerts/triggers/c49e92c4-764c-4163-9200-245b31933e94")
                  .withMethod("DELETE")
                  .withHeader("Hawkular-Tenant", "1234")
        )
        .respond(response()
                     .withStatusCode(404)
                     .withHeader("Content-Type", "application/json")
                     .withBody("{ \"errorMessage\" : \"does not exist\" }")
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
  }

  private void mockNotifications() {
    mockServerClient
        .when(request()
              .withPath("/endpoints/email/subscription/.*")
              .withMethod("PUT")
        )
        .respond(response()
                 .withStatusCode(204)
        );
    mockServerClient
        .when(request()
              .withPath("/endpoints/email/subscription/.*")
              .withMethod("DELETE")
        )
        .respond(response()
                 .withStatusCode(204)
        );
  }

  private void mockRbac() {
    // RBac server
    String fullAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_full_access.json", false);
    String noAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_no_access.json", false);
    mockServerClient
        .when(request()
                  .withPath("/api/rbac/v1/access/")
                  .withQueryStringParameter("application", "policies")
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
                  .withQueryStringParameter("application", "policies")
                  .withHeader("x-rh-identity", ".*kYW1wZi11c2VyIn0=") // hans dampf user nothing allowed
        )
        .respond(HttpResponse.response()
                     .withStatusCode(200)
                     .withHeader("Content-Type", "application/json")
                     .withBody(noAccessRbac)
        );
  }

}
