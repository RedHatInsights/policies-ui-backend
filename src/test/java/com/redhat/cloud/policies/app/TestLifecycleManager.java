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

import static java.util.Calendar.getInstance;
import static java.util.Calendar.MAY;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
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
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.RegexBody;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private static final String LOG_LEVEL_KEY = "mockserver.logLevel";

    private PostgreSQLContainer postgreSQLContainer;

    private static ClientAndServer mockServer;

    @Override
    public Map<String, String> start() {
        System.err.println("++++  TestLifecycleManager start +++");
        Map<String, String> properties = new HashMap<>();
        setupPostgres(properties);
        setupMockEngine(properties);

        System.out.println(" -- Running with properties: " + properties);
        return properties;
    }

    @Override
    public void stop() {
        postgreSQLContainer.stop();
        // Helper to debug mock server issues
        // System.err.println(mockServerClient.retrieveLogMessages(request()));
        // System.err.println(mockServerClient.retrieveRecordedRequests(request()));
    }

    @Override
    public void inject(Object testInstance) {
        if (testInstance instanceof AbstractITest) {
            AbstractITest test = (AbstractITest) testInstance;
            test.mockServer = mockServer;
        }
    }

    void setupPostgres(Map<String, String> props) {
        postgreSQLContainer = new PostgreSQLContainer<>("postgres");
        postgreSQLContainer.start();
        // Now that postgres is started, we need to get its URL and tell Quarkus
        // quarkus.datasource.driver=io.opentracing.contrib.jdbc.TracingDriver
        // Driver needs a 'tracing' in the middle like jdbc:tracing:postgresql://localhost:5432/postgres
        String jdbcUrl = postgreSQLContainer.getJdbcUrl();
        jdbcUrl = "jdbc:tracing:" + jdbcUrl.substring(jdbcUrl.indexOf(':') + 1);
        props.put("quarkus.datasource.jdbc.url", jdbcUrl);
        props.put("quarkus.datasource.username", "test");
        props.put("quarkus.datasource.password", "test");
        props.put("quarkus.datasource.jdbc.driver", "io.opentracing.contrib.jdbc.TracingDriver");
    }

    void setupMockEngine(Map<String, String> props) {
        if (System.getProperty(LOG_LEVEL_KEY) == null) {
            System.setProperty(LOG_LEVEL_KEY, "OFF");
            System.out.println("MockServer log is disabled. Use '-D" + LOG_LEVEL_KEY + "=WARN|INFO|DEBUG|TRACE' to enable it.");
        }
        mockServer = startClientAndServer();

        mockRbac();
        mockEngine();

        String mockServerUrl = "http://localhost:" + mockServer.getPort();
        props.put("quarkus.rest-client.engine.url", mockServerUrl);
        props.put("quarkus.rest-client.rbac.url", mockServerUrl);
        props.put("quarkus.rest-client.notifications.url", mockServerUrl);

    }

    private void mockEngine() {
        mockServer
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
        Map<String, Object> ev = new HashMap<>();
        Calendar cal = getInstance();
        cal.set(2020, MAY, 8, 10, 0, 0);
        ev.put("status", "ALERT_GENERATE");
        ev.put("stime", cal.getTimeInMillis());
        trigger.lifecycle.add(ev);
        ev = new HashMap<>();
        cal.set(2020, MAY, 9, 10, 0, 0);
        ev.put("status", "ALERT_GENERATE");
        ev.put("stime", cal.getTimeInMillis());
        trigger.lifecycle.add(ev);
        ev = new HashMap<>();
        cal.set(2020, MAY, 10, 10, 0, 0);
        ev.put("status", "ALERT_GENERATE");
        ev.put("stime", cal.getTimeInMillis());
        trigger.lifecycle.add(ev);
        triggers.add(trigger);

        mockServer
                .when(request()
                        .withPath("/hawkular/alerts/triggers")
                        .withQueryStringParameter("triggerIds", "bd0ee2ec-eec0-44a6-8bb1-29c4179fc21c")
                        .withHeader("Hawkular-Tenant", "1234")
                        .withMethod("GET")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(JsonBody.json(triggers))
                );

        // -------------------------------

        FullTrigger ft = new FullTrigger();
        ft.trigger.id = "00000000-0000-0000-0000-000000000001";
        JsonBody ftBody = JsonBody.json(ft);

        mockServer
                .when(request()
                        .withPath("/hawkular/alerts/triggers/trigger/00000000-0000-0000-0000-000000000001")
                        .withMethod("GET")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ftBody)
                );

        mockServer
                .when(request()
                        .withPath("/hawkular/alerts/triggers/.*/enable")
                        .withMethod("PUT")
                )
                .respond(response().withStatusCode(200));
        mockServer
                .when(request()
                        .withPath("/hawkular/alerts/triggers/.*/enable")
                        .withMethod("DELETE")
                )
                .respond(response().withStatusCode(200));

        // Simulate internal engine issue
        mockServer
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

        // ------------ status page
        mockServer
                .when(request()
                        .withPath("/hawkular/alerts/triggers")
                        .withQueryStringParameter("triggerIds", "dummy")
                        .withHeader("Hawkular-Tenant", "dummy")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                );

        mockServer
                .when(request()
                        .withPath("/apps")
                )
                .respond(response()
                        .withBody("[]")
                        .withHeader("Content-Type", "application/json")
                        .withStatusCode(200)
                );

        // ------------

        mockServer
                .when(request()
                        .withPath("/hawkular/alerts/triggers/trigger")
                        .withHeader("Hawkular-Tenant", "1234")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"msg\" : \"ok\" }")
                );
        mockServer
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
        mockServer
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
        mockServer
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
        mockServer
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

    private void mockRbac() {
        // RBac server
        String fullAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_full_access.json", false);
        String noAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_no_access.json", false);
        mockServer
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
        mockServer
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
