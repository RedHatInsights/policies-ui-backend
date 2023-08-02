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

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.HashMap;
import java.util.Map;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
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
                .when(request().withPath("/lightweight-engine/validate"))
                .respond(response().withStatusCode(200));
    }

    private void mockRbac() {
        // RBac server
        String fullAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_full_access.json", false);
        String accessWithGroupsRbac = HeaderHelperTest.getStringFromFile("rbac_example_groups.json", false);
        String noAccessRbac = HeaderHelperTest.getStringFromFile("rbac_example_no_access.json", false);
        mockServer
                .when(request()
                        .withPath("/api/rbac/v1/access/")
                        .withQueryStringParameter("application", "policies,inventory")
                        .withHeader("x-rh-identity", ".*vZS1kb2UtdXNlciJ9") // normal user all allowed
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fullAccessRbac)

                );

        mockServer
                .when(request()
                        .withPath("/api/rbac/v1/access/")
                        .withQueryStringParameter("application", "policies,inventory")
                        .withHeader("x-rh-identity", ".*XNlci13aXRoLWdyb3VwcyJ9") // user with host groups
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(accessWithGroupsRbac)

                );
        mockServer
                .when(request()
                        .withPath("/api/rbac/v1/access/")
                        .withQueryStringParameter("application", "policies,inventory")
                        .withHeader("x-rh-identity", ".*hbXBmLXVzZXIifQ==") // hans dampf user nothing allowed
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(noAccessRbac)
                );
    }

}
