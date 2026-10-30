/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.domain;

/**
 * Origin of a configurable item. The system seeds read-only {@link #SYSTEM} baselines; {@link
 * #CUSTOM} denotes tenant-created content (e.g. cloned org standards, custom cert definitions).
 */
public enum StandardSource {
    SYSTEM,
    CUSTOM
}
