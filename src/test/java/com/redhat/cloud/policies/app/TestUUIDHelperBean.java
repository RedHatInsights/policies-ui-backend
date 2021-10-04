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

import com.redhat.cloud.policies.app.model.UUIDHelperBean;
import java.util.UUID;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

/**
 * Provider for UUIDs for Tests. A test can
 * provide a known uuid and later check on it.
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class TestUUIDHelperBean extends UUIDHelperBean {

    private UUID storedUUID;

    public void storeUUID(UUID toBeStored) {
        this.storedUUID = toBeStored;
    }

    public void storeUUIDString(String toBeStored) {
        this.storedUUID = UUID.fromString(toBeStored);
    }

    public void clearUUID() {
        this.storedUUID = null;
    }

    public UUID getUUID() {
        if (storedUUID != null) {
            return storedUUID;
        }
        return UUID.randomUUID();
    }
}
