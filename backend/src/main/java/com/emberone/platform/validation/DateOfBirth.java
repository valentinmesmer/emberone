/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates a date of birth using {@link DateOfBirthRules}.
 *
 * <p>When {@link #allowEmpty()} is true, null values are accepted.
 */
@Documented
@Constraint(validatedBy = DateOfBirthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateOfBirth {
    String message() default "Date of birth must be a valid past date with a full four-digit year";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Whether to allow null values. Default is true. */
    boolean allowEmpty() default true;
}
