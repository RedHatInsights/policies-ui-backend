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

import com.reprezen.kaizen.oasparser.OpenApi3Parser;
import com.reprezen.kaizen.oasparser.model3.OpenApi3;
import com.reprezen.kaizen.oasparser.model3.Path;
import com.reprezen.kaizen.oasparser.model3.Schema;
import com.reprezen.kaizen.oasparser.val.ValidationResults;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Do some validation of the OApiModel.
 */
@QuarkusTest
@Tag("integration")
class OapiTest extends AbstractITest {

    private static final String OAPI_JSON = "openapi.json";
    private static final String TARGET_OPENAPI = "./target/openapi.json";

    // QuarkusTest will inject the host+port for us.
    @TestHTTPResource(API_BASE_V1_0 + "/" + OAPI_JSON)
    URL url;

    @Test
    void validateOpenApi() throws Exception {
        OpenApi3 model = new OpenApi3Parser().parse(url, true);
        System.out.printf("OpenAPI Model at %s\n", url);
        if (!model.isValid()) {
            for (ValidationResults.ValidationItem item : model.getValidationItems()) {
                System.err.println(item);
            }
            fail("OpenAPI spec is not valid");
        }
        //
        // Now that basic validation is done, we can add some of our own
        //
        Map<String, Path> paths = model.getPaths();
        Map<String, Schema> schemas = model.getSchemas();

        // The base path filler. See also OASModifier.mangleName
        assertTrue(paths.containsKey("/"));

        // User config is private, so don't show it
        assertFalse(paths.containsKey("/user-config"));
        assertFalse(schemas.containsKey("SettingsValues"));

        // Check that openapi does not (again) collapse parameters
        assertEquals(9, paths.get("/policies").getOperation("get").getParameters().size());

        // Check that all properties are present ( https://github.com/smallrye/smallrye-open-api/issues/437 )
        Map<String, Schema> policyProperties = schemas.get("Policy").getProperties();
        assertEquals(9, policyProperties.size());
        assertTrue(policyProperties.containsKey("ctime"));
        assertTrue(policyProperties.containsKey("mtime"));

        // Now that the OpenAPI file has been validated, save a copy to the filesystem
        // This file is going to be uploaded in a regular CI build to know the API state
        // for a given build.
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(TARGET_OPENAPI), StandardCopyOption.REPLACE_EXISTING);
    }
}
