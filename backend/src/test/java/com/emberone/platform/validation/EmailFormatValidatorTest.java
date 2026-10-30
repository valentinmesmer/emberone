/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailFormatValidatorTest {

    private EmailFormatValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EmailFormatValidator();
    }

    @Test
    void allowsEmptyWhenConfigured() {
        validator.initialize(annotation(true));

        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void rejectsEmptyWhenNotAllowed() {
        validator.initialize(annotation(false));

        assertThat(validator.isValid(null, null)).isFalse();
        assertThat(validator.isValid("   ", null)).isFalse();
    }

    @Test
    void acceptsValidEmail() {
        validator.initialize(annotation(false));

        assertThat(validator.isValid("user@example.com", null)).isTrue();
    }

    @Test
    void rejectsInvalidEmail() {
        validator.initialize(annotation(false));

        assertThat(validator.isValid("not-an-email", null)).isFalse();
        assertThat(validator.isValid("missing@domain", null)).isFalse();
    }

    @Test
    void rejectsNonAsciiEmail() {
        validator.initialize(annotation(false));

        assertThat(validator.isValid("andreas.kölbl@example.com", null)).isFalse();
    }

    private EmailFormat annotation(boolean allowEmpty) {
        return new EmailFormat() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return EmailFormat.class;
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
