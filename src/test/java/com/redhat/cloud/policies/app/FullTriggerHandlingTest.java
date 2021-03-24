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

import com.redhat.cloud.policies.app.model.Policy;
import com.redhat.cloud.policies.app.model.engine.FullTrigger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * Some testing around FullTrigger setup and updating
 * @author hrupp
 */
public class FullTriggerHandlingTest {

    @Test
    void testSetup() {
        Policy p = createPolicy();

        FullTrigger ft = new FullTrigger(p);

        Assert.assertFalse(ft.trigger.enabled);
        Assert.assertEquals("hula", ft.trigger.name);

        Assert.assertEquals(1,ft.conditions.size());
        Assert.assertEquals("bla",ft.conditions.get(0).expression);

        Assert.assertEquals(1,ft.trigger.actions.size());
        Assert.assertEquals("notification",ft.trigger.actions.iterator().next().actionPlugin);

        Assert.assertEquals("hula", ft.trigger.name);

        Assert.assertEquals("some text",ft.trigger.description);
    }

    @NotNull
    private Policy createPolicy() {
        Policy p = new Policy();
        p.customerid = "1234";
        p.conditions = "bla";
        p.actions = "notification";
        p.name = "hula";
        p.description = "some text";
        return p;
    }

    @Test
    void testActionUpdate1() {
        Policy p = createPolicy();

        FullTrigger ft = new FullTrigger(p);

        p.actions = "notification";
        ft.updateFromPolicy(p);

        Assert.assertEquals(1,ft.trigger.actions.size());
        Assert.assertEquals("notification",ft.trigger.actions.iterator().next().actionPlugin);
    }

    @Test
    void testActionUpdate4() {
        Policy p = createPolicy();
        p.actions = "notification";

        FullTrigger ft = new FullTrigger(p);

        p.actions = null;
        ft.updateFromPolicy(p);

        Assert.assertEquals(0,ft.trigger.actions.size());
    }

    @Test
    void testConditionUpdate1() {
        Policy p = createPolicy();

        FullTrigger ft = new FullTrigger(p);

        p.conditions = "blabla";
        ft.updateFromPolicy(p);

        Assert.assertEquals(1,ft.conditions.size());
        Assert.assertEquals("blabla",ft.conditions.get(0).expression);

    }

    @Test
    void testEnabledUpdate() {
        Policy p = createPolicy();

        FullTrigger ft = new FullTrigger(p);

        p.isEnabled = true;
        ft.updateFromPolicy(p);

        Assert.assertTrue(ft.trigger.enabled);
    }


    @Test
    void testActionWithParam() {
        Policy p = createPolicy();
        p.actions = "notification maybe-param";

        FullTrigger ft = new FullTrigger(p);

        Assert.assertEquals(1,ft.trigger.actions.size());
    }

    @Test
    void testNameUpdate() {
        Policy p = createPolicy();

        FullTrigger ft = new FullTrigger(p);

        p.name = p.name + "-2";
        ft.updateFromPolicy(p);

        Assert.assertEquals("hula-2", ft.trigger.name);
    }

    @Test
    void testDescriptionUpdate() {
        Policy p = createPolicy();

        FullTrigger ft = new FullTrigger(p);

        p.description = p.description + "-2";
        ft.updateFromPolicy(p);

        Assert.assertEquals("some text-2", ft.trigger.description);
    }

}
