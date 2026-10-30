/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.domain;

/** Derived from {@code endedAt} and {@code validUntil}; never stored. */
public enum ManagementActivationStatus {
    ACTIVE,
    EXPIRED,
    ENDED;

    public static ManagementActivationStatus of(
            ManagementActivation activation, java.time.Instant now) {
        if (activation.getEndedAt() != null) {
            return ENDED;
        }
        return activation.getValidUntil().isAfter(now) ? ACTIVE : EXPIRED;
    }
}
