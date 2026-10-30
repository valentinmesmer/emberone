/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PhoneNumberValidatorTest {

    private PhoneNumberValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PhoneNumberValidator();
    }

    @Test
    void allowsEmptyWhenConfigured() {
        validator.initialize(annotation(true, "DE"));

        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void rejectsEmptyWhenNotAllowed() {
        validator.initialize(annotation(false, "DE"));

        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void acceptsInternationalGermanNumber() {
        validator.initialize(annotation(true, "DE"));

        assertThat(validator.isValid("+49 30 12345678", null)).isTrue();
    }

    @Test
    void fallsBackToInternationalWhenNationalInvalid() {
        validator.initialize(annotation(true, "DE"));

        assertThat(validator.isValid("14155552671", null)).isTrue();
    }

    @Test
    void acceptsNationalNumberWithDefaultRegion() {
        validator.initialize(annotation(true, "DE"));

        assertThat(validator.isValid("030 12345678", null)).isTrue();
    }

    @Test
    void rejectsInvalidNumber() {
        validator.initialize(annotation(true, "DE"));

        assertThat(validator.isValid("not-a-phone", null)).isFalse();
        assertThat(validator.isValid("123", null)).isFalse();
    }

    @Test
    void validatesDigitsOnlyWhenNoDefaultRegion() {
        validator.initialize(annotation(true, ""));

        assertThat(validator.isValid("14155552671", null)).isTrue();
    }

    @Test
    void rejectsInputWithoutDigitsWhenNoDefaultRegion() {
        validator.initialize(annotation(true, ""));

        assertThat(validator.isValid("not-a-phone", null)).isFalse();
    }

    @Test
    void rejectsWhenNationalInvalidAndNoDigitsRemain() {
        validator.initialize(annotation(true, "DE"));

        assertThat(validator.isValid("()", null)).isFalse();
    }

    private PhoneNumber annotation(boolean allowEmpty, String defaultRegion) {
        return new PhoneNumber() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return PhoneNumber.class;
            }

            @Override
            public String message() {
                return "invalid";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public String defaultRegion() {
                return defaultRegion;
            }

            @Override
            public boolean allowEmpty() {
                return allowEmpty;
            }
        };
    }
}
