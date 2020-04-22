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

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
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
@ApplicationPath("/api/policies/v1.0")
public class JaxRsApplication extends Application {

  Logger log = Logger.getLogger("Policies UI-Backend");

  @ConfigProperty(name = "accesslog.filter.health", defaultValue = "true")
  boolean filterHealth;

  // Server init is done here, so we can do some more initialisation

  void observeRouter(@Observes Router router) {
    //Produce access log
    Handler<RoutingContext> handler = new JsonAccessLoggerHandler(filterHealth);
    router.route().order(-1000).handler(handler);

    // Produce build-info and log on startup
    String info = String.format("\n    Build-date [%s] \n    on host [%s]\n    from branch [%s]\n    with git sha [%s]",
        Version.COMPILE_TIME,
        Version.BUILD_HOST,
        Version.GIT_BRANCH,
        Version.GIT_SHA);
    log.info(info);

  }
}
