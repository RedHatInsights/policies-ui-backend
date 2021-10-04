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

import com.redhat.cloud.policies.app.model.Msg;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Priority;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.validation.ValidationException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Deal with different status codes to return different Exceptions
 */
@Priority(4000)
public class EngineResponseExceptionMapper implements ResponseExceptionMapper<RuntimeException> {
    @Override
    public RuntimeException toThrowable(Response response) {
        int status = response.getStatus();

        RuntimeException re;
        switch (status) {
            case 400:
                re = new ValidationException("Validation failed: " + getBody(response).msg);
                break;
            case 404:
                re = new NotFoundException(getBody(response).msg);
                break;
            default:
                // If this is a 500 error it is likely we get HTML
                re = new WebApplicationException(status + " " + response.getStatusInfo().getReasonPhrase());
        }
        return re;
    }

    private Msg getBody(Response response) {
        String msg = response.readEntity(String.class);
        if (msg != null && !msg.isEmpty()) {
            try (Jsonb jsonb = JsonbBuilder.create()) {
                Map<String, String> errorMap = jsonb.fromJson(msg, HashMap.class);
                return new Msg(errorMap.get("errorMsg"));
            } catch (Exception e) {
                return new Msg("Parsing of response failed. Status code was " + response.getStatus());
            }
        } else {
            return new Msg("-- no body received, status code is " + response.getStatus());
        }
    }
}
