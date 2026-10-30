/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.util;

import com.emberone.platform.exception.ConflictException;
import java.util.Locale;
import java.util.regex.Pattern;

/** User-defined abbreviation for tenant training course types (e.g. {@code GF}, {@code MTA ZP}). */
public final class TrainingCourseAbbreviation {

    private static final int MAX_LENGTH = 255;
    private static final Pattern ALLOWED = Pattern.compile("^[A-Z0-9]+(?:[ /_-][A-Z0-9]+)*$");

    private TrainingCourseAbbreviation() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
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
                    "Abbreviation may only contain letters, digits, spaces, slashes, hyphens and underscores");
        }
        return normalized;
    }
}
