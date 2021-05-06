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

import com.redhat.cloud.policies.app.auth.IncomingRequestFilter;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.impl.Utils;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.logging.Logger;

/**
 * @author hrupp
 */
public class JsonAccessLoggerHandler implements LoggerHandler {

  public static final String REFERRER = "referrer";
  Jsonb jsonb;
  JsonObjectBuilder jsonObjectBuilder;
  private final boolean filterHealthCalls;

  private final Logger log = Logger.getLogger("Access");

  public JsonAccessLoggerHandler(boolean filterHealthCalls) {
    this.filterHealthCalls = filterHealthCalls;
    jsonb = JsonbBuilder.create();
    jsonObjectBuilder = Json.createObjectBuilder();
  }


  void log(RoutingContext context, long timestamp, String remoteClient, HttpVersion version, HttpMethod method, String uri) {

    HttpServerRequest request = context.request();
    int status = request.response().getStatusCode();

    if (status == 301 || status == 302) {
      // Those are redirects, that we skip
      return;
    }

    // By default omit requests for metrics and health check if they return a 200
    if (filterHealthCalls) {
      if (status==200 && (uri.startsWith("/health") ||
                          uri.startsWith("/metrics") ||
                          uri.startsWith("/q/metrics") ||  // New metric path with micrometer
                          uri.startsWith("/q/health") ||   // New health path with micrometer
                          uri.equals("/api/policies/v1.0/status")
      )) {
        return;
      }
    }

    jsonObjectBuilder.add("date", Utils.formatRFC1123DateTime(timestamp));
    jsonObjectBuilder.add("method",method.name());
    jsonObjectBuilder.add("uri",uri);

    // See IncomingRequestFilter on how this is populated
    String acctId = (String) context.data().get(IncomingRequestFilter.X_RH_ACCOUNT);
    if (acctId!=null) {
      jsonObjectBuilder.add("acct", acctId);
    }
    String user = (String) context.data().get(IncomingRequestFilter.X_RH_USER);
    if (user!=null) {
      jsonObjectBuilder.add("user", user);
    }

    String versionFormatted ;
    switch (version){
      case HTTP_1_0:
        versionFormatted = "HTTP/1.0";
        break;
      case HTTP_1_1:
        versionFormatted = "HTTP/1.1";
        break;
      case HTTP_2:
        versionFormatted = "HTTP/2.0";
        break;
      default:
        versionFormatted = "-none-";
    }
    jsonObjectBuilder.add("http_version",versionFormatted);
    long contentLength = request.response().bytesWritten();


    jsonObjectBuilder.add("status",status);
    jsonObjectBuilder.add("content_length",contentLength);

    jsonObjectBuilder.add("duration", System.currentTimeMillis()-timestamp);

    final MultiMap headers = request.headers();
    String referrer = headers.contains(REFERRER) ? headers.get(REFERRER) : headers.get("referer");
    String userAgent = request.headers().get("user-agent");

    if (referrer != null) {
      jsonObjectBuilder.add(REFERRER, referrer);
    }
    if (userAgent != null) {
      jsonObjectBuilder.add("user_agent", userAgent);
    }
    jsonObjectBuilder.add("remote",remoteClient);


    JsonObject jsonMessage = jsonObjectBuilder.build();
    String msg = jsonb.toJson(jsonMessage);

    log.info(msg);
  }


  private String getClientAddress(SocketAddress inetSocketAddress) {
    if (inetSocketAddress == null) {
      return null;
    }
    return inetSocketAddress.host();
  }


  @Override
  public void handle(RoutingContext context) {
    // common logging data
    long timestamp = System.currentTimeMillis();
    String remoteClient = getClientAddress(context.request().remoteAddress());
    HttpMethod method = context.request().method();
    String uri = context.request().uri();
    HttpVersion version = context.request().version();

    context.addBodyEndHandler(v -> log(context, timestamp, remoteClient, version, method, uri));

    context.next();

  }

}
