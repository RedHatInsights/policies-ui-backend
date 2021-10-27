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

import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Do the validation if passed actions are good or not.
 */
public class ActionValidator implements ConstraintValidator<ValidActionS, String> {

    private static final List<String> VALID_ACTIONS = List.of("notification");

    @Override
    public boolean isValid(String input, ConstraintValidatorContext context) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        for (String action : input.split(";")) {
            String a = action.toLowerCase().strip();
            if (!a.isEmpty() && !VALID_ACTIONS.contains(a)) {
                return false;
            }
        }
        return true;
    }
}
