/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

/** Parsed {@code sort} query value ({@code key,asc|desc}). Shared by all list implementations. */
public record ListSort(String key, Sort.Direction direction) {

    /**
     * @throws IllegalArgumentException if the value is not {@code key,asc} or {@code key,desc}
     */
    public static ListSort parse(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("invalid sort format");
        }
        String[] parts = value.split(",", 2);
        if (parts.length != 2 || !StringUtils.hasText(parts[0])) {
            throw new IllegalArgumentException("invalid sort format");
        }
        Sort.Direction direction =
                switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
                    case "asc" -> Sort.Direction.ASC;
                    case "desc" -> Sort.Direction.DESC;
                    default -> throw new IllegalArgumentException("invalid sort direction");
                };
        return new ListSort(parts[0].trim(), direction);
    }
}
