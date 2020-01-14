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
package com.redhat.cloud.custompolicies.app;

import com.redhat.cloud.custompolicies.app.model.Msg;
import java.io.ByteArrayInputStream;
import javax.annotation.Priority;
import javax.json.bind.JsonbBuilder;
import javax.validation.ValidationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Deal with different status codes to return different Exceptions
 * @author hrupp
 */
@Priority(4000)
public class EngineResponseExceptionMapper implements ResponseExceptionMapper<RuntimeException> {
  @Override
  public RuntimeException toThrowable(Response response) {
    int status = response.getStatus();

    // Get the body of the response as it may give additional info
    Msg msg = getBody(response);

    RuntimeException re ;
    switch (status) {
      case 412: re = new ValidationException("Validation failed " + msg.msg);
      break;
      default:
        re = new WebApplicationException(status);
    }
    return re;
  }

  private Msg getBody(Response response) {
    ByteArrayInputStream is = (ByteArrayInputStream) response.getEntity();
    if (is != null ) {
      byte[] bytes = new byte[is.available()];
      is.read(bytes, 0, is.available());
      String json = new String(bytes);
      return JsonbBuilder.create().fromJson(json, Msg.class);
    } else {
      return new Msg("-- no body received, status code is " + response.getStatus());
    }
  }
}
