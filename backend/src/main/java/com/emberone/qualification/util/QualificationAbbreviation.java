/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.util;

import com.emberone.platform.exception.ConflictException;
import java.util.regex.Pattern;

public final class QualificationAbbreviation {

    private static final int MAX_LENGTH = 255;
    private static final Pattern ALLOWED = Pattern.compile("^[\\p{L}0-9./ _-]+$");

    private QualificationAbbreviation() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    public static String requireValid(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            throw new ConflictException("Abbreviation is required");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new ConflictException("Abbreviation is too long");
        }
        if (!ALLOWED.matcher(normalized).matches()) {
            throw new ConflictException(
                    "Abbreviation may only contain letters, digits, dots, spaces, slashes, hyphens and underscores");
        }
        return normalized;
    }
}
