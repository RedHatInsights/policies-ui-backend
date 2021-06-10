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
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.enterprise.event.Observes;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.logging.Logger;

/**
 * Set the base path for the application
 * @author hrupp
 *
 */
@ApplicationPath("/")
public class JaxRsApplication extends Application {

  Logger log = Logger.getLogger("Policies UI-Backend");

  public static final String FILTER_REGEX = ".*(/health(/\\w+)?|/metrics|/api/policies/v1.0/status) HTTP/[0-9].[0-9]\" 200.*\\n?";
  private static final Pattern pattern = Pattern.compile(FILTER_REGEX);

  @ConfigProperty(name = "quarkus.http.access-log.category")
  String loggerName;

  // Server init is done here, so we can do some more initialisation
  void init(@Observes Router router) {
    initAccessLogFilter();

    showVersionInfo();

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

  private void showVersionInfo() {
    // Produce build-info and log on startup

    String commmitSha = System.getenv("OPENSHIFT_BUILD_COMMIT");
    if (commmitSha != null ) {
      String openshiftBuildReference = System.getenv("OPENSHIFT_BUILD_REFERENCE");
      String openshiftBuildName = System.getenv("OPENSHIFT_BUILD_NAME");

      String info = String.format("%n    s2i-build [%s]%n    from branch [%s]%n    with git sha [%s]",
          openshiftBuildName,
          openshiftBuildReference,
          commmitSha);
      log.info(info);
    } else {
      log.info("%n    Not built on OpenShift s2i, no version info available");
    }
  }
}
