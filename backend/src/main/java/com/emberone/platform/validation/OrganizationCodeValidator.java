/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OrganizationCodeValidator implements ConstraintValidator<OrganizationCode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return OrganizationCodeRules.isValid(value);
    }
}
