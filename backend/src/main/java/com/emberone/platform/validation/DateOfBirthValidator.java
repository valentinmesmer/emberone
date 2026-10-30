/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class DateOfBirthValidator implements ConstraintValidator<DateOfBirth, LocalDate> {

    private boolean allowEmpty;

    @Override
    public void initialize(DateOfBirth constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowEmpty;
        }
        return DateOfBirthRules.isValid(value);
    }
}
