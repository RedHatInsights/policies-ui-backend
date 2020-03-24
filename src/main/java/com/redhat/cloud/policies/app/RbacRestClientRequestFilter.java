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

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

/**
 * Filter to optionally add data on outgoing requests to the RBAC service
 * @author hrupp
 */
public class RbacRestClientRequestFilter implements ClientRequestFilter {

  private String authInfo;

  public RbacRestClientRequestFilter() {
     String tmp = System.getProperty("develop.exceptional.user.auth.info");
    if (tmp!=null && !tmp.isEmpty()) {
      authInfo = Base64.getEncoder().encodeToString(tmp.getBytes());
    }
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {

    if (authInfo!=null) {
      URI uri = requestContext.getUri();
      if (uri.toString().startsWith("https://ci.cloud.redhat.com")) {
        requestContext.getHeaders().putSingle("Authorization", "Basic " + authInfo);
      }
    }
  }
}
