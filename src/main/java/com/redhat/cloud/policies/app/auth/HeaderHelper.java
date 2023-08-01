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
package com.redhat.cloud.policies.app.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Optional;
import javax.ws.rs.core.HttpHeaders;

public abstract class HeaderHelper {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static Optional<XRhIdentity> getRhIdFromHeader(HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return Optional.empty();
        }
        String headerString = httpHeaders.getHeaderString("x-rh-identity");
        if (headerString == null) {
            return Optional.empty();
        }
        return getRhIdFromString(headerString);
    }

    public static Optional<XRhIdentity> getRhIdFromString(String xRhIdHeader) {
        XRhIdentity rhIdentity;
        try {
            String json_string = new String(Base64.getDecoder().decode(xRhIdHeader));
            rhIdentity = objectMapper.readValue(json_string, XRhIdentity.class);
        } catch (JsonProcessingException jpe) {
            return Optional.empty();
        }
        return Optional.ofNullable(rhIdentity);
    }
}
