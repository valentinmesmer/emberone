/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.domain;

/** Who ended an activation before its {@code validUntil}. */
public enum ManagementActivationEndedBy {
    USER,
    SYSTEM
}
