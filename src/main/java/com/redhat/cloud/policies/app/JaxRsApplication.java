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

import io.quarkus.runtime.StartupEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.enterprise.event.Observes;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Set the base path for the application
 */
@ApplicationPath("/")
public class JaxRsApplication extends Application {

    Logger log = Logger.getLogger("Policies UI-Backend");

    public static final String FILTER_REGEX = ".*(/health(/\\w+)?|/metrics|/api/policies/v1.0/status) HTTP/[0-9].[0-9]\" 200.*\\n?";
    private static final Pattern pattern = Pattern.compile(FILTER_REGEX);

    @ConfigProperty(name = "quarkus.http.access-log.category")
    String loggerName;

    // Server init is done here, so we can do some more initialisation
    void init(@Observes StartupEvent event) {
        initAccessLogFilter();

        log.info(readGitProperties());

        logExternalServiceUrl("engine/mp-rest/url");
        logExternalServiceUrl("notifications/mp-rest/url");
        logExternalServiceUrl("rbac/mp-rest/url");

        // Generate a token
        StuffHolder.getInstance();
    }

    private void initAccessLogFilter() {
        java.util.logging.Logger accessLog = java.util.logging.Logger.getLogger(loggerName);
        accessLog.setFilter(record -> {
            final String logMessage = record.getMessage();
            Matcher matcher = pattern.matcher(logMessage);
            return !matcher.matches();
        });
    }

    private String readGitProperties() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("git.properties");
        try {
            return readFromInputStream(inputStream);
        } catch (IOException e) {
            log.error("Could not read git.properties.", e);
            return "Version information could not be retrieved";
        }
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "git.properties file not available";
        }
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    private void logExternalServiceUrl(String configKey) {
        log.infof(configKey + "=%s", ConfigProvider.getConfig().getValue(configKey, String.class));
    }
}
