/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a phone number is in a valid format. Uses Google's libphonenumber library for
 * validation.
 *
 * <p>The phone number can be in any format (international or national). If a default region is
 * specified, national format numbers will be validated against that region. Defaults to {@link
 * ValidationPatterns#DEFAULT_PHONE_REGION} (DE), aligned with the frontend.
 *
 * <p>Examples: - "+49 30 12345678" (international format) - "030 12345678" (national format,
 * requires defaultRegion="DE") - "+1 555 1234567" (international format)
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {
    String message() default "Invalid phone number format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Default region code (ISO 3166-1 alpha-2 country code) for validating national format numbers.
     * If not specified, only international format numbers (starting with +) will be accepted.
     *
     * <p>Examples: "DE", "US", "GB"
     */
    String defaultRegion() default ValidationPatterns.DEFAULT_PHONE_REGION;

    /** Whether to allow empty/null values. Default is true. */
    boolean allowEmpty() default true;
}
