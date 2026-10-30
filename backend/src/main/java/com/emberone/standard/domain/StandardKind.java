/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.domain;

/**
 * The kind of bundle a {@link Standard} represents. Today a standard is always a complete
 * per-organization setup ({@link #ORGANIZATION_SETUP}) that is selected per organization type. The
 * field exists so future standard kinds can dock onto the same SPI without a schema change.
 */
public enum StandardKind {
    ORGANIZATION_SETUP
}
