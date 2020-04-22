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
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Map;

/**
 * Do some validation of the OApiModel.
 * @author hrupp
 */
@QuarkusTest
public class OapiTest extends AbstractITest {

  private static final String OAPI_YAML = "openapi.json";

  // QuarkusTest will inject the host+port for us.
 	@TestHTTPResource(API_BASE_V1_0 + "/" + OAPI_YAML)
	URL url;

 	@Test
 	public void validateOpenApi() throws Exception {
 		OpenApi3 model = new OpenApi3Parser().parse(url, true);
 		System.out.printf("OpenAPI Model at %s\n", url);
 		if (! model.isValid()) {
 			for (ValidationResults.ValidationItem item : model.getValidationItems()) {
 				System.err.println(item);
 			}
 			Assert.fail("OpenAPI spec is not valid");
 		}
 		//
 		// Now that basic validation is done, we can add some of our own
		//
		Map<String, Path> paths = model.getPaths();
		Map<String, Schema> schemas = model.getSchemas();

		// The base path filler. See also OASModifier.mangleName
 		Assert.assertTrue(paths.containsKey("/"));

 		// User config is private, so don't show it
 		Assert.assertFalse(paths.containsKey("/user-config"));
 		Assert.assertFalse(schemas.containsKey("SettingsValues"));
 	}
}
