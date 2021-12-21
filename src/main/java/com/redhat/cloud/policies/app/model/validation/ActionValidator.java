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
package com.redhat.cloud.policies.app.model.validation;

import com.redhat.cloud.policies.app.EnvironmentInfo;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Do the validation if passed actions are good or not.
 */
@ApplicationScoped
public class ActionValidator implements ConstraintValidator<ValidActionS, String> {

    @Inject
    EnvironmentInfo environmentInfo;

    private static final List<String> VALID_ACTIONS = List.of("notification");
    private static final List<String> VALID_ACTIONS_FEDRAMP = List.of();

    public List<String> getValidActions() {
        if (environmentInfo.isFedramp()) {
            return VALID_ACTIONS_FEDRAMP;
        }

        return VALID_ACTIONS;
    }

    @Override
    public boolean isValid(String input, ConstraintValidatorContext context) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        final List<String> validActions = getValidActions();

        for (String action : input.split(";")) {
            String a = action.strip();
            if (!a.isEmpty() && !validActions.contains(a)) {
                return false;
            }
        }
        return true;
    }
}
