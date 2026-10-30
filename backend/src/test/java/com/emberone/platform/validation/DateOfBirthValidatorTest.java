/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateOfBirthValidatorTest {

    private DateOfBirthValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DateOfBirthValidator();
    }

    @Test
    void allowsNullWhenConfigured() {
        validator.initialize(annotation(true));

        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void rejectsNullWhenNotAllowed() {
        validator.initialize(annotation(false));

        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void rejectsShortYear() {
        validator.initialize(annotation(true));

        assertThat(validator.isValid(LocalDate.of(4, 5, 10), null)).isFalse();
    }

    @Test
    void acceptsValidDateOfBirth() {
        validator.initialize(annotation(true));

        assertThat(validator.isValid(LocalDate.of(2004, 5, 10), null)).isTrue();
    }

    private DateOfBirth annotation(boolean allowEmpty) {
        return new DateOfBirth() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return DateOfBirth.class;
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
            public boolean allowEmpty() {
                return allowEmpty;
            }
        };
    }
}
