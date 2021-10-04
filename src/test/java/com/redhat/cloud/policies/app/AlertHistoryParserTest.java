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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.cloud.policies.app.model.engine.HistoryItem;
import com.redhat.cloud.policies.app.rest.PolicyCrudService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Some additional testing around parsing of alert history items. This is on a unit test level so also make it easier to
 * understand and follow format changes from the engine.
 */
public class AlertHistoryParserTest {

    @Test
    void testAlertHistoryDocument() throws JsonProcessingException {

        String alerts = HeaderHelperTest.getStringFromFile("alerts-history2.json", false);

        List<HistoryItem> items = new ArrayList<>();

        PolicyCrudService.parseHistoryFromEngine(alerts, items);

        Assert.assertEquals(1, items.size());
        HistoryItem item = items.get(0);
        Assert.assertEquals("VM", item.hostName);
    }
}
