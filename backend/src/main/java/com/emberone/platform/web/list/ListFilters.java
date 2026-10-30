/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Small helpers for optional list query filters (repeated params, enums). */
public final class ListFilters {

    private ListFilters() {}

    /** Absent or empty request param list → empty immutable list (no filter). */
    public static List<Long> optionalLongIds(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return List.copyOf(values);
    }

    /**
     * Parses repeated string query values into domain enums. Blank entries are skipped; invalid
     * values throw {@link IllegalArgumentException} with {@code paramName} in the message.
     */
    public static <T> List<T> parseEnumList(
            List<String> raw, String paramName, Function<String, Optional<T>> parser) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<T> parsed = new ArrayList<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            parsed.add(
                    parser.apply(value.trim())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "invalid " + paramName + " filter")));
        }
        return List.copyOf(parsed);
    }

    /** Parses repeated numeric string query values; invalid entries throw. */
    public static List<Long> parseLongList(List<String> raw, String paramName) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Long> parsed = new ArrayList<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                parsed.add(Long.parseLong(value.trim()));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("invalid " + paramName + " filter");
            }
        }
        return List.copyOf(parsed);
    }
}
