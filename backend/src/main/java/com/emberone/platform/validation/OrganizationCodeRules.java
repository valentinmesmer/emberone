/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.validation;

public final class OrganizationCodeRules {

    private OrganizationCodeRules() {}

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.length() > ValidationPatterns.ORGANIZATION_CODE_MAX_LENGTH) {
            return false;
        }
        return normalized.matches(ValidationPatterns.ORGANIZATION_CODE);
    }
}
