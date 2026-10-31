/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.management.domain;

/** Who ended an activation before its {@code validUntil}. */
public enum ManagementActivationEndedBy {
    USER,
    SYSTEM
}
