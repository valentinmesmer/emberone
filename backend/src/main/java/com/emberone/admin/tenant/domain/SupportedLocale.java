/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Tenant UI language. Immutable after tenant creation (v1). Extend when adding locale bundles. */
@Getter
@RequiredArgsConstructor
public enum SupportedLocale {
    DE("de");

    private final String code;

    @JsonCreator
    public static SupportedLocale fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DE;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (SupportedLocale locale : values()) {
            if (locale.code.equals(normalized)) {
                return locale;
            }
        }
        throw new IllegalArgumentException("Unsupported locale: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public Locale toJavaLocale() {
        return Locale.forLanguageTag(code);
    }
}
