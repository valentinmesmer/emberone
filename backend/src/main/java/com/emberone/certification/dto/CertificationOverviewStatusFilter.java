/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.dto;

import com.emberone.platform.web.list.ListFilters;
import java.util.List;
import java.util.Optional;

/**
 * Status filter of the certification status overview. Repeated {@code status} params are
 * OR-combined; empty list means no status restriction.
 */
public enum CertificationOverviewStatusFilter {
    MISSING,
    EXPIRED,
    EXPIRING_SOON,
    VALID;

    public static List<CertificationOverviewStatusFilter> parseStatuses(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return ListFilters.parseEnumList(
                raw,
                "status",
                value -> {
                    try {
                        return Optional.of(valueOf(value));
                    } catch (IllegalArgumentException ex) {
                        return Optional.empty();
                    }
                });
    }
}
