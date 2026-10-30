/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.util;

import com.emberone.user.util.UsernamePartNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class TenantAbbreviation {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 32;

    private TenantAbbreviation() {}

    public static String requireValid(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant code is required");
        }
        String normalized = UsernamePartNormalizer.normalize(raw.trim());
        if (normalized.length() < MIN_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tenant code must be at least "
                            + MIN_LENGTH
                            + " characters after normalization");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant code is too long");
        }
        return normalized;
    }
}
