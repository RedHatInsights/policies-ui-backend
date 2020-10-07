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
package com.redhat.cloud.policies.app.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserialize JsonPatch formatted json into {{@link JsonPatch}} objects
 *
 * @see JsonPatch
 * @link https://tools.ietf.org/html/rfc6902
 *
 * @author hrupp
 */
@Provider
@Consumes("application/json-patch+json")
public class JsonPatchDeserializer implements MessageBodyReader<JsonPatch> {

  ObjectMapper mapper = new ObjectMapper();

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return mediaType.equals(MediaType.valueOf(MediaType.APPLICATION_JSON_PATCH_JSON));
  }

  @Override
  public JsonPatch readFrom(Class<JsonPatch> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
    String data = new String(entityStream.readAllBytes());

    List<JsonPatch.JsonPatchOp> outer = mapper.readValue(data, new TypeReference<>() { });
    System.out.println(outer);

    JsonPatch patch = new JsonPatch();
    if (outer.size()==0) {
      patch.operations= new ArrayList<>(1);
    } else {
      patch.operations= new ArrayList<>(outer);
    }

    return patch;
  }
}
